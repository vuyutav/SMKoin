-- SMKoin redemption idempotency.
--
-- LOCAL DRAFT ONLY: this migration has not been applied to Supabase.
-- Coordinate the Android client update to call redeem_product(uuid, integer, text, uuid)
-- before applying. Revoking the legacy three-argument RPC intentionally prevents
-- older clients from bypassing idempotency.

begin;

alter table public.orders
    add column if not exists idempotency_key uuid;

create unique index if not exists orders_user_idempotency_key_uidx
    on public.orders(user_id, idempotency_key)
    where idempotency_key is not null;

create or replace function public.redeem_product(
    p_product_id uuid,
    p_quantity integer,
    p_delivery_location text,
    p_idempotency_key uuid
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_user_id uuid;
    v_product public.products%rowtype;
    v_balance bigint;
    v_total bigint;
    v_order_id uuid;
    v_existing_product_id uuid;
    v_existing_quantity integer;
    v_existing_location text;
    v_location text;
begin
    v_user_id := auth.uid();

    if v_user_id is null then
        raise exception 'Authentication required';
    end if;

    if p_idempotency_key is null then
        raise exception 'Idempotency key is required';
    end if;

    if p_quantity is null or p_quantity < 1 or p_quantity > 100 then
        raise exception 'Invalid quantity';
    end if;

    v_location := nullif(trim(p_delivery_location), '');

    -- Fast replay path for a request that has already committed.
    select o.id, oi.product_id, oi.quantity, o.delivery_location
    into v_order_id, v_existing_product_id, v_existing_quantity, v_existing_location
    from public.orders o
    join public.order_items oi on oi.order_id = o.id
    where o.user_id = v_user_id
      and o.idempotency_key = p_idempotency_key
    for update of o;

    if found then
        if v_existing_product_id is distinct from p_product_id
           or v_existing_quantity is distinct from p_quantity
           or v_existing_location is distinct from v_location then
            raise exception 'Idempotency key was already used for a different redemption';
        end if;
        return v_order_id;
    end if;

    -- Serialize stock changes for this product.
    select *
    into v_product
    from public.products
    where id = p_product_id
      and is_active = true
    for update;

    if not found then
        raise exception 'Product not found or unavailable';
    end if;

    -- A same-product retry waits for the product lock. Recheck before stock
    -- validation so it can replay the committed result even if stock is now zero.
    select o.id, oi.product_id, oi.quantity, o.delivery_location
    into v_order_id, v_existing_product_id, v_existing_quantity, v_existing_location
    from public.orders o
    join public.order_items oi on oi.order_id = o.id
    where o.user_id = v_user_id
      and o.idempotency_key = p_idempotency_key
    for update of o;

    if found then
        if v_existing_product_id is distinct from p_product_id
           or v_existing_quantity is distinct from p_quantity
           or v_existing_location is distinct from v_location then
            raise exception 'Idempotency key was already used for a different redemption';
        end if;
        return v_order_id;
    end if;

    if v_product.stock_quantity < p_quantity then
        raise exception 'Insufficient stock';
    end if;

    -- Serialize point spending for this user. This also closes the race for the
    -- same idempotency key used concurrently with different product locks.
    perform 1
    from public.profiles
    where id = v_user_id
    for update;

    if not found then
        raise exception 'User profile not found';
    end if;

    select o.id, oi.product_id, oi.quantity, o.delivery_location
    into v_order_id, v_existing_product_id, v_existing_quantity, v_existing_location
    from public.orders o
    join public.order_items oi on oi.order_id = o.id
    where o.user_id = v_user_id
      and o.idempotency_key = p_idempotency_key
    for update of o;

    if found then
        if v_existing_product_id is distinct from p_product_id
           or v_existing_quantity is distinct from p_quantity
           or v_existing_location is distinct from v_location then
            raise exception 'Idempotency key was already used for a different redemption';
        end if;
        return v_order_id;
    end if;

    select coalesce(sum(t.points_delta), 0)::bigint
    into v_balance
    from public.point_transactions t
    where t.user_id = v_user_id;

    v_total := v_product.point_price::bigint * p_quantity::bigint;

    if v_balance < v_total then
        raise exception 'Insufficient point balance';
    end if;

    insert into public.orders (
        user_id,
        status,
        total_points,
        delivery_location,
        idempotency_key
    )
    values (
        v_user_id,
        'pending',
        v_total,
        v_location,
        p_idempotency_key
    )
    returning id into v_order_id;

    insert into public.order_items (
        order_id,
        product_id,
        product_name,
        quantity,
        unit_price_points
    )
    values (
        v_order_id,
        v_product.id,
        v_product.name,
        p_quantity,
        v_product.point_price
    );

    update public.products
    set stock_quantity = stock_quantity - p_quantity
    where id = v_product.id;

    if v_total > 0 then
        insert into public.point_transactions (
            user_id,
            points_delta,
            transaction_type,
            description,
            reference_id
        )
        values (
            v_user_id,
            -v_total,
            'purchase',
            'Reward redemption: ' || v_product.name,
            v_order_id
        );
    end if;

    return v_order_id;
end;
$$;

-- The old RPC must not remain callable after the idempotent client is deployed.
revoke all on function public.redeem_product(uuid, integer, text)
    from public, anon, authenticated;

revoke all on function public.redeem_product(uuid, integer, text, uuid)
    from public, anon;

grant execute on function public.redeem_product(uuid, integer, text, uuid)
    to authenticated;

commit;
