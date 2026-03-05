package com.example.majuri_app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.io.File;
import java.util.List;
import java.util.Locale;

/**
 * Lightweight on-device face verification helper.
 * Detects one face in each image and compares normalized face signatures.
 */
public class FaceVerificationHelper {

    private static final int SIGNATURE_GRID = 10;
    private static final int FACE_SIZE = 120;
    private static final float FACE_EXPAND_RATIO = 0.18f;
    private static final float MATCH_THRESHOLD = 0.78f;
    private static final float CORE_MIN_THRESHOLD = 0.72f;

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
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setMinFaceSize(0.12f)
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
            // Detect on the same bitmap used for signature extraction (after EXIF normalization).
            startImage = InputImage.fromBitmap(startBitmap, 0);
            endImage = InputImage.fromBitmap(endBitmap, 0);
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

                        float[] startFullSig = buildFaceSignature(startBitmap, startFace, 1.0f);
                        float[] endFullSig = buildFaceSignature(endBitmap, endFace, 1.0f);
                        float[] startCoreSig = buildFaceSignature(startBitmap, startFace, 0.72f);
                        float[] endCoreSig = buildFaceSignature(endBitmap, endFace, 0.72f);

                        float fullSimilarity = cosineSimilarity(startFullSig, endFullSig);
                        float coreSimilarity = cosineSimilarity(startCoreSig, endCoreSig);
                        float similarity = (coreSimilarity * 0.58f) + (fullSimilarity * 0.42f);
                        boolean matched = similarity >= MATCH_THRESHOLD && coreSimilarity >= CORE_MIN_THRESHOLD;

                        String message = matched
                                ? "Face verified successfully."
                                : String.format(
                                Locale.US,
                                "Face mismatch (score %.2f, core %.2f).",
                                similarity,
                                coreSimilarity
                        );
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
            Bitmap decoded = BitmapFactory.decodeFile(path, options);
            if (decoded == null) return null;
            return applyExifOrientation(path, decoded);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Bitmap applyExifOrientation(String path, Bitmap source) {
        try {
            ExifInterface exif = new ExifInterface(path);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90f);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180f);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270f);
                    break;
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    matrix.preScale(-1f, 1f);
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    matrix.preScale(1f, -1f);
                    break;
                case ExifInterface.ORIENTATION_TRANSPOSE:
                    matrix.preScale(-1f, 1f);
                    matrix.postRotate(270f);
                    break;
                case ExifInterface.ORIENTATION_TRANSVERSE:
                    matrix.preScale(-1f, 1f);
                    matrix.postRotate(90f);
                    break;
                default:
                    return source;
            }
            return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
        } catch (Exception ignored) {
            return source;
        }
    }

    private float[] buildFaceSignature(Bitmap source, Face face, float centerRatio) {
        Rect expandedBounds = expandAndClamp(face.getBoundingBox(), source.getWidth(), source.getHeight(), FACE_EXPAND_RATIO);
        Bitmap faceCrop = Bitmap.createBitmap(source, expandedBounds.left, expandedBounds.top, expandedBounds.width(), expandedBounds.height());
        Bitmap aligned = alignWithEyeLine(faceCrop, face, expandedBounds);
        Bitmap focused = centerCropRatio(aligned, centerRatio);
        Bitmap resized = Bitmap.createScaledBitmap(focused, FACE_SIZE, FACE_SIZE, true);

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

    private Rect expandAndClamp(Rect r, int w, int h, float expandRatio) {
        int bw = Math.max(1, r.width());
        int bh = Math.max(1, r.height());
        int dw = Math.round(bw * expandRatio);
        int dh = Math.round(bh * expandRatio);
        Rect expanded = new Rect(r.left - dw, r.top - dh, r.right + dw, r.bottom + dh);
        return clampBounds(expanded, w, h);
    }

    private Rect clampBounds(Rect r, int w, int h) {
        int left = Math.max(0, Math.min(r.left, w - 1));
        int top = Math.max(0, Math.min(r.top, h - 1));
        int right = Math.max(left + 1, Math.min(r.right, w));
        int bottom = Math.max(top + 1, Math.min(r.bottom, h));
        return new Rect(left, top, right, bottom);
    }

    private Bitmap centerCropRatio(Bitmap src, float ratio) {
        float safeRatio = Math.max(0.55f, Math.min(1.0f, ratio));
        if (safeRatio >= 0.999f) {
            return src;
        }
        int targetW = Math.max(1, Math.round(src.getWidth() * safeRatio));
        int targetH = Math.max(1, Math.round(src.getHeight() * safeRatio));
        int left = Math.max(0, (src.getWidth() - targetW) / 2);
        int top = Math.max(0, (src.getHeight() - targetH) / 2);
        return Bitmap.createBitmap(src, left, top, targetW, targetH);
    }

    private Bitmap alignWithEyeLine(Bitmap faceCrop, Face face, Rect cropBounds) {
        FaceLandmark leftEyeLandmark = face.getLandmark(FaceLandmark.LEFT_EYE);
        FaceLandmark rightEyeLandmark = face.getLandmark(FaceLandmark.RIGHT_EYE);
        if (leftEyeLandmark == null || rightEyeLandmark == null) {
            return faceCrop;
        }

        PointF leftEye = leftEyeLandmark.getPosition();
        PointF rightEye = rightEyeLandmark.getPosition();
        if (leftEye == null || rightEye == null) {
            return faceCrop;
        }

        float dx = rightEye.x - leftEye.x;
        float dy = rightEye.y - leftEye.y;
        if (Math.abs(dx) < 1e-3f) {
            return faceCrop;
        }

        float angleDeg = (float) Math.toDegrees(Math.atan2(dy, dx));
        if (Math.abs(angleDeg) < 2f) {
            return faceCrop;
        }

        float leftX = leftEye.x - cropBounds.left;
        float leftY = leftEye.y - cropBounds.top;
        float rightX = rightEye.x - cropBounds.left;
        float rightY = rightEye.y - cropBounds.top;
        float centerX = (leftX + rightX) / 2f;
        float centerY = (leftY + rightY) / 2f;

        Matrix matrix = new Matrix();
        matrix.postRotate(-angleDeg, centerX, centerY);
        try {
            return Bitmap.createBitmap(faceCrop, 0, 0, faceCrop.getWidth(), faceCrop.getHeight(), matrix, true);
        } catch (Exception ignored) {
            return faceCrop;
        }
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
