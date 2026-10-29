package com.example.smkoin;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.content.Context;
import android.security.KeyPairGeneratorSpec;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.util.Calendar;
import javax.security.auth.x500.X500Principal;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Stores the Supabase session as AES-GCM ciphertext protected by Android Keystore. */
final class SupabaseSessionStore {
    private static final String PREFS = "supabase_session";
    private static final String KEY_ALIAS = "smkoin_supabase_session_key";
    private static final String RSA_ALIAS = "smkoin_supabase_session_wrap_key";
    private final SharedPreferences preferences;
    private final Context context;

    SupabaseSessionStore(Context context) {
        this.context = context.getApplicationContext();
        preferences = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    synchronized void save(JSONObject session) throws Exception {
        SecretKey encryptionKey = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? getOrCreateKey() : createTemporaryAesKey();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey);
        byte[] encrypted = cipher.doFinal(session.toString().getBytes(StandardCharsets.UTF_8));
        SharedPreferences.Editor editor = preferences.edit()
                .putString("iv", Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                .putString("data", Base64.encodeToString(encrypted, Base64.NO_WRAP));
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsa.init(Cipher.ENCRYPT_MODE, getOrCreateRsaKeyPair().getPublic());
            editor.putString("wrapped_key", Base64.encodeToString(rsa.doFinal(encryptionKey.getEncoded()), Base64.NO_WRAP));
        }
        if (!editor.commit()) throw new IllegalStateException("Could not save the encrypted session.");
    }

    synchronized JSONObject read() {
        try {
            String ivText = preferences.getString("iv", null);
            String dataText = preferences.getString("data", null);
            if (ivText == null || dataText == null) return null;
            SecretKey decryptionKey;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) decryptionKey = getOrCreateKey();
            else {
                String wrapped = preferences.getString("wrapped_key", null);
                if (wrapped == null) return null;
                KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
                keyStore.load(null);
                Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                rsa.init(Cipher.DECRYPT_MODE, keyStore.getKey(RSA_ALIAS, null));
                decryptionKey = new SecretKeySpec(rsa.doFinal(Base64.decode(wrapped, Base64.NO_WRAP)), "AES");
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, decryptionKey, new GCMParameterSpec(128, Base64.decode(ivText, Base64.NO_WRAP)));
            byte[] plain = cipher.doFinal(Base64.decode(dataText, Base64.NO_WRAP));
            return new JSONObject(new String(plain, StandardCharsets.UTF_8));
        } catch (Exception invalidSession) {
            clear();
            return null;
        }
    }

    synchronized void clear() {
        preferences.edit().clear().commit();
    }

    synchronized void saveOAuthTransaction(String verifier, String state, String userId) throws Exception {
        JSONObject session = read();
        if (session == null) throw new IllegalStateException("Sign in before linking an account.");
        session.put("smkoin_oauth_link", new JSONObject().put("verifier", verifier).put("state", state).put("user_id", userId));
        save(session);
    }

    synchronized JSONObject readOAuthTransaction() {
        JSONObject session = read();
        return session == null ? null : session.optJSONObject("smkoin_oauth_link");
    }

    synchronized void clearOAuthTransaction() throws Exception {
        JSONObject session = read();
        if (session == null) return;
        session.remove("smkoin_oauth_link");
        save(session);
    }

    @android.annotation.TargetApi(Build.VERSION_CODES.M)
    private SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build());
        return generator.generateKey();
    }

    private SecretKey createTemporaryAesKey() throws Exception {
        // KeyProperties is API 23+, while the app still supports API 21 and 22.
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(256);
        return generator.generateKey();
    }

    private KeyPair getOrCreateRsaKeyPair() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(RSA_ALIAS)) {
            return new KeyPair(keyStore.getCertificate(RSA_ALIAS).getPublicKey(), (java.security.PrivateKey) keyStore.getKey(RSA_ALIAS, null));
        }
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        end.add(Calendar.YEAR, 25);
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
        generator.initialize(new KeyPairGeneratorSpec.Builder(context)
                .setAlias(RSA_ALIAS)
                .setSubject(new X500Principal("CN=SMKoin session encryption"))
                .setSerialNumber(BigInteger.ONE)
                .setStartDate(start.getTime())
                .setEndDate(end.getTime())
                .build());
        return generator.generateKeyPair();
    }
}
