package com.ekyc.facedetector;

import com.google.mlkit.vision.face.Face;

import java.util.List;

public interface ImageProcessingCallback {
   void onSuccess(List<Face> faces);
   void onFailure( Exception e) ;
}
