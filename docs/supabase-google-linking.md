# Google identity linking setup

The Android app starts the Supabase Auth identity-linking flow using PKCE and returns
to `smkoin://auth/callback`. The PKCE verifier is stored inside the encrypted local
session; the callback state and authenticated user ID are checked before exchanging
the one-time code. The app checks Auth's returned `identities` list before showing
Google as connected.

Before testing or using this flow:

1. Enable Google in Supabase Auth Providers and configure the Google OAuth client ID
   and client secret there. Do not place the Google client secret in the Android app.
2. Add `smkoin://auth/callback` to Supabase Auth's allowed Redirect URLs.
3. Configure the Google OAuth client with the Supabase project's Auth callback URL
   shown in the provider settings.
4. Enable Supabase manual identity linking. Supabase currently documents manual
   linking as beta and requires it to be enabled for linking an OAuth identity to an
   existing user.

Provider secrets and hosted dashboard settings have not been changed. If manual
linking is disabled, Google is not configured, or the redirect URL is not allowlisted,
the app displays the Auth service error and does not show Google as connected.
