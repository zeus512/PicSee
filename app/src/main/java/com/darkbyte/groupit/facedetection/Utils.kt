package com.darkbyte.groupit.facedetection

import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceDetectorOptions.CLASSIFICATION_MODE_NONE
import com.google.mlkit.vision.face.FaceDetectorOptions.LANDMARK_MODE_NONE
import com.google.mlkit.vision.face.FaceDetectorOptions.PERFORMANCE_MODE_FAST


object Utils {
    private val faceDetectorOptions =
        FaceDetectorOptions.Builder().setPerformanceMode(PERFORMANCE_MODE_FAST)
            .setContourMode(LANDMARK_MODE_NONE)
            .setClassificationMode(CLASSIFICATION_MODE_NONE)
            .build()
    val faceDetector: FaceDetector = FaceDetection.getClient(faceDetectorOptions)
}