package com.example.smkoin;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import android.widget.ImageView;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Small HTTPS-only image loader for product image_url fields, with bounded memory cache. */
final class ProductImageLoader {
    private static final ExecutorService WORKER = Executors.newFixedThreadPool(2);
    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(4 * 1024 * 1024) {
        @Override protected int sizeOf(String key, Bitmap value) { return value.getByteCount(); }
    };

    private ProductImageLoader() { }

    static void load(ImageView view, String imageUrl, int fallbackResource) {
        view.setImageResource(fallbackResource);
        view.setTag(imageUrl);
        if (imageUrl == null || !imageUrl.startsWith("https://")) return;
        Bitmap cached = CACHE.get(imageUrl);
        if (cached != null) { view.setImageBitmap(cached); return; }
        WORKER.execute(() -> {
            Bitmap bitmap = download(imageUrl);
            if (bitmap == null) return;
            CACHE.put(imageUrl, bitmap);
            view.post(() -> {
                if (imageUrl.equals(view.getTag())) view.setImageBitmap(bitmap);
            });
        });
    }

    private static Bitmap download(String imageUrl) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(imageUrl).openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setInstanceFollowRedirects(true);
            try (InputStream input = connection.getInputStream()) { return BitmapFactory.decodeStream(input); }
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
