package com.example.smkoin;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.util.Collections;
import java.security.SecureRandom;
import java.security.MessageDigest;
import android.util.Base64;
import android.net.Uri;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** App data boundary for the supplied Supabase schema. All HTTP work runs off the UI thread. */
final class SupabaseRepository {
    interface Callback<T> { void onResult(T value, Exception error); }

    static final class AmbiguousRedemptionException extends Exception {
        AmbiguousRedemptionException() {
            super("We could not confirm whether redemption completed. Check History before retrying.");
        }
    }

    static final class AmbiguousReceiptSubmissionException extends Exception {
        AmbiguousReceiptSubmissionException() {
            super("The receipt upload completed, but the submission status could not be confirmed. Retry to check its status before creating another submission.");
        }
    }


    static final class UserProfile {
        final String id, fullName, email;
        UserProfile(String id, String fullName, String email) { this.id = id; this.fullName = fullName; this.email = email; }
    }

    private static volatile SupabaseRepository instance;
    private final Context appContext;
    private final SupabaseClient client = new SupabaseClient();
    private final SupabaseSessionStore sessionStore;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    static SupabaseRepository get(Context context) {
        if (instance == null) synchronized (SupabaseRepository.class) {
            if (instance == null) instance = new SupabaseRepository(context.getApplicationContext());
        }
        return instance;
    }

    private SupabaseRepository(Context context) { appContext = context.getApplicationContext(); sessionStore = new SupabaseSessionStore(appContext); }
    boolean isConfigured() { return client.isConfigured(); }
    boolean hasSession() { return sessionStore.read() != null; }
    String currentUserId() {
        JSONObject session = sessionStore.read();
        JSONObject user = session == null ? null : session.optJSONObject("user");
        String id = user == null ? null : user.optString("id", null);
        return id == null || id.trim().isEmpty() ? null : id;
    }

    /** Confirms the persisted session with Auth before protected UI is shown. */
    void validateSession(Callback<Boolean> callback) {
        run(callback, () -> {
            JSONObject session = sessionStore.read();
            if (session == null) return false;
            String token;
            try {
                token = validAccessToken();
            } catch (Exception error) {
                if (sessionStore.read() == null) return false;
                throw error;
            }
            if (token == null || token.isEmpty()) {
                sessionStore.clear();
                return false;
            }
            try {
                Object response = client.request("GET", "/auth/v1/user", null, token);
                if (!(response instanceof JSONObject) || !((JSONObject) response).has("id")) {
                    throw new Exception("Supabase returned an invalid session response.");
                }
                session.put("user", response);
                sessionStore.save(session);
                return true;
            } catch (SupabaseClient.RequestException error) {
                if (error.statusCode == 401) {
                    sessionStore.clear();
                    return false;
                }
                throw error;
            }
        });
    }

    UserProfile cachedUser() {
        JSONObject session = sessionStore.read();
        if (session == null) return null;
        JSONObject user = session.optJSONObject("user");
        if (user == null) return null;
        JSONObject metadata = user.optJSONObject("user_metadata");
        String name = metadata == null ? "" : metadata.optString("full_name", "");
        return new UserProfile(user.optString("id", ""), name, user.optString("email", ""));
    }

    void signIn(String email, String password, Callback<UserProfile> callback) {
        run(callback, () -> {
            JSONObject body = new JSONObject().put("email", email).put("password", password);
            JSONObject response = (JSONObject) client.request("POST", "/auth/v1/token?grant_type=password", body, null);
            saveSession(response);
            UserProfile profile = cachedUser();
            if (profile == null) throw new Exception("Sign-in response did not include a user profile.");
            return profile;
        });
    }

    void signUp(String name, String email, String password, Callback<UserProfile> callback) {
        run(callback, () -> {
            JSONObject body = new JSONObject().put("email", email).put("password", password)
                    .put("data", new JSONObject().put("full_name", name));
            JSONObject response = (JSONObject) client.request("POST", "/auth/v1/signup", body, null);
            if (response.has("access_token")) {
                saveSession(response);
                UserProfile profile = cachedUser();
                if (profile != null) return profile;
            }
            throw new Exception("Check your email to confirm your account, then sign in.");
        });
    }

