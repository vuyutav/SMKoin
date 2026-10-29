package com.example.smkoin;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.net.Uri;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.common.HybridBinarizer;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Camera-backed receipt and QR capture. Awards remain controlled by trusted review. */
public class ScanFragment extends Fragment {
    private static final int AUTH_REQUEST = 42;
    private static final String MODE_RECEIPT = "Receipt";
    private static final String MODE_QR = "QR Code";
    private String mode = MODE_RECEIPT;
    private TextView receiptTab, qrTab, cameraTitle, guidance, submitButton, captureButton;
    private EditText payloadField;
    private FrameLayout previewFrame;
    private PreviewView previewView;
    private SupabaseRepository repository;
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Set<String> submittedPayloadHashes = new HashSet<>();
    private final Map<DecodeHintType, Object> qrHints = new HashMap<>();
    private boolean submitting;
    private boolean cameraPermissionRequested;
    private boolean cameraPermissionDenied;
    private boolean cameraPermissionInFlight;
    private boolean permissionDeniedThisView;
    private boolean qrDetected;
    private File capturedReceiptFile;
    private String receiptSubmissionId;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        repository = SupabaseRepository.get(requireContext());
        loadSubmittedPayloadHashes();
        cameraExecutor = Executors.newSingleThreadExecutor();
        qrHints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        cameraPermissionRequested = requireContext().getSharedPreferences("scan_camera", Context.MODE_PRIVATE)
                .getBoolean("requested", false);
        cameraPermissionDenied = requireContext().getSharedPreferences("scan_camera", Context.MODE_PRIVATE)
                .getBoolean("denied", false);
        if (state != null) {
            mode = state.getString("mode", MODE_RECEIPT);
            String savedPath = state.getString("receipt_file", null);
            receiptSubmissionId = state.getString("receipt_submission_id", null);
            if (savedPath != null) {
                File savedFile = new File(savedPath);
                if (savedFile.isFile()) capturedReceiptFile = savedFile;
            }
        }

        ScrollView scroll = new ScrollView(requireContext());
        scroll.setBackgroundColor(AppUi.SOFT);
        LinearLayout page = AppUi.column(requireContext());
        page.setPadding(AppUi.dp(requireContext(), 20), AppUi.dp(requireContext(), 18), AppUi.dp(requireContext(), 20), AppUi.dp(requireContext(), 24));
        scroll.addView(page);
        page.addView(AppUi.text(requireContext(), "Scan & Earn", 24, true, AppUi.INK));
        page.addView(AppUi.text(requireContext(), "Scan a QR code or submit a receipt for trusted review", 14, false, AppUi.MUTED));

        LinearLayout modes = AppUi.row(requireContext());
        receiptTab = modeButton(MODE_RECEIPT);
        qrTab = modeButton(MODE_QR);
        modes.addView(receiptTab, new LinearLayout.LayoutParams(0, AppUi.dp(requireContext(), 52), 1));
        LinearLayout.LayoutParams qrParams = new LinearLayout.LayoutParams(0, AppUi.dp(requireContext(), 52), 1);
        qrParams.leftMargin = AppUi.dp(requireContext(), 10);
        modes.addView(qrTab, qrParams);
        LinearLayout.LayoutParams modesParams = new LinearLayout.LayoutParams(-1, -2);
        modesParams.topMargin = AppUi.dp(requireContext(), 18);
        page.addView(modes, modesParams);
        receiptTab.setOnClickListener(v -> selectMode(MODE_RECEIPT));
        qrTab.setOnClickListener(v -> selectMode(MODE_QR));

