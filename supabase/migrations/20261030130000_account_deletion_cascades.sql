-- LOCAL PREPARATION ONLY. Pair with the delete-account Edge Function.
-- The selected product rule is to erase all account-linked history.
-- Do not apply until the retention behavior has been reviewed.

begin;

alter table public.point_transactions
    drop constraint if exists point_transactions_user_id_fkey;
alter table public.point_transactions
    add constraint point_transactions_user_id_fkey
    foreign key (user_id) references public.profiles(id) on delete cascade;

alter table public.receipt_submissions
    drop constraint if exists receipt_submissions_user_id_fkey;
alter table public.receipt_submissions
    add constraint receipt_submissions_user_id_fkey
    foreign key (user_id) references public.profiles(id) on delete cascade;

alter table public.user_coupons
    drop constraint if exists user_coupons_user_id_fkey;
alter table public.user_coupons
    add constraint user_coupons_user_id_fkey
    foreign key (user_id) references public.profiles(id) on delete cascade;

alter table public.orders
    drop constraint if exists orders_user_id_fkey;
alter table public.orders
    add constraint orders_user_id_fkey
    foreign key (user_id) references public.profiles(id) on delete cascade;

alter table public.order_items
    drop constraint if exists order_items_order_id_fkey;
alter table public.order_items
    add constraint order_items_order_id_fkey
    foreign key (order_id) references public.orders(id) on delete cascade;

commit;
