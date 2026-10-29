-- SMKoin receipt uploads.
--
-- LOCAL PREPARATION ONLY. This migration has not been applied to the hosted project.
-- The Android client uploads JPEGs to:
--   smkoin-receipts/<authenticated-user-uuid>/<client-generated-submission-uuid>.jpg
-- receipt_submissions.receipt_image_url stores that private object path (not a public URL).

begin;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('smkoin-receipts', 'smkoin-receipts', false, 10485760, array['image/jpeg'])
on conflict (id) do nothing;

do $$
begin
    if not exists (
        select 1 from storage.buckets
        where id = 'smkoin-receipts'
          and name = 'smkoin-receipts'
          and public = false
          and file_size_limit = 10485760
          and allowed_mime_types = array['image/jpeg']::text[]
    ) then
        raise exception 'The existing smkoin-receipts bucket does not match the required private JPEG configuration.';
    end if;
end;
$$;

create policy "SMKoin users upload own receipts"
on storage.objects for insert
to authenticated
with check (
    bucket_id = 'smkoin-receipts'
    and (storage.foldername(name))[1] = (select auth.uid())::text
);

create policy "SMKoin users read own receipts"
on storage.objects for select
to authenticated
using (
    bucket_id = 'smkoin-receipts'
    and (storage.foldername(name))[1] = (select auth.uid())::text
);

create policy "SMKoin users delete own receipt uploads"
on storage.objects for delete
to authenticated
using (
    bucket_id = 'smkoin-receipts'
    and (storage.foldername(name))[1] = (select auth.uid())::text
);

commit;
