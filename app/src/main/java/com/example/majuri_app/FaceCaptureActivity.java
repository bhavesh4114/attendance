package com.example.majuri_app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Front-camera selfie capture with live face overlay and auto-capture.
 */
public class FaceCaptureActivity extends AppCompatActivity {

    public static final String EXTRA_OUTPUT_PATH = "extra_output_path";
    public static final String EXTRA_DUTY_ACTION = "extra_duty_action";
    public static final String EXTRA_CAPTURED_PATH = "extra_captured_path";
    public static final String EXTRA_CAPTURED_SUCCESS = "extra_captured_success";

    private static final int REQUIRED_STABLE_FRAMES = 8;

    private PreviewView previewView;
    private FaceGuideOverlayView overlayView;
    private TextView tvCaptureMode;
    private TextView tvCaptureHint;

    private FaceDetector faceDetector;
    private ExecutorService analysisExecutor;
    private ImageCapture imageCapture;
    private final AtomicBoolean processingFrame = new AtomicBoolean(false);

    private boolean captureTriggered = false;
    private int stableFrames = 0;
    private String outputPath;
    private int dutyAction = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_capture);

        previewView = findViewById(R.id.previewView);
        overlayView = findViewById(R.id.faceGuideOverlay);
        tvCaptureMode = findViewById(R.id.tvCaptureMode);
        tvCaptureHint = findViewById(R.id.tvCaptureHint);

        outputPath = getIntent().getStringExtra(EXTRA_OUTPUT_PATH);
        dutyAction = getIntent().getIntExtra(EXTRA_DUTY_ACTION, 0);
        tvCaptureMode.setText(dutyAction == 2 ? "End Duty Selfie" : "Start Duty Selfie");
        tvCaptureHint.setText("Auto capture will start when face is aligned.");

        findViewById(R.id.btnCloseCapture).setOnClickListener(v -> {
            setCancelledResult();
            finish();
        });

        analysisExecutor = Executors.newSingleThreadExecutor();
        FaceDetectorOptions detectorOptions = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setMinFaceSize(0.12f)
                .build();
        faceDetector = FaceDetection.getClient(detectorOptions);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_SHORT).show();
            setCancelledResult();
            finish();
            return;
        }
        startCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (analysisExecutor != null) {
            analysisExecutor.shutdownNow();
        }
        if (faceDetector != null) {
            faceDetector.close();
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindUseCases(cameraProvider);
            } catch (Exception e) {
                Toast.makeText(this, "Unable to start camera.", Toast.LENGTH_SHORT).show();
                setCancelledResult();
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setTargetRotation(previewView.getDisplay() != null ? previewView.getDisplay().getRotation() : getWindowManager().getDefaultDisplay().getRotation())
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        imageAnalysis.setAnalyzer(analysisExecutor, this::analyzeImage);

        CameraSelector selector = CameraSelector.DEFAULT_FRONT_CAMERA;
        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, selector, preview, imageCapture, imageAnalysis);
    }

    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        if (captureTriggered) {
            imageProxy.close();
            return;
        }
        if (!processingFrame.compareAndSet(false, true)) {
            imageProxy.close();
            return;
        }

        if (imageProxy.getImage() == null) {
            processingFrame.set(false);
            imageProxy.close();
            return;
        }

        InputImage inputImage = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());
        faceDetector.process(inputImage)
                .addOnSuccessListener(faces -> handleFaces(faces, imageProxy))
                .addOnFailureListener(e -> updateOverlay(false, false, 0, "Detecting face..."))
                .addOnCompleteListener(task -> {
                    processingFrame.set(false);
                    imageProxy.close();
                });
    }

    private void handleFaces(@NonNull List<Face> faces, @NonNull ImageProxy imageProxy) {
        if (faces.size() != 1) {
            stableFrames = 0;
            updateOverlay(false, false, stableFrames, "Keep only one face in frame");
            return;
        }

        Face face = faces.get(0);
        Rect box = face.getBoundingBox();
        float frameW = imageProxy.getWidth();
        float frameH = imageProxy.getHeight();

        float cx = box.centerX() / Math.max(1f, frameW);
        float cy = box.centerY() / Math.max(1f, frameH);
        float areaRatio = (box.width() * box.height()) / Math.max(1f, frameW * frameH);
        float yaw = Math.abs(face.getHeadEulerAngleY());
        float pitch = Math.abs(face.getHeadEulerAngleX());

        boolean centered = Math.abs(cx - 0.5f) <= 0.18f && Math.abs(cy - 0.5f) <= 0.22f;
        boolean sizeOk = areaRatio >= 0.10f && areaRatio <= 0.55f;
        boolean angleOk = yaw <= 15f && pitch <= 15f;
        boolean aligned = centered && sizeOk && angleOk;

        if (aligned) {
            stableFrames++;
        } else {
            stableFrames = 0;
        }

        String hint;
        if (!centered) {
            hint = "Move face to center";
        } else if (!sizeOk) {
            hint = areaRatio < 0.10f ? "Move closer to camera" : "Move slightly back";
        } else if (!angleOk) {
            hint = "Keep your face straight";
        } else {
            hint = String.format(Locale.US, "Hold still... %d/%d", Math.min(stableFrames, REQUIRED_STABLE_FRAMES), REQUIRED_STABLE_FRAMES);
        }
        updateOverlay(true, aligned, stableFrames, hint);

        if (stableFrames >= REQUIRED_STABLE_FRAMES && !captureTriggered) {
            captureTriggered = true;
            runOnUiThread(() -> tvCaptureHint.setText("Capturing selfie..."));
            capturePhotoToFile();
        }
    }

    private void updateOverlay(boolean faceDetected, boolean aligned, int stableFrames, String hint) {
        runOnUiThread(() -> overlayView.updateState(faceDetected, aligned, stableFrames, REQUIRED_STABLE_FRAMES, hint));
    }

    private void capturePhotoToFile() {
        if (imageCapture == null) {
            Toast.makeText(this, "Camera is not ready.", Toast.LENGTH_SHORT).show();
            setCancelledResult();
            finish();
            return;
        }

        File outFile = resolveOutputFile();
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(outFile).build();
        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Intent result = new Intent();
                        result.putExtra(EXTRA_CAPTURED_SUCCESS, true);
                        result.putExtra(EXTRA_CAPTURED_PATH, outFile.getAbsolutePath());
                        setResult(RESULT_OK, result);
                        finish();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        captureTriggered = false;
                        stableFrames = 0;
                        tvCaptureHint.setText("Auto capture will start when face is aligned.");
                        Toast.makeText(FaceCaptureActivity.this, "Unable to capture selfie.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private File resolveOutputFile() {
        if (outputPath != null && !outputPath.trim().isEmpty()) {
            File file = new File(outputPath);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                //noinspection ResultOfMethodCallIgnored
                parent.mkdirs();
            }
            return file;
        }
        return new File(getCacheDir(), "duty_selfie_" + System.currentTimeMillis() + ".jpg");
    }

    private void setCancelledResult() {
        Intent result = new Intent();
        result.putExtra(EXTRA_CAPTURED_SUCCESS, false);
        setResult(RESULT_CANCELED, result);
    }
}
