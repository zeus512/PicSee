package com.darkbyte.groupit.facedetection

import android.content.ContentResolver
import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.media.MediaCodec.MetricsConstants.MODE
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import android.widget.Toast
import com.darkbyte.groupit.logger.Logger
import com.darkbyte.groupit.tflite.SimilarityClassifier
import com.darkbyte.groupit.tflite.TFLiteObjectDetectionAPIModel
import com.darkbyte.groupit.tflite.UserFace
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceDetectorOptions.CLASSIFICATION_MODE_NONE
import com.google.mlkit.vision.face.FaceDetectorOptions.LANDMARK_MODE_NONE
import com.google.mlkit.vision.face.FaceDetectorOptions.PERFORMANCE_MODE_FAST
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException


object Utils {

    private var detector: SimilarityClassifier? = null
    private var lastProcessingTimeMs: Long = 0
    private var bitmap: Bitmap? = null

    private var counter = 0

    private val faceDetectorOptions =
        FaceDetectorOptions.Builder().setPerformanceMode(PERFORMANCE_MODE_FAST)
            .setContourMode(LANDMARK_MODE_NONE)
            .setClassificationMode(CLASSIFICATION_MODE_NONE)
            .build()
    private val faceDetector: FaceDetector by lazy {
        FaceDetection.getClient(faceDetectorOptions)
    }

    fun initiateTFLite(context: Context, assetManager: AssetManager) {
        try {
            detector = TFLiteObjectDetectionAPIModel().create(
                assetManager,
                TF_OD_API_MODEL_FILE,
                TF_OD_API_LABELS_FILE,
                TF_OD_API_INPUT_SIZE,
                TF_OD_API_IS_QUANTIZED
            )
        } catch (e: IOException) {
            e.printStackTrace()
            Logger.e("Exception initializing classifier!")
            val toast = Toast.makeText(
                context, "Classifier could not be initialized", Toast.LENGTH_SHORT
            )
            toast.show()
        }
    }

    fun initiateDetection(
        context: Context,
        uri: Uri,
        onFaceCropped: (Bitmap, Rect) -> Unit
    ) {
        buildBitmapFromUri(uri, context)
        fetchImageFromMediaUri(context, uri, onFaceCropped)
    }

    fun fetchDetector() = detector

    private fun drawRectOnBitmap(bitmap: Bitmap, rect: Rect, drawColor: Int) {
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = drawColor
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }

        // Draw a rectangle on the canvas
        canvas.drawRect(
            rect.left.toFloat(),
            rect.top.toFloat(),
            rect.right.toFloat(),
            rect.bottom.toFloat(),
            paint
        )
    }

    private fun onFacesDetected(
        faces: List<Face>,
        onFaceCropped: (Bitmap, Rect) -> Unit
    ) {
        val paint = Paint()
        paint.setColor(Color.RED)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.0f
        var minimumConfidence: Float =
            MINIMUM_CONFIDENCE_TF_OD_API
        when (MODE) {
            DetectorMode.TF_OD_API -> minimumConfidence =
                MINIMUM_CONFIDENCE_TF_OD_API
        }
        val mutableBitmap = bitmap?.copy(Bitmap.Config.ARGB_8888, true)


        for (face in faces) {
            Logger.i("FACE$face")
            //results = detector.recognizeImage(croppedBitmap);
            val boundingBox = RectF(face.boundingBox)

            //final boolean goodConfidence = result.getConfidence() >= minimumConfidence;
//            val goodConfidence = true //face.get;
            var label = "photo$counter"
            var confidence = -1f
            var color = Color.BLUE
            var extra: Any? = null
            var croppedBitmap: Bitmap? = null
            val startTime = SystemClock.uptimeMillis()
            val bounds = face.boundingBox
            val croppedFaceBitmap = Bitmap.createBitmap(
                mutableBitmap!!,
                bounds.left,
                minOf(bounds.top, 0),
                if (bounds.left + bounds.width() <= bounds.width()) {
                    bounds.width()
                } else {
                    mutableBitmap.width - bounds.left
                },
                if (bounds.top + bounds.height() <= bounds.height()) {
                    bounds.height()
                } else {
                    mutableBitmap.height - minOf(bounds.top, 0)
                }
            )
            val result = detector!!.recognizeImage(croppedFaceBitmap)
            lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime
            if (result != null) {
                extra = result.extra
                croppedBitmap = bitmap
                val conf: Float = result.distance ?: 0f
                confidence = conf
                if (conf < 1.0f) {
                    label = result.title ?: "photo$counter"
                    color = Color.GREEN
                } else {
                    counter++
                    label = "photo$counter"
                }
            }
            drawRectOnBitmap(mutableBitmap, bounds, color)
            onFaceCropped(mutableBitmap, bounds)

            val userFace = UserFace(
                id = "$counter",
                title = label,
                distance = confidence,
                location = boundingBox,
                extra = extra,
                bitmap = croppedBitmap,
                facesFoundAlong = faces.size
            )
            detector?.register(userFace.title ?: "unknown", userFace)
        }


    }


    private fun buildBitmapFromUri(uri: Uri, context: Context) {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        val contentResolver: ContentResolver = context.contentResolver

        // Get the image's input stream using the URI
        val inputStream = contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream, null, options)
        val previewHeight = options.outHeight
        val previewWidth = options.outWidth

        Logger.d("Initializing at size $previewHeight x $previewWidth")
        bitmap = if (Build.VERSION.SDK_INT < 28) {
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        } else {
            val source = ImageDecoder.createSource(contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        }

    }


    private fun fetchImageFromMediaUri(
        context: Context,
        uri: Uri,
        onFaceCropped: (Bitmap, Rect) -> Unit
    ) {
        var image: InputImage? = null
        try {
            image = InputImage.fromFilePath(context, uri)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        image?.let {
            processImage(it) { faces ->
                GlobalScope.launch(Dispatchers.IO) {
                    onFacesDetected(faces, onFaceCropped)
                }
            }
        }
    }

    private fun processImage(inputImage: InputImage, onSuccess: (List<Face>) -> Unit) {
        faceDetector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.size == 0) return@addOnSuccessListener
                onSuccess(faces)
            }
            .addOnFailureListener { e ->
                // Task failed with an exception
                // ...
            }
    }


    // Which detection model to use: by default uses Tensorflow Object Detection API frozen
// checkpoints.
    private enum class DetectorMode {
        TF_OD_API
    }


    // FaceNet
//  private static final int TF_OD_API_INPUT_SIZE = 160;
//  private static final boolean TF_OD_API_IS_QUANTIZED = false;
//  private static final String TF_OD_API_MODEL_FILE = "facenet.tflite";
//  //private static final String TF_OD_API_MODEL_FILE = "facenet_hiroki.tflite";
// MobileFaceNet
    private const val TF_OD_API_INPUT_SIZE = 112
    private const val TF_OD_API_IS_QUANTIZED = false
    private const val TF_OD_API_MODEL_FILE = "mobile_face_net.tflite"
    private const val TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt"
    private val MODE = DetectorMode.TF_OD_API

    // Minimum detection confidence to track a detection.
    private const val MINIMUM_CONFIDENCE_TF_OD_API = 0.5f
    private const val MAINTAIN_ASPECT = false

    //private static final int CROP_SIZE = 320;
//private static final Size CROP_SIZE = new Size(320, 320);
    private const val SAVE_PREVIEW_BITMAP = false
    private const val TEXT_SIZE_DIP = 10f
}


