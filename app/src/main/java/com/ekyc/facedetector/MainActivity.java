package com.ekyc.facedetector;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private CameraManager cameraManager;
    public static final int CAMERA_CODE = 110;
    private PreviewView previewView;
    private boolean isBack = true;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private boolean isFlashOn = false;
    private ImageAnalysis imageAnalysis;
    private ExecutorService cameraExecutor;
    private Camera camera;
    private ImageButton btnFlash;
    private ImageView imgFaceDetector;
    private CardView cardView;
    private TextView txtFaceDetectHeading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera(CameraSelector.DEFAULT_BACK_CAMERA);
        } else {
            requestCameraPermissions();
        }
        viewInitialize();
        onClickListener();
        sharedPreferences = getSharedPreferences("FACE", MODE_PRIVATE);
        editor = sharedPreferences.edit();
        //Initialize camera executor
/*      1. Camera Frame Processing is Sequential
        Camera frames are generated sequentially, and you process one frame at a time.
        Using a single-threaded executor ensures that only one frame is processed at any given moment, avoiding race conditions or out-of-order processing.
        For example, if frame 2 starts processing before frame 1 is finished, it could lead to incorrect results or crashes.*/
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void onClickListener() {
        findViewById(R.id.btnCameraSwitch).setOnClickListener(view -> {
            if (isBack) {
                startCamera(CameraSelector.DEFAULT_FRONT_CAMERA);
                // btnFlash.setVisibility(View.GONE);

            } else {
                startCamera(CameraSelector.DEFAULT_BACK_CAMERA);
                // btnFlash.setVisibility(View.VISIBLE);

            }
            isBack = !isBack;
        });
        //flash On or Off
        btnFlash.setOnClickListener(view -> {
            if (isFlashOn) {
                toggleFlashlight(false);

            } else {
                toggleFlashlight(true);
            }

        });
    }

    private void requestCameraPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.CAMERA}, CAMERA_CODE);
    }

    private void startCamera(CameraSelector cameraSelector) {
         /*ProcessCameraProvider. This is used to bind the lifecycle of cameras to the lifecycle owner. This eliminates the task of opening
        and closing the camera since CameraX is lifecycle-aware.*/
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                // Unbind all use cases before rebinding
                cameraProvider.unbindAll();
                // Preview
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                // Create an image analysis use case
                imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);
                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void analyzeImage(ImageProxy imageProxy) {
/*        ImageProxy is a class provided by Android's CameraX library, representing an image frame from the camera.
        It serves as a bridge between the camera and image processing libraries (such as ML Kit) and allows you to access and process image data in real-time.*/
        @SuppressWarnings("UnsafeOptInUsageError")
        InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());
        FaceDetectorManager.processImage(image, this, new ImageProcessingCallback() {
            @Override
            public void onSuccess(List<Face> faces) {

                if (faces != null && !faces.isEmpty()) {
                    if (faces.size() >= 1) {
                        imgFaceDetector.setBackgroundResource(R.drawable.circular_farme_green);
                        txtFaceDetectHeading.setText(R.string.face_detected);
                    } else {
                        imgFaceDetector.setBackgroundResource(R.drawable.circular_farme_red);
                        txtFaceDetectHeading.setText(R.string.no_face_detect_heading);
                    }
                } else {
                    imgFaceDetector.setBackgroundResource(R.drawable.circular_farme_red);
                    txtFaceDetectHeading.setText(R.string.no_face_detect_heading);
                }
                imageProxy.close();

            }

            @Override
            public void onFailure(Exception e) {
                Log.d("VALUEE", "ERROR" + e.getMessage());

            }
        });
    }

    private void toggleFlashlight(boolean enable) {
        if (camera != null) {
            camera.getCameraControl().enableTorch(enable);  // Enable or disable the torch
        } else {
            Toast.makeText(this, "Camera not ready yet", Toast.LENGTH_SHORT).show();
        }
        if (enable) {
            btnFlash.setBackgroundResource(R.drawable.flash_off_24px);
        } else {
            btnFlash.setBackgroundResource(R.drawable.flash_on_24px);

        }
        isFlashOn = !isFlashOn;
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera(CameraSelector.DEFAULT_BACK_CAMERA);
            } else {
                Toast.makeText(this, "Camera permission denied. Enable it in settings to use this feature.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    private void viewInitialize() {
        previewView = findViewById(R.id.preview);
        cardView = findViewById(R.id.cardView);
        imgFaceDetector = findViewById(R.id.imgFaceDetector);
        txtFaceDetectHeading = findViewById(R.id.txtFaceDetect);
        btnFlash = findViewById(R.id.btnFlash);

    }
}
