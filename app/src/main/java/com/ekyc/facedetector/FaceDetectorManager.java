package com.ekyc.facedetector;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;

public class FaceDetectorManager {
    static FaceDetectorOptions detectorOptions =
            new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                    .build();

    private static FaceDetector detection = FaceDetection.getClient(detectorOptions);

    public static void processImage(InputImage  inputImage, Activity activity, ImageProcessingCallback callback) {
        //InputImage image = InputImage.fromBitmap(bitmap, 0);
        detection.process(inputImage).addOnSuccessListener(activity, new OnSuccessListener<List<Face>>() {
            @Override
            public void onSuccess(List<Face> faces) {
                callback.onSuccess(faces);
            }
        }).addOnFailureListener(activity, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                callback.onFailure(e);
            }
        });

    }
}