    void signOut(Callback<Void> callback) {
        JSONObject session = sessionStore.read();
        String accessToken = session == null ? null : session.optString("access_token", null);
        String previousUserId = currentUserId();
        // Local logout is immediate; remote revocation is best effort and must not keep
        // this device authenticated while the network is slow or unavailable.
        sessionStore.clear();
        clearUserSpecificData(previousUserId);
        run(callback, () -> {
            if (accessToken != null && !accessToken.isEmpty()) {
                client.request("POST", "/auth/v1/logout", null, accessToken);
            }
            return null;
        });
    }

    void loadProducts(Callback<List<Produk>> callback) {
        run(callback, () -> {
            Object response = publicRequest("GET", "/rest/v1/products?select=id,name,description,image_url,weight_label,point_price,stock_quantity,category_id,categories(name)&is_active=eq.true&order=name.asc", null);
            JSONArray rows = asArray(response);
            List<Produk> products = new ArrayList<>();
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                JSONObject category = row.optJSONObject("categories");
                String categoryName = category == null ? "Other" : category.optString("name", "Other");
                products.add(new Produk(row.optString("id"), row.optString("name"), row.optInt("point_price"),
                        row.optString("description", ""), row.optString("image_url", ""), row.optString("weight_label", ""),
                        row.optInt("stock_quantity"), categoryName));
            }
            return products;
        });
    }

    void loadOffers(Callback<List<Offer>> callback) {
        run(callback, () -> {
            Object response = publicRequest("GET", "/rest/v1/offers?select=id,title,description,image_url&is_active=eq.true&order=created_at.desc", null);
            JSONArray rows = asArray(response);
            List<Offer> offers = new ArrayList<>();
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                offers.add(new Offer(row.optString("id"), row.optString("title"), row.optString("description", ""), row.optString("image_url", "")));
            }
            return offers;
        });
    }

    void loadBalance(Callback<Long> callback) {
        run(callback, () -> {
            String userId = requireUserId();
            JSONArray rows = asArray(authorizedRequest("GET", "/rest/v1/my_point_balance?select=user_id,point_balance&user_id=eq." + encode(userId), null));
            return parseBalance(userId, rows);
        });
    }

    static long parseBalance(String requestedUserId, JSONArray rows) throws Exception {
        int rowCount = rows == null ? 0 : rows.length();
        if (rowCount != 1) return parseBalance(requestedUserId, rowCount, null, null);
        JSONObject row = rows.getJSONObject(0);
        Object rawBalance = row.opt("point_balance");
        String balance = rawBalance == null || rawBalance == JSONObject.NULL ? null : rawBalance.toString();
        return parseBalance(requestedUserId, rowCount, row.optString("user_id", ""), balance);
    }

    static long parseBalance(String requestedUserId, int rowCount, String rowUserId, String rawBalance) {
        if (requestedUserId == null || requestedUserId.trim().isEmpty()) {
            throw new IllegalArgumentException("A signed-in user is required to read a point balance.");
        }
        if (rowCount != 1) {
            throw new IllegalStateException("The point balance view did not return exactly one row for this account.");
        }
        if (!requestedUserId.equals(rowUserId)) {
            throw new IllegalStateException("The point balance response did not match the signed-in account.");
        }
        if (rawBalance == null) {
            throw new IllegalStateException("The point balance response did not contain a value.");
        }
        try {
            return new BigDecimal(rawBalance).longValueExact();
        } catch (NumberFormatException | ArithmeticException invalidBalance) {
            throw new IllegalStateException("The point balance response was not a valid whole number.", invalidBalance);
        }
    }

    void loadProfile(Callback<UserProfile> callback) {
        run(callback, () -> {
            String userId = requireUserId();
            JSONArray rows = asArray(authorizedRequest("GET", "/rest/v1/profiles?select=id,full_name&id=eq." + encode(userId), null));
            if (rows.length() == 0) throw new Exception("Profile was not found.");
            JSONObject row = rows.getJSONObject(0);
            UserProfile authUser = cachedUser();
            return new UserProfile(row.optString("id"), row.optString("full_name", ""), authUser == null ? "" : authUser.email);
        });
    }

    void updateFullName(String fullName, Callback<UserProfile> callback) {
        run(callback, () -> {
            String normalized = fullName == null ? "" : fullName.trim();
            String validationError = AccountInputValidator.fullNameError(normalized);
            if (validationError != null) throw new Exception(validationError);
            String userId = requireUserId();
            JSONObject body = new JSONObject().put("full_name", normalized);
            Object response = authorizedRequest("PATCH", "/rest/v1/profiles?id=eq." + encode(userId), body,
                    Collections.singletonMap("Prefer", "return=representation"));
            JSONArray rows = asArray(response);
            if (rows.length() != 1 || !userId.equals(rows.getJSONObject(0).optString("id", ""))
                    || !normalized.equals(rows.getJSONObject(0).optString("full_name", ""))) {
                throw new Exception("Supabase did not confirm the profile update.");
            }
            UserProfile authUser = cachedUser();
            return new UserProfile(userId, normalized, authUser == null ? "" : authUser.email);
        });
    }

    void changePassword(String currentPassword, String newPassword, Callback<Void> callback) {
        run(callback, () -> {
            UserProfile profile = cachedUser();
            if (profile == null || profile.email.isEmpty()) throw new Exception("Sign in again before changing your password.");
            if (currentPassword == null || currentPassword.isEmpty()) throw new Exception("Enter your current password to verify your account.");
            String passwordError = AccountInputValidator.newPasswordError(currentPassword, newPassword);
            if (passwordError != null) throw new Exception(passwordError);

            // Reauthenticate explicitly. Only the Auth service sees the password.
            JSONObject authResponse = (JSONObject) client.request("POST", "/auth/v1/token?grant_type=password",
                    new JSONObject().put("email", profile.email).put("password", currentPassword), null);
            saveSession(authResponse);
            String userId = requireUserId();
            JSONObject update = (JSONObject) authorizedRequest("PUT", "/auth/v1/user",
                    new JSONObject().put("password", newPassword));
            if (!userId.equals(update.optString("id", ""))) throw new Exception("Supabase did not confirm the password update.");
            return null;
        });
    }

    void deleteAccount(String currentPassword, Callback<Void> callback) {
        run(callback, () -> {
            UserProfile profile = cachedUser();
            if (profile == null || profile.email.isEmpty()) throw new Exception("Sign in again before deleting this account.");
            if (currentPassword == null || currentPassword.isEmpty()) throw new Exception("Enter your current password to confirm deletion.");
            JSONObject authResponse = (JSONObject) client.request("POST", "/auth/v1/token?grant_type=password",
                    new JSONObject().put("email", profile.email).put("password", currentPassword), null);
            saveSession(authResponse);
            String accessToken = validAccessToken();
            if (accessToken == null) throw new Exception("Your session expired. Please sign in again.");
            client.request("POST", "/functions/v1/delete-account", new JSONObject(), accessToken);
            clearUserSpecificData(profile.id);
            sessionStore.clear();
            return null;
        });
    }

    void loadGoogleIdentityStatus(Callback<Boolean> callback) {
        run(callback, () -> {
            JSONObject user = (JSONObject) authorizedRequest("GET", "/auth/v1/user", null);
            JSONArray identities = user.optJSONArray("identities");
            if (identities == null) return false;
            for (int i = 0; i < identities.length(); i++) {
                if ("google".equalsIgnoreCase(identities.getJSONObject(i).optString("provider", ""))) return true;
            }
            return false;
        });
    }

    void startGoogleIdentityLink(Callback<String> callback) {
        run(callback, () -> {
            String userId = requireUserId();
            byte[] verifierBytes = new byte[32];
            SecureRandom random = new SecureRandom();
            random.nextBytes(verifierBytes);
            String verifier = Base64.encodeToString(verifierBytes, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
            String challenge = Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)),
                    Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
            String route = "/auth/v1/user/identities/authorize?provider=google&redirect_to=" + encode("smkoin://auth/callback")
                    + "&code_challenge=" + encode(challenge) + "&code_challenge_method=s256";
            String token = validAccessToken();
            if (token == null) throw new Exception("Sign in before connecting Google.");
            String url;
            try {
                url = client.requestRedirectUrl(route, token);
            } catch (SupabaseClient.RequestException error) {
                if (error.statusCode != 401) throw error;
                url = client.requestRedirectUrl(route, refreshSession());
            }
            String serverState = Uri.parse(url).getQueryParameter("state");
            if (serverState == null || serverState.isEmpty()) throw new Exception("Supabase returned an authorization URL without state.");
            // Bind the client transaction to the returned Auth state as well as the PKCE verifier.
            sessionStore.saveOAuthTransaction(verifier, serverState, userId);
            return url;
        });
    }

    void completeGoogleIdentityLink(String authorizationCode, String returnedState, Callback<Void> callback) {
        run(callback, () -> {
            JSONObject transaction = sessionStore.readOAuthTransaction();
            if (transaction == null) throw new Exception("This Google linking request has expired. Start again.");
            String expectedState = transaction.optString("state", "");
            if (expectedState.isEmpty() || returnedState == null || !expectedState.equals(returnedState)) {
                sessionStore.clearOAuthTransaction();
                throw new Exception("Google account verification failed. Start linking again.");
            }
            String code = authorizationCode == null ? "" : authorizationCode.trim();
            if (code.isEmpty()) {
                sessionStore.clearOAuthTransaction();
                throw new Exception("Google linking was cancelled or Supabase returned no authorization code.");
            }
            String expectedUserId = transaction.optString("user_id", "");
            JSONObject response = (JSONObject) client.request("POST", "/auth/v1/token?grant_type=pkce",
                    new JSONObject().put("auth_code", code).put("code_verifier", transaction.optString("verifier", "")), null);
            JSONObject linkedUser = response.optJSONObject("user");
            if (linkedUser == null || !expectedUserId.equals(linkedUser.optString("id", ""))) {
                sessionStore.clearOAuthTransaction();
                throw new Exception("The Google identity did not link to the signed-in SMKoin account.");
            }
            saveSession(response);
            JSONObject refreshedUser = (JSONObject) authorizedRequest("GET", "/auth/v1/user", null);
            if (!expectedUserId.equals(refreshedUser.optString("id", ""))) throw new Exception("The linked account could not be verified.");
            JSONArray identities = refreshedUser.optJSONArray("identities");
            boolean googleLinked = false;
            if (identities != null) for (int i = 0; i < identities.length(); i++) {
                if ("google".equalsIgnoreCase(identities.getJSONObject(i).optString("provider", ""))) googleLinked = true;
            }
            if (!googleLinked) throw new Exception("Supabase did not confirm that Google is linked.");
            JSONObject session = sessionStore.read();
            if (session != null) { session.put("user", refreshedUser); session.remove("smkoin_oauth_link"); sessionStore.save(session); }
            return null;
        });
    }

    void cancelGoogleIdentityLink() {
        try { sessionStore.clearOAuthTransaction(); } catch (Exception ignored) { }
    }

    void loadTransactions(Callback<List<PointTransaction>> callback) {
        run(callback, () -> {
            String userId = requireUserId();
            JSONArray rows = asArray(authorizedRequest("GET", "/rest/v1/point_transactions?select=id,transaction_type,description,points_delta,created_at&user_id=eq."
                    + encode(userId) + "&order=created_at.desc&limit=100", null));
            List<PointTransaction> transactions = new ArrayList<>();
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                transactions.add(new PointTransaction(row.optString("id"), row.optString("transaction_type"),
                        row.optString("description", ""), row.optString("created_at", ""), row.optInt("points_delta")));
            }
            return transactions;
        });
    }

    void redeem(String productId, int quantity, String location, Callback<String> callback) {
        run(callback, () -> {
            JSONObject body = new JSONObject().put("p_product_id", productId).put("p_quantity", quantity)
                    .put("p_delivery_location", location == null ? JSONObject.NULL : location.trim());
            Object response;
            try {
                response = authorizedRequest("POST", "/rest/v1/rpc/redeem_product", body);
            } catch (Exception error) {
                if (redemptionFailureMayHaveCommitted(error)) throw new AmbiguousRedemptionException();
                throw error;
            }
            String orderId = response instanceof String ? (String) response : "";
            if (!isCanonicalUuid(orderId)) throw new AmbiguousRedemptionException();
            return orderId;
        });
    }

    static boolean redemptionFailureMayHaveCommitted(Exception error) {
        if (error instanceof AmbiguousRedemptionException) return true;
        if (error instanceof SupabaseClient.RequestException) {
            return ((SupabaseClient.RequestException) error).statusCode >= 500;
        }
        String message = error == null ? "" : error.getMessage();
        if (message != null && (message.startsWith("Sign in to use your SMKoin account.")
                || message.startsWith("Your session expired."))) return false;
        return error != null;
    }

    private static boolean isCanonicalUuid(String value) {
        if (value == null || !value.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) return false;
        try {
            return java.util.UUID.fromString(value).toString().equalsIgnoreCase(value);
        } catch (IllegalArgumentException invalidUuid) {
            return false;
        }
    }

    void submitQrPayload(String payload, Callback<Void> callback) {
        run(callback, () -> {
            if (!isValidQrPayload(payload)) throw new Exception("This QR payload is empty, too long, or contains unsupported control characters.");
            String userId = requireUserId();
            JSONObject body = new JSONObject()
                    .put("user_id", userId)
                    .put("submission_type", "qr")
                    .put("qr_payload", payload.trim())
                    .put("status", "pending")
                    .put("points_awarded", 0);
            authorizedRequest("POST", "/rest/v1/receipt_submissions", body);
            return null;
        });
    }

    static boolean isValidQrPayload(String payload) {
        if (payload == null) return false;
        String value = payload.trim();
        if (value.isEmpty() || value.length() > 2048) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t') return false;
        }
        return true;
    }

    void submitReceipt(File image, String submissionId, SupabaseClient.UploadProgress progress, Callback<Void> callback) {
        run(callback, () -> {
            if (image == null || !image.isFile() || image.length() <= 0L) throw new Exception("Capture the receipt again before submitting.");
            if (image.length() > 10L * 1024L * 1024L) throw new Exception("This receipt photo exceeds 10 MiB. Retake it using a lower resolution camera setting.");
            if (submissionId == null || !submissionId.matches("[0-9a-fA-F-]{36}")) throw new Exception("The receipt submission is invalid. Retake the photo.");
            String userId = requireUserId();
            String objectPath = userId + "/" + submissionId + ".jpg";
            String storedPath = "smkoin-receipts/" + objectPath;

            if (receiptRecordExists(userId, storedPath)) return null;
            try {
                uploadReceiptFile(objectPath, image, progress);
            } catch (SupabaseClient.RequestException uploadError) {
                // Reusing the same submission UUID after a lost upload response may encounter
                // the object created by the first attempt. Continue to check/insert its row.
                if (uploadError.statusCode != 409) throw uploadError;
            }

            JSONObject body = new JSONObject().put("user_id", userId)
                    .put("submission_type", "receipt")
                    .put("receipt_image_url", storedPath)
                    .put("status", "pending")
                    .put("points_awarded", 0);
            try {
                authorizedRequest("POST", "/rest/v1/receipt_submissions", body);
            } catch (Exception error) {
                if (redemptionFailureMayHaveCommitted(error)) throw new AmbiguousReceiptSubmissionException();
                throw error;
            }
            return null;
        });
    }

    private boolean receiptRecordExists(String userId, String storedPath) throws Exception {
        String path = "/rest/v1/receipt_submissions?select=id&user_id=eq." + encode(userId)
                + "&receipt_image_url=eq." + encode(storedPath) + "&limit=1";
        JSONArray rows = asArray(authorizedRequest("GET", path, null));
        return rows.length() > 0;
    }

    private Object uploadReceiptFile(String objectPath, File image, SupabaseClient.UploadProgress progress) throws Exception {
        String route = "/storage/v1/object/smkoin-receipts/" + encodePath(objectPath);
        String token = validAccessToken();
        if (token == null) throw new Exception("Sign in to submit a receipt.");
        try {
            return client.requestFile("POST", route, image, token, progress);
        } catch (SupabaseClient.RequestException error) {
            if (error.statusCode != 401) throw error;
            return client.requestFile("POST", route, image, refreshSession(), progress);
        }
    }

    private static String encodePath(String value) {
        String[] parts = value.split("/");
        StringBuilder encoded = new StringBuilder();
        for (String part : parts) {
            if (encoded.length() > 0) encoded.append('/');
            encoded.append(encode(part));
        }
        return encoded.toString();
    }

    private Object publicRequest(String method, String path, JSONObject body) throws Exception {
        JSONObject session = sessionStore.read();
        long expiresAt = session == null ? 0L : session.optLong("expires_at", 0L);
        String token = session == null || expiresAt <= System.currentTimeMillis() / 1000L + 60L
                ? null : session.optString("access_token", null);
        return client.request(method, path, body, token);
    }

    private Object authorizedRequest(String method, String path, JSONObject body) throws Exception {
        return authorizedRequest(method, path, body, null);
    }

    private Object authorizedRequest(String method, String path, JSONObject body, java.util.Map<String, String> headers) throws Exception {
        String token = validAccessToken();
        if (token == null) throw new Exception("Sign in to use your SMKoin account.");
        try {
            return client.request(method, path, body, token, headers);
        } catch (SupabaseClient.RequestException error) {
            if (error.statusCode != 401) throw error;
            String refreshed = refreshSession();
            return client.request(method, path, body, refreshed, headers);
        }
    }

    private String validAccessToken() throws Exception {
        JSONObject session = sessionStore.read();
        if (session == null) return null;
        long expiresAt = session.optLong("expires_at", 0L);
        if (expiresAt > System.currentTimeMillis() / 1000L + 60L) return session.optString("access_token", null);
        return refreshSession();
    }

    private String refreshSession() throws Exception {
        JSONObject current = sessionStore.read();
        if (current == null || current.optString("refresh_token", "").isEmpty()) {
            sessionStore.clear();
            throw new Exception("Your session expired. Please sign in again.");
        }
        try {
            JSONObject response = (JSONObject) client.request("POST", "/auth/v1/token?grant_type=refresh_token",
                    new JSONObject().put("refresh_token", current.getString("refresh_token")), null);
            if (!response.has("user") && current.has("user")) response.put("user", current.getJSONObject("user"));
            if (response.optString("access_token", "").isEmpty()
                    || response.optString("refresh_token", "").isEmpty()) {
                throw new Exception("Supabase returned an incomplete refreshed session.");
            }
            saveSession(response);
            return response.getString("access_token");
        } catch (SupabaseClient.RequestException error) {
            if (error.statusCode == 400 || error.statusCode == 401 || error.statusCode == 403) {
                sessionStore.clear();
                throw new Exception("Your session expired. Please sign in again.");
            }
            throw error;
        }
    }

    private void saveSession(JSONObject session) throws Exception {
        long expiresAt = session.optLong("expires_at", 0L);
        if (expiresAt == 0L) expiresAt = System.currentTimeMillis() / 1000L + session.optLong("expires_in", 3600L);
        session.put("expires_at", expiresAt);
        sessionStore.save(session);
    }

    private String requireUserId() throws Exception {
        JSONObject session = sessionStore.read();
        JSONObject user = session == null ? null : session.optJSONObject("user");
        String id = user == null ? "" : user.optString("id", "");
        if (id.isEmpty()) throw new Exception("Sign in to use your SMKoin account.");
        return id;
    }

    private <T> void run(Callback<T> callback, Work<T> work) {
        worker.execute(() -> {
            T result = null;
            Exception failure = null;
            try { result = work.run(); }
            catch (Exception error) { failure = error; }
            T finalResult = result;
            Exception finalFailure = failure;
            main.post(() -> callback.onResult(finalResult, finalFailure));
        });
    }

    private interface Work<T> { T run() throws Exception; }
    private static JSONArray asArray(Object value) throws Exception {
        if (value instanceof JSONArray) return (JSONArray) value;
        throw new Exception("The SMKoin service returned an unexpected response.");
    }
    private static String encode(String value) {
        try { return URLEncoder.encode(value, StandardCharsets.UTF_8.name()); }
        catch (Exception ignored) { return value; }
    }

    void clearUserSpecificData(String userId) {
        if (userId == null || userId.trim().isEmpty()) return;
        appContext.getSharedPreferences("scan_submissions", Context.MODE_PRIVATE)
                .edit().remove("qr_" + userId).apply();
    }
}