        previewFrame = new FrameLayout(requireContext());
        previewFrame.setBackground(AppUi.background(0xFFE8EAE4, AppUi.dp(requireContext(), 24), 0));
        previewFrame.setClipToOutline(true);
        previewView = new PreviewView(requireContext());
        previewView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        previewFrame.addView(previewView, new FrameLayout.LayoutParams(-1, -1));
        cameraTitle = AppUi.text(requireContext(), "Camera preview", 17, true, AppUi.INK);
        cameraTitle.setGravity(Gravity.CENTER);
        cameraTitle.setBackground(AppUi.background(0xBFFFFFFF, AppUi.dp(requireContext(), 12), 0));
        FrameLayout.LayoutParams titleParams = new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER);
        titleParams.setMargins(AppUi.dp(requireContext(), 22), AppUi.dp(requireContext(), 22), AppUi.dp(requireContext(), 22), AppUi.dp(requireContext(), 22));
        previewFrame.addView(cameraTitle, titleParams);
        previewFrame.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCameraIfAllowed();
            else { permissionDeniedThisView = false; requestCameraPermission(); }
        });
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(-1, AppUi.dp(requireContext(), 280));
        previewParams.topMargin = AppUi.dp(requireContext(), 18);
        page.addView(previewFrame, previewParams);

        guidance = AppUi.text(requireContext(), "Allow camera access to begin.", 14, false, AppUi.MUTED);
        guidance.setGravity(Gravity.CENTER);
        guidance.setPadding(AppUi.dp(requireContext(), 8), AppUi.dp(requireContext(), 10), AppUi.dp(requireContext(), 8), AppUi.dp(requireContext(), 4));
        page.addView(guidance);

        captureButton = AppUi.text(requireContext(), "Capture receipt", 15, true, AppUi.INK);
        captureButton.setGravity(Gravity.CENTER);
        captureButton.setBackground(AppUi.background(AppUi.WHITE, AppUi.dp(requireContext(), 16), 0));
        LinearLayout.LayoutParams captureParams = new LinearLayout.LayoutParams(-1, AppUi.dp(requireContext(), 50));
        captureParams.topMargin = AppUi.dp(requireContext(), 10);
        page.addView(captureButton, captureParams);
        captureButton.setOnClickListener(v -> {
            if (capturedReceiptFile != null && capturedReceiptFile.isFile()) showReceiptPreview();
            else captureReceipt();
        });

        payloadField = new EditText(requireContext());
        payloadField.setHint("Paste QR payload (manual fallback)");
        payloadField.setMinLines(2);
        payloadField.setGravity(Gravity.TOP | Gravity.START);
        payloadField.setTextSize(14);
        payloadField.setPadding(AppUi.dp(requireContext(), 14), AppUi.dp(requireContext(), 12), AppUi.dp(requireContext(), 14), AppUi.dp(requireContext(), 12));
        payloadField.setBackground(AppUi.background(AppUi.WHITE, AppUi.dp(requireContext(), 14), 0));
        LinearLayout.LayoutParams payloadParams = new LinearLayout.LayoutParams(-1, AppUi.dp(requireContext(), 88));
        payloadParams.topMargin = AppUi.dp(requireContext(), 12);
        page.addView(payloadField, payloadParams);

        page.addView(AppUi.text(requireContext(), "Submissions stay pending until reviewed. Scanning or uploading never awards points by itself.", 13, false, AppUi.MUTED));
        submitButton = AppUi.text(requireContext(), "Submit for verification", 16, true, AppUi.INK);
        submitButton.setGravity(Gravity.CENTER);
        submitButton.setBackground(AppUi.background(AppUi.LIME, AppUi.dp(requireContext(), 18), 0));
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-1, AppUi.dp(requireContext(), 52));
        buttonParams.topMargin = AppUi.dp(requireContext(), 16);
        page.addView(submitButton, buttonParams);
        submitButton.setOnClickListener(v -> onSubmit());

        cameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (!isAdded()) return;
            cameraPermissionInFlight = false;
            if (granted) {
                cameraPermissionDenied = false;
                requireContext().getSharedPreferences("scan_camera", Context.MODE_PRIVATE).edit().putBoolean("denied", false).apply();
                startCameraIfAllowed();
            } else {
                boolean previouslyDenied = cameraPermissionDenied;
                cameraPermissionDenied = true;
                permissionDeniedThisView = true;
                requireContext().getSharedPreferences("scan_camera", Context.MODE_PRIVATE).edit().putBoolean("denied", true).apply();
                handlePermissionDenied(previouslyDenied);
            }
        });
        selectMode(mode);
        if (!repository.isConfigured()) {
            submitButton.setEnabled(false);
            submitButton.setAlpha(0.5f);
        }
        if (capturedReceiptFile != null) showReceiptPreview();
        return scroll;
    }

    private TextView modeButton(String label) {
        TextView button = AppUi.text(requireContext(), label, 14, true, AppUi.INK);
        button.setGravity(Gravity.CENTER);
        return button;
    }

    private void selectMode(String value) {
        mode = value;
        if (cameraProvider != null) cameraProvider.unbindAll();
        imageCapture = null;
        boolean isQr = MODE_QR.equals(value);
        if (receiptTab != null) receiptTab.setBackground(AppUi.background(!isQr ? AppUi.LIME : AppUi.WHITE, AppUi.dp(requireContext(), 16), 0));
        if (qrTab != null) qrTab.setBackground(AppUi.background(isQr ? AppUi.LIME : AppUi.WHITE, AppUi.dp(requireContext(), 16), 0));
        if (payloadField != null) payloadField.setVisibility(isQr ? View.VISIBLE : View.GONE);
        if (captureButton != null) captureButton.setVisibility(isQr ? View.GONE : View.VISIBLE);
        if (submitButton != null) submitButton.setVisibility(isQr ? View.VISIBLE : View.GONE);
        if (cameraTitle != null) cameraTitle.setText(isQr ? "Point at a QR code" : "Frame the full receipt");
        if (guidance != null) guidance.setText(isQr
                ? "QR format rules are not defined. Scanned text is sent only for review."
                : "Capture a clear receipt photo, review it, then submit.");
        if (isAdded()) startCameraIfAllowed();
    }

    @Override public void onStart() {
        super.onStart();
        startCameraIfAllowed();
    }

    @Override public void onResume() {
        super.onResume();
        if (isAdded() && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCameraIfAllowed();
        }
    }

    @Override public void onPause() {
        stopCamera();
        super.onPause();
    }

    private void startCameraIfAllowed() {
        if (!isAdded() || previewView == null || !isVisible()) return;
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraTitle.setText("Camera permission required");
            if (!permissionDeniedThisView) requestCameraPermission();
            return;
        }
        cameraTitle.setText(MODE_QR.equals(mode) ? "Point at a QR code" : "Frame the full receipt");
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(requireContext());
        future.addListener(() -> {
            if (!isAdded() || previewView == null || !isVisible()) return;
            try {
                cameraProvider = future.get();
                bindCameraUseCases();
            } catch (Exception cameraError) {
                cameraTitle.setText("Camera unavailable");
                guidance.setText("The camera could not start. Check camera access and try again.");
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void requestCameraPermission() {
        if (!isAdded() || cameraPermissionInFlight) return;
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            permissionDeniedThisView = true;
            new AlertDialog.Builder(requireContext()).setTitle("Camera access")
                    .setMessage("SMKoin needs the camera to scan QR codes and capture receipt photos.")
                    .setNegativeButton("Not now", null)
                    .setPositiveButton("Continue", (dialog, which) -> { permissionDeniedThisView = false; requestCameraPermissionDirect(); }).show();
        } else if (cameraPermissionDenied) {
            permissionDeniedThisView = true;
            new AlertDialog.Builder(requireContext()).setTitle("Camera permission is off")
                    .setMessage("Enable camera access in Android Settings to scan a QR code or capture a receipt.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Open Settings", (dialog, which) -> startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", requireContext().getPackageName(), null)))).show();
        } else requestCameraPermissionDirect();
    }

    private void requestCameraPermissionDirect() {
        cameraPermissionInFlight = true;
        cameraPermissionRequested = true;
        requireContext().getSharedPreferences("scan_camera", Context.MODE_PRIVATE).edit().putBoolean("requested", true).apply();
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void handlePermissionDenied(boolean previouslyDenied) {
        if (guidance != null) guidance.setText("Camera access was denied. Manual QR entry is still available.");
        if (previouslyDenied && !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            new AlertDialog.Builder(requireContext()).setTitle("Camera permission is off")
                    .setMessage("You can enable camera access in Android Settings. QR payloads can still be entered manually.")
                    .setPositiveButton("Open Settings", (dialog, which) -> startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", requireContext().getPackageName(), null))))
                    .setNegativeButton("Cancel", null).show();
        }
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null || previewView == null) return;
        cameraProvider.unbindAll();
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        try {
            if (MODE_QR.equals(mode)) {
                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                analysis.setAnalyzer(cameraExecutor, this::analyzeFrame);
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis);
            } else {
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setJpegQuality(85)
                        .build();
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture);
            }
        } catch (Exception bindError) {
            imageCapture = null;
            cameraTitle.setText("Camera unavailable");
            guidance.setText("This device could not open the camera. You can still enter a QR payload manually.");
        }
    }

    private void analyzeFrame(ImageProxy proxy) {
        try {
            if (proxy.getPlanes().length == 0) return;
            ImageProxy.PlaneProxy plane = proxy.getPlanes()[0];
            ByteBuffer buffer = plane.getBuffer();
            int width = proxy.getWidth(), height = proxy.getHeight();
            byte[] luminance = copyLuminance(buffer, width, height, plane.getRowStride(), plane.getPixelStride());
            LuminanceSource source = new PlanarYUVLuminanceSource(luminance, width, height, 0, 0, width, height, false);
            int rotation = proxy.getImageInfo().getRotationDegrees();
            int clockwiseTurns = ((rotation % 360) + 360) % 360 / 90;
            int counterClockwiseTurns = (4 - clockwiseTurns) % 4;
            for (int i = 0; i < counterClockwiseTurns; i++) source = source.rotateCounterClockwise();
            Result result = new QRCodeReader().decode(new BinaryBitmap(new HybridBinarizer(source)), qrHints);
            String text = result.getText();
            mainHandler.post(() -> onQrDetected(text));
        } catch (ReaderException ignored) {
            // No QR in this frame is the expected live-preview state.
        } catch (RuntimeException ignored) {
            // Camera frames can be transiently incomplete while the use case is switching.
        } finally {
            proxy.close();
        }
    }

    private static byte[] copyLuminance(ByteBuffer source, int width, int height, int rowStride, int pixelStride) {
        ByteBuffer buffer = source.duplicate();
        int start = buffer.position();
        byte[] output = new byte[width * height];
        for (int row = 0; row < height; row++) {
            int rowStart = start + row * rowStride;
            for (int column = 0; column < width; column++) {
                output[row * width + column] = buffer.get(rowStart + column * pixelStride);
            }
        }
        return output;
    }

    private void onQrDetected(String payload) {
        if (!isAdded() || !MODE_QR.equals(mode) || submitting || qrDetected) return;
        qrDetected = true;
        if (!SupabaseRepository.isValidQrPayload(payload)) {
            guidance.setText("This QR code has no usable payload. Try another code.");
            mainHandler.postDelayed(() -> qrDetected = false, 1800L);
            return;
        }
        String hash = hashPayload(payload);
        if (submittedPayloadHashes.contains(hash)) {
            guidance.setText("This QR code was already submitted in this scan session.");
            mainHandler.postDelayed(() -> qrDetected = false, 2200L);
            return;
        }
        payloadField.setText(payload);
        guidance.setText("QR captured. Review the payload and submit it for verification.");
    }

    private void onSubmit() {
        if (!repository.isConfigured()) { guidance.setText("Supabase is not configured; no submission was sent."); return; }
        if (!repository.hasSession()) {
            Intent login = new Intent(requireContext(), AuthActivity.class);
            login.putExtra(AuthActivity.EXTRA_PROTECTED_CALLER, true);
            startActivityForResult(login, AUTH_REQUEST);
            return;
        }
        if (MODE_QR.equals(mode)) submitPayload();
    }

    private void submitPayload() {
        if (submitting) return;
        String payload = payloadField.getText().toString().trim();
        if (!SupabaseRepository.isValidQrPayload(payload)) {
            payloadField.setError("Enter a QR payload of at most 2,048 characters.");
            guidance.setText("The QR payload is empty or invalid.");
            return;
        }
        String hash = hashPayload(payload);
        if (submittedPayloadHashes.contains(hash)) {
            guidance.setText("This QR code has already been submitted in this scan session.");
            return;
        }
        submitting = true;
        submitButton.setEnabled(false);
        submitButton.setText("Submitting...");
        guidance.setText("Sending this QR for trusted review...");
        repository.submitQrPayload(payload, (ignored, error) -> {
            if (!isAdded()) return;
            submitting = false;
            submitButton.setEnabled(true);
            submitButton.setText("Submit QR for verification");
            qrDetected = false;
            if (error != null) guidance.setText("Submission failed: " + error.getMessage());
            else {
                submittedPayloadHashes.add(hash);
                persistSubmittedPayloadHashes();
                payloadField.setText("");
                guidance.setText("Submitted and pending review. No points have been awarded.");
            }
        });
    }

    private void captureReceipt() {
        if (!repository.isConfigured()) { guidance.setText("Supabase is not configured; no receipt was captured for submission."); return; }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionDeniedThisView = false;
            requestCameraPermission();
            return;
        }
        if (!repository.hasSession()) {
            Intent login = new Intent(requireContext(), AuthActivity.class);
            login.putExtra(AuthActivity.EXTRA_PROTECTED_CALLER, true);
            startActivityForResult(login, AUTH_REQUEST);
            return;
        }
        if (imageCapture == null || submitting) { guidance.setText("The camera is not ready yet. Try again in a moment."); return; }
        try {
            File output = File.createTempFile("smkoin_receipt_", ".jpg", requireContext().getCacheDir());
            ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(output).build();
            imageCapture.takePicture(options, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
                @Override public void onImageSaved(@NonNull ImageCapture.OutputFileResults result) {
                    mainHandler.post(() -> {
                        if (!isAdded()) { output.delete(); return; }
                        capturedReceiptFile = output;
                        receiptSubmissionId = UUID.randomUUID().toString();
                        showReceiptPreview();
                    });
                }
                @Override public void onError(@NonNull ImageCaptureException error) {
                    output.delete();
                    mainHandler.post(() -> { if (isAdded()) guidance.setText("Receipt capture failed. Check camera access and try again."); });
                }
            });
            guidance.setText("Capturing receipt...");
        } catch (IOException error) {
            guidance.setText("Could not prepare a temporary receipt image. Check device storage and retry.");
        }
    }

    private void showReceiptPreview() {
        if (!isAdded() || capturedReceiptFile == null || !capturedReceiptFile.isFile()) return;
        cameraTitle.setText("Receipt preview ready");
        Bitmap bitmap = loadPreviewBitmap(capturedReceiptFile);
        if (bitmap == null) { guidance.setText("Could not preview this image. Retake the receipt."); return; }
        ImageView image = new ImageView(requireContext());
        image.setImageBitmap(bitmap);
        image.setAdjustViewBounds(true);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int maxHeight = AppUi.dp(requireContext(), 420);
        image.setMaxHeight(maxHeight);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Review receipt")
                .setView(image)
                .setNegativeButton("Retake", (d, which) -> {
                    deleteCapturedReceipt();
                    guidance.setText("Frame the full receipt, then capture again.");
                })
                .setPositiveButton("Submit receipt", (d, which) -> uploadReceipt())
                .create();
        dialog.setOnDismissListener(d -> { if (!bitmap.isRecycled()) bitmap.recycle(); });
        dialog.show();
        dialog.getWindow().setLayout(AppUi.dp(requireContext(), 340), -2);
    }

    private Bitmap loadPreviewBitmap(File file) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = Math.max(1, Math.min(bounds.outWidth / 900, bounds.outHeight / 1200));
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        if (bitmap == null) return null;
        try {
            int orientation = new ExifInterface(file.getAbsolutePath()).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            Matrix matrix = new Matrix();
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) matrix.postRotate(90);
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) matrix.postRotate(180);
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) matrix.postRotate(270);
            if (!matrix.isIdentity()) {
                Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                if (rotated != bitmap) bitmap.recycle();
                bitmap = rotated;
            }
        } catch (IOException ignored) { }
        return bitmap;
    }

    private void uploadReceipt() {
        if (submitting || capturedReceiptFile == null || receiptSubmissionId == null) return;
        if (!repository.hasSession()) { guidance.setText("Your session expired. Sign in again before submitting this receipt."); return; }
        submitting = true;
        captureButton.setEnabled(false);
        captureButton.setText("Preparing...");
        guidance.setText("Preparing a private upload...");
        File file = capturedReceiptFile;
        String id = receiptSubmissionId;
        File cacheDirectory = requireContext().getCacheDir();
        cameraExecutor.execute(() -> {
            File uploadFile = null;
            Exception compressionError = null;
            try { uploadFile = compressReceiptImage(file, cacheDirectory); }
            catch (Exception error) { compressionError = error; }
            File finalUploadFile = uploadFile;
            Exception finalCompressionError = compressionError;
            mainHandler.post(() -> {
                if (!isAdded()) {
                    if (finalUploadFile != null) finalUploadFile.delete();
                    if (file.exists()) file.delete();
                    return;
                }
                if (finalCompressionError != null || finalUploadFile == null) {
                    submitting = false;
                    captureButton.setEnabled(true);
                    captureButton.setText("Retry receipt submission");
                    guidance.setText(finalCompressionError == null ? "Could not prepare the receipt image." : finalCompressionError.getMessage());
                    return;
                }
                if (capturedReceiptFile != null && !capturedReceiptFile.equals(finalUploadFile)) capturedReceiptFile.delete();
                capturedReceiptFile = finalUploadFile;
                sendReceiptFile(finalUploadFile, id);
            });
        });
    }

    private File compressReceiptImage(File source, File cacheDirectory) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(source.getAbsolutePath(), bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new IOException("The receipt image could not be read. Retake it.");
        int maxSide = 2000;
        for (int attempt = 0; attempt < 5; attempt++) {
            int sample = 1;
            while (Math.max(bounds.outWidth, bounds.outHeight) / sample > maxSide) sample *= 2;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample;
            Bitmap bitmap = BitmapFactory.decodeFile(source.getAbsolutePath(), options);
            if (bitmap == null) throw new IOException("The receipt image could not be decoded. Retake it.");
            File output = File.createTempFile("smkoin_receipt_upload_", ".jpg", cacheDirectory);
            try {
                int orientation = new ExifInterface(source.getAbsolutePath()).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                Matrix matrix = new Matrix();
                if (orientation == ExifInterface.ORIENTATION_ROTATE_90) matrix.postRotate(90);
                else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) matrix.postRotate(180);
                else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) matrix.postRotate(270);
                Bitmap uploadBitmap = bitmap;
                if (!matrix.isIdentity()) uploadBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                try (java.io.FileOutputStream stream = new java.io.FileOutputStream(output)) {
                    if (!uploadBitmap.compress(Bitmap.CompressFormat.JPEG, 82 - attempt * 4, stream)) throw new IOException("The receipt image could not be compressed.");
                }
                if (uploadBitmap != bitmap) uploadBitmap.recycle();
                if (output.length() <= 10L * 1024L * 1024L) return output;
            } catch (IOException error) {
                output.delete();
                throw error;
            } finally {
                if (!bitmap.isRecycled()) bitmap.recycle();
            }
            output.delete();
            maxSide = maxSide * 3 / 4;
        }
        throw new IOException("The receipt photo is still too large after resizing. Retake a closer, clearer image.");
    }

    private void sendReceiptFile(File file, String id) {
        captureButton.setText("Uploading...");
        guidance.setText("Uploading receipt: 0%");
        repository.submitReceipt(file, id, (sent, total) -> {
            int percent = total <= 0 ? 0 : (int) Math.min(100, sent * 100L / total);
            mainHandler.post(() -> { if (isAdded() && guidance != null) guidance.setText("Uploading receipt: " + percent + "%"); });
        }, (ignored, error) -> {
            if (!isAdded()) {
                if (file.exists()) file.delete();
                if (capturedReceiptFile != null && capturedReceiptFile.exists()) capturedReceiptFile.delete();
                return;
            }
            submitting = false;
            captureButton.setEnabled(true);
            captureButton.setText("Retry receipt submission");
            if (error != null) guidance.setText(error.getMessage());
            else {
                guidance.setText("Receipt submitted and pending review. No points have been awarded.");
                deleteCapturedReceipt();
                captureButton.setText("Capture receipt");
            }
        });
    }

    private void deleteCapturedReceipt() {
        if (capturedReceiptFile != null && capturedReceiptFile.exists()) capturedReceiptFile.delete();
        capturedReceiptFile = null;
        receiptSubmissionId = null;
    }

    private void stopCamera() {
        if (cameraProvider != null) cameraProvider.unbindAll();
        imageCapture = null;
    }

    private static String hashPayload(String payload) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder text = new StringBuilder(hash.length * 2);
            for (byte value : hash) text.append(String.format(java.util.Locale.ROOT, "%02x", value & 0xff));
            return text.toString();
        } catch (Exception impossible) { return Integer.toHexString(payload.hashCode()); }
    }

    private void loadSubmittedPayloadHashes() {
        String userId = repository.currentUserId();
        if (userId == null) return;
        Set<String> saved = requireContext().getSharedPreferences("scan_submissions", Context.MODE_PRIVATE)
                .getStringSet("qr_" + userId, Collections.emptySet());
        if (saved != null) submittedPayloadHashes.addAll(saved);
    }

    private void persistSubmittedPayloadHashes() {
        String userId = repository.currentUserId();
        if (userId == null) return;
        Set<String> bounded = new HashSet<>(submittedPayloadHashes);
        while (bounded.size() > 100) bounded.remove(bounded.iterator().next());
        submittedPayloadHashes.clear();
        submittedPayloadHashes.addAll(bounded);
        requireContext().getSharedPreferences("scan_submissions", Context.MODE_PRIVATE)
                .edit().putStringSet("qr_" + userId, new HashSet<>(bounded)).apply();
    }

    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString("mode", mode);
        if (capturedReceiptFile != null) outState.putString("receipt_file", capturedReceiptFile.getAbsolutePath());
        if (receiptSubmissionId != null) outState.putString("receipt_submission_id", receiptSubmissionId);
        super.onSaveInstanceState(outState);
    }

    @Override public void onDestroyView() {
        stopCamera();
        previewView = null;
        previewFrame = null;
        cameraTitle = null;
        guidance = null;
        payloadField = null;
        submitButton = null;
        captureButton = null;
        receiptTab = null;
        qrTab = null;
        super.onDestroyView();
    }

    @Override public void onDestroy() {
        if (!submitting && capturedReceiptFile != null) capturedReceiptFile.delete();
        if (cameraExecutor != null) cameraExecutor.shutdown();
        super.onDestroy();
    }

    @Override public void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AUTH_REQUEST && resultCode == Activity.RESULT_OK && isAdded()) {
            if (MODE_QR.equals(mode)) submitPayload();
            else if (capturedReceiptFile != null) showReceiptPreview();
        }
    }
}
