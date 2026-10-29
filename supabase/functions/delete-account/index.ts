import { createClient } from "npm:@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json",
};

function response(status: number, message: string, deleted = false): Response {
  return new Response(JSON.stringify({ deleted, message }), { status, headers: corsHeaders });
}

async function listReceiptObjects(admin: ReturnType<typeof createClient>, folder: string): Promise<string[]> {
  const bucket = admin.storage.from("smkoin-receipts");
  const found: string[] = [];
  let offset = 0;
  for (;;) {
    const { data, error } = await bucket.list(folder, { limit: 100, offset });
    if (error) throw new Error("Could not list account receipt files.");
    for (const entry of data ?? []) {
      const path = `${folder}/${entry.name}`;
      if (entry.id === null && entry.metadata === null) found.push(...await listReceiptObjects(admin, path));
      else found.push(path);
    }
    if (!data || data.length < 100) break;
    offset += data.length;
  }
  return found;
}

Deno.serve(async (request: Request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (request.method !== "POST") return response(405, "Use POST to request account deletion.");

  const authorization = request.headers.get("Authorization") ?? "";
  const bearer = authorization.match(/^Bearer\s+(.+)$/i)?.[1];
  if (!bearer) return response(401, "A valid signed-in session is required.");

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? Deno.env.get("SUPABASE_PUBLISHABLE_KEY");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !anonKey || !serviceRoleKey) return response(500, "The account deletion service is not configured.");

  const caller = createClient(supabaseUrl, anonKey, {
    auth: { autoRefreshToken: false, persistSession: false },
  });
  const { data: userResult, error: userError } = await caller.auth.getUser(bearer);
  if (userError || !userResult.user) return response(401, "The signed-in session could not be verified.");

  const admin = createClient(supabaseUrl, serviceRoleKey, {
    auth: { autoRefreshToken: false, persistSession: false },
  });
  const userId = userResult.user.id;

  try {
    // Delete private objects first. A Storage failure aborts before the Auth user is removed.
    const receiptPaths = await listReceiptObjects(admin, userId);
    for (let offset = 0; offset < receiptPaths.length; offset += 100) {
      const { error } = await admin.storage.from("smkoin-receipts").remove(receiptPaths.slice(offset, offset + 100));
      if (error) return response(502, "Receipt files could not all be removed; the account remains active.");
    }

    // The local account-deletion migration makes all specified relational history cascade
    // from profiles. Auth deletes the user; profiles.id then cascades to those rows.
    const { error: deleteError } = await admin.auth.admin.deleteUser(userId, false);
    if (deleteError) {
      return response(502, "Receipt files were removed, but the account deletion was not confirmed. Contact support before retrying.");
    }
    return response(200, "The account and its linked records were deleted.", true);
  } catch (_error) {
    // Avoid logging or returning tokens, email addresses, or raw backend error details.
    return response(502, "The account deletion could not be completed. No success was reported.");
  }
});
