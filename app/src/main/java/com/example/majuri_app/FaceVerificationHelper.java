package com.example.majuri_app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.util.List;
import java.util.Locale;

/**
 * Lightweight on-device face verification helper.
 * Detects one face in each image and compares normalized face signatures.
 */
public class FaceVerificationHelper {

    private static final int SIGNATURE_GRID = 8;
    private static final float MATCH_THRESHOLD = 0.86f;

    public interface Callback {
        void onVerified(boolean matched, float similarity, String message);
        void onFailure(String message);
    }

    public interface SingleFaceCallback {
        void onResult(boolean valid, String message);
    }

    private final Context appContext;
    private final FaceDetector detector;

    public FaceVerificationHelper(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .build();
        detector = FaceDetection.getClient(options);
    }

    public void verifyFaces(String startImagePath, String endImagePath, @NonNull Callback callback) {
        if (startImagePath == null || startImagePath.trim().isEmpty()
                || endImagePath == null || endImagePath.trim().isEmpty()) {
            callback.onFailure("Missing image path for verification.");
            return;
        }

        Bitmap startBitmap = decodeBitmap(startImagePath);
        Bitmap endBitmap = decodeBitmap(endImagePath);
        if (startBitmap == null || endBitmap == null) {
            callback.onFailure("Unable to read selfie image.");
            return;
        }

        InputImage startImage;
        InputImage endImage;
        try {
            startImage = InputImage.fromFilePath(appContext, Uri.fromFile(new File(startImagePath)));
            endImage = InputImage.fromFilePath(appContext, Uri.fromFile(new File(endImagePath)));
        } catch (Exception e) {
            callback.onFailure("Unable to process selfie image.");
            return;
        }

        Tasks.whenAllSuccess(
                        detector.process(startImage),
                        detector.process(endImage)
                )
                .addOnSuccessListener(results -> {
                    try {
                        @SuppressWarnings("unchecked")
                        List<Face> startFaces = (List<Face>) results.get(0);
                        @SuppressWarnings("unchecked")
                        List<Face> endFaces = (List<Face>) results.get(1);

                        if (startFaces == null || startFaces.size() != 1) {
                            callback.onFailure("Start selfie must contain exactly one face.");
                            return;
                        }
                        if (endFaces == null || endFaces.size() != 1) {
                            callback.onFailure("End selfie must contain exactly one face.");
                            return;
                        }

                        Face startFace = startFaces.get(0);
                        Face endFace = endFaces.get(0);

                        float[] startSig = buildFaceSignature(startBitmap, startFace.getBoundingBox());
                        float[] endSig = buildFaceSignature(endBitmap, endFace.getBoundingBox());
                        float similarity = cosineSimilarity(startSig, endSig);
                        boolean matched = similarity >= MATCH_THRESHOLD;

                        String message = matched
                                ? "Face verified successfully."
                                : String.format(Locale.US, "Face mismatch (score %.2f).", similarity);
                        callback.onVerified(matched, similarity, message);
                    } catch (Exception ignored) {
                        callback.onFailure("Face verification failed.");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure("Face verification failed."));
    }

    public void validateSingleFace(String imagePath, @NonNull SingleFaceCallback callback) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            callback.onResult(false, "Selfie image path is missing.");
            return;
        }
        try {
            InputImage image = InputImage.fromFilePath(appContext, Uri.fromFile(new File(imagePath)));
            detector.process(image)
                    .addOnSuccessListener(faces -> {
                        if (faces != null && faces.size() == 1) {
                            callback.onResult(true, "Single face detected.");
                        } else {
                            callback.onResult(false, "Selfie must contain exactly one face.");
                        }
                    })
                    .addOnFailureListener(e -> callback.onResult(false, "Unable to detect face in selfie."));
        } catch (Exception e) {
            callback.onResult(false, "Unable to process selfie image.");
        }
    }

    public void close() {
        detector.close();
    }

    private Bitmap decodeBitmap(String path) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeFile(path, options);
        } catch (Exception ignored) {
            return null;
        }
    }

    private float[] buildFaceSignature(Bitmap source, Rect bounds) {
        Rect safeBounds = clampBounds(bounds, source.getWidth(), source.getHeight());
        Bitmap faceCrop = Bitmap.createBitmap(
                source,
                safeBounds.left,
                safeBounds.top,
                safeBounds.width(),
                safeBounds.height()
        );

        Bitmap resized = Bitmap.createScaledBitmap(faceCrop, 96, 96, true);
        float[] signature = new float[SIGNATURE_GRID * SIGNATURE_GRID];

        int cellW = resized.getWidth() / SIGNATURE_GRID;
        int cellH = resized.getHeight() / SIGNATURE_GRID;
        int idx = 0;
        float mean = 0f;

        for (int gy = 0; gy < SIGNATURE_GRID; gy++) {
            for (int gx = 0; gx < SIGNATURE_GRID; gx++) {
                int startX = gx * cellW;
                int startY = gy * cellH;
                int endX = Math.min(startX + cellW, resized.getWidth());
                int endY = Math.min(startY + cellH, resized.getHeight());

                float sum = 0f;
                int count = 0;
                for (int y = startY; y < endY; y++) {
                    for (int x = startX; x < endX; x++) {
                        int px = resized.getPixel(x, y);
                        float gray = (Color.red(px) * 0.299f) + (Color.green(px) * 0.587f) + (Color.blue(px) * 0.114f);
                        sum += gray;
                        count++;
                    }
                }
                float avg = count == 0 ? 0f : (sum / count);
                signature[idx++] = avg;
                mean += avg;
            }
        }

        mean /= signature.length;
        float norm = 0f;
        for (int i = 0; i < signature.length; i++) {
            signature[i] = signature[i] - mean;
            norm += signature[i] * signature[i];
        }
        norm = (float) Math.sqrt(Math.max(norm, 1e-6f));
        for (int i = 0; i < signature.length; i++) {
            signature[i] /= norm;
        }
        return signature;
    }

    private Rect clampBounds(Rect r, int w, int h) {
        int left = Math.max(0, Math.min(r.left, w - 1));
        int top = Math.max(0, Math.min(r.top, h - 1));
        int right = Math.max(left + 1, Math.min(r.right, w));
        int bottom = Math.max(top + 1, Math.min(r.bottom, h));
        return new Rect(left, top, right, bottom);
    }

    private float cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) return -1f;
        float dot = 0f;
        float normA = 0f;
        float normB = 0f;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA <= 0f || normB <= 0f) return -1f;
        return (float) (dot / (Math.sqrt(normA) * Math.sqrt(normB)));
    }
}
