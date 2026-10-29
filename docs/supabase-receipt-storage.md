# Private receipt storage setup

`supabase/migrations/20261030120000_private_receipt_storage.sql` prepares a private
`smkoin-receipts` bucket with a 10 MiB JPEG limit. The migration is local only and has
not been applied to Supabase.

The Android client stores images as
`smkoin-receipts/<auth-user-id>/<submission-uuid>.jpg`. The same path is written to
`receipt_submissions.receipt_image_url`; this column contains a private object path,
not a public URL. Storage RLS limits insert, select, and delete to the matching user
folder. No public bucket access is granted.

Before applying the migration, confirm the table schema has already been deployed and
review the policies in the Supabase SQL editor. The upload workflow first stores the
image, then inserts a `receipt_submissions` row with `status = 'pending'` and
`points_awarded = 0`. A trusted review process is still required to approve/reject a
receipt and create any point ledger entry. The app does not run OCR or award points.

The schema has no unique receipt submission key. The app reuses one generated object
path for retries in the current capture session and checks for an existing row by
that path, but the database does not provide a global idempotency guarantee. A
submission request that fails ambiguously should be checked in the user's receipt
records before manually creating another submission.
