# Account deletion backend setup

The owner selected deletion of all account-linked history. The local-only migration
`supabase/migrations/20261030130000_account_deletion_cascades.sql` changes the relevant
foreign keys so deleting `auth.users` removes the profile, point transactions, receipt
submissions, coupons, orders, and order items. The private receipt objects are removed
by `supabase/functions/delete-account/index.ts` before the Auth user is deleted.

Neither the migration nor function has been applied or deployed. Review both before
using them. The operation is destructive and Storage plus Postgres cannot participate
in one transaction: if receipt removal succeeds but Auth deletion then fails, the
function reports that partial outcome and does not claim the account was deleted.

## Supabase setup (after review)

1. Apply the existing application schema, the private receipt bucket migration, and
   the account-deletion cascade migration in order.
2. Configure the Edge Function runtime secrets from the Supabase dashboard or secret
   manager: `SUPABASE_URL`, `SUPABASE_ANON_KEY` (or `SUPABASE_PUBLISHABLE_KEY`), and
   `SUPABASE_SERVICE_ROLE_KEY`. Never put the service-role key in the repository or
   Android build configuration.
3. Deploy the function only after reviewing the destructive behavior. The Android app
   calls `POST /functions/v1/delete-account` with the current user's bearer token after
   reauthenticating with the current password.

Do not grant authenticated clients direct delete privileges on profiles, transactions,
receipts, coupons, orders, or order items. The function validates the bearer token via
Supabase Auth before using the server-side admin client.
