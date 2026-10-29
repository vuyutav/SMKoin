package com.example.smkoin;

import org.json.JSONTokener;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.nio.charset.StandardCharsets;

/** Minimal HTTPS transport for Supabase Auth and PostgREST, keeping the app Java-only. */
final class SupabaseClient {
    interface UploadProgress { void onProgress(long sent, long total); }
    static final class RequestException extends Exception {
        final int statusCode;
        RequestException(int statusCode, String message) { super(message); this.statusCode = statusCode; }
    }

    private final String baseUrl = BuildConfig.SUPABASE_URL == null ? "" : BuildConfig.SUPABASE_URL.replaceAll("/+$", "");
    private final String apiKey = BuildConfig.SUPABASE_API_KEY == null ? "" : BuildConfig.SUPABASE_API_KEY;

    boolean isConfigured() {
        return !apiKey.isEmpty() && baseUrl.startsWith("https://");
    }

    Object request(String method, String route, JSONObject body, String accessToken) throws Exception {
        return request(method, route, body, accessToken, null);
    }

    Object request(String method, String route, JSONObject body, String accessToken, Map<String, String> headers) throws Exception {
        return requestInternal(method, route, body, null, accessToken, null, headers);
    }

    Object requestFile(String method, String route, File file, String accessToken, UploadProgress progress) throws Exception {
        return requestInternal(method, route, null, file, accessToken, progress, null);
    }

    String requestRedirectUrl(String route, String accessToken) throws Exception {
        if (!isConfigured()) throw new RequestException(0, "Supabase is not configured.");
        URL url = new URL(baseUrl + route);
        if (!"https".equalsIgnoreCase(url.getProtocol())) throw new RequestException(0, "Supabase URL must use HTTPS.");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(false);
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(20000);
            connection.setRequestProperty("apikey", apiKey);
            connection.setRequestProperty("Accept", "application/json");
            if (accessToken != null && !accessToken.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            int status = connection.getResponseCode();
            if (status >= 300 && status < 400) {
                String location = connection.getHeaderField("Location");
                if (location != null && !location.isEmpty()) return location;
            }
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String body = stream == null ? "" : readAll(stream);
            if (status < 200 || status >= 300) throw new RequestException(status, userMessage(status, body));
            try {
                JSONObject response = new JSONObject(body);
                String returnedUrl = response.optString("url", "");
                if (!returnedUrl.isEmpty()) return returnedUrl;
            } catch (Exception ignored) { }
            throw new RequestException(status, "Supabase did not return a Google authorization URL.");
        } finally { connection.disconnect(); }
    }

    private Object requestInternal(String method, String route, JSONObject body, File file,
                                   String accessToken, UploadProgress progress, Map<String, String> headers) throws Exception {
        if (!isConfigured()) throw new RequestException(0, "Supabase is not configured. Add project_url and publishable_key to the local .env file.");
        if (file != null && (!file.isFile() || file.length() <= 0L)) throw new RequestException(0, "The selected receipt image is unavailable.");
        URL url = new URL(baseUrl + route);
        if (!"https".equalsIgnoreCase(url.getProtocol())) throw new RequestException(0, "Supabase URL must use HTTPS.");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setRequestMethod(method);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(20000);
            connection.setRequestProperty("apikey", apiKey);
            connection.setRequestProperty("Accept", "application/json");
            if (headers != null) for (Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
            if (accessToken != null && !accessToken.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            } else if (route.startsWith("/rest/v1/") && isLegacyJwt(apiKey)) {
                // Legacy anon keys are JWTs and establish the public "anon" Postgres role.
                // New sb_publishable_* keys are API keys, not bearer JWTs.
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            }
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream output = connection.getOutputStream()) { output.write(bytes); }
            } else if (file != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "image/jpeg");
                connection.setRequestProperty("x-upsert", "false");
                connection.setFixedLengthStreamingMode(file.length());
                long total = file.length();
                long sent = 0L;
                byte[] buffer = new byte[16 * 1024];
                try (InputStream input = new FileInputStream(file); OutputStream output = connection.getOutputStream()) {
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        output.write(buffer, 0, count);
                        sent += count;
                        if (progress != null) progress.onProgress(sent, total);
                    }
                }
            }
            int status = connection.getResponseCode();
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String response = stream == null ? "" : readAll(stream);
            if (status < 200 || status >= 300) throw new RequestException(status, userMessage(status, response));
            if (response.trim().isEmpty()) return null;
            return new JSONTokener(response).nextValue();
        } finally {
            connection.disconnect();
        }
    }

    private static String readAll(InputStream input) throws Exception {
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = stream.read(buffer)) != -1) output.write(buffer, 0, count);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static String userMessage(int status, String response) {
        try {
            JSONObject error = new JSONObject(response);
            String message = error.optString("msg", error.optString("message", error.optString("error_description", "")));
            if (message.isEmpty()) message = error.optString("error", "");
            if (!message.isEmpty()) return message;
        } catch (Exception ignored) { }
        if (status == 401 || status == 403) return "Your session is no longer valid. Please sign in again.";
        if (status == 404) return "The requested SMKoin data was not found.";
        if (status >= 500) return "The SMKoin service is temporarily unavailable. Try again later.";
        return "The request could not be completed (HTTP " + status + ").";
    }

    private static boolean isLegacyJwt(String value) {
        return value != null && value.split("\\.").length == 3;
    }
}
