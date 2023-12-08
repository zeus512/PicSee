package com.darkbyte.groupit.facedetection

import android.content.ContentResolver
import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import android.widget.Toast
import com.darkbyte.groupit.Logger
import com.darkbyte.groupit.tflite.SimilarityClassifier
import com.darkbyte.groupit.tflite.SimilarityClassifier.Recognition
import com.darkbyte.groupit.tflite.TFLiteObjectDetectionAPIModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceDetectorOptions.CLASSIFICATION_MODE_NONE
import com.google.mlkit.vision.face.FaceDetectorOptions.LANDMARK_MODE_NONE
import com.google.mlkit.vision.face.FaceDetectorOptions.PERFORMANCE_MODE_FAST
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.LinkedList


object Utils {

    private var sensorOrientation: Int = 0
    private var detector: SimilarityClassifier? = null
    private var lastProcessingTimeMs: Long = 0
    private var rgbFrameBitmap: Bitmap? = null
    private var bitmap: Bitmap? = null
    private var croppedBitmap: Bitmap? = null
    private var cropCopyBitmap: Bitmap? = null
    private var computingDetection = false

    var counter = 0

    //private boolean adding = false;
    private var timestamp: Long = 0
    private var frameToCropTransform: Matrix? = null
    private var cropToFrameTransform: Matrix? = null

    //private Matrix cropToPortraitTransform;
//    private var tracker: MultiBoxTracker? = null
//    private var borderedText: BorderedText? = null


    // here the preview image is drawn in portrait way
    private var portraitBmp: Bitmap? = null

    // here the face is cropped and drawn
    private var faceBmp: Bitmap? = null

    //private HashMap<String, Classifier.Recognition> knownFaces = new HashMap<>();

    private val faceDetectorOptions =
        FaceDetectorOptions.Builder().setPerformanceMode(PERFORMANCE_MODE_FAST)
            .setContourMode(LANDMARK_MODE_NONE)
            .setClassificationMode(CLASSIFICATION_MODE_NONE)
            .build()
    private val faceDetector: FaceDetector by lazy {
        FaceDetection.getClient(faceDetectorOptions)
    }

    suspend fun initiateTFLite(context: Context, assetManager: AssetManager) {
        try {
            detector = TFLiteObjectDetectionAPIModel().create(
                assetManager,
                TF_OD_API_MODEL_FILE,
                TF_OD_API_LABELS_FILE,
                TF_OD_API_INPUT_SIZE,
                TF_OD_API_IS_QUANTIZED
            )
            faceBmp = Bitmap.createBitmap(
                TF_OD_API_INPUT_SIZE,
                TF_OD_API_INPUT_SIZE,
                Bitmap.Config.ARGB_8888
            )


            //cropSize = TF_OD_API_INPUT_SIZE;
        } catch (e: IOException) {
            e.printStackTrace()
            Logger().e(
                e,
                "Exception initializing classifier!"
            )
            val toast = Toast.makeText(
                context, "Classifier could not be initialized", Toast.LENGTH_SHORT
            )
            toast.show()
        }
    }

    suspend fun initiateDetection(context: Context, uri: Uri) {
        getIMGSize(uri, context)

        fetchImageFromMediaUri(context, uri)
    }

    private fun onFacesDetected(faces: List<Face>, add: Boolean) {
        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap!!)
        val canvas = Canvas(cropCopyBitmap!!)
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
        val mappedRecognitions: MutableList<Recognition> = LinkedList()


        //final List<Classifier.Recognition> results = new ArrayList<>();

        // Note this can be done only once
        val sourceW = rgbFrameBitmap!!.getWidth()
        val sourceH = rgbFrameBitmap!!.getHeight()
        val targetW = portraitBmp!!.getWidth()
        val targetH = portraitBmp!!.getHeight()
        val transform = createTransform(
            sourceW,
            sourceH,
            targetW,
            targetH,
            sensorOrientation
        )
        val cv = Canvas(portraitBmp!!)

        // draws the original image in portrait mode.
        cv.drawBitmap(rgbFrameBitmap!!, transform, null)
        val cvFace = Canvas(faceBmp!!)
        val saved = false
        for (face in faces) {
            Logger().i("FACE$face")
            //results = detector.recognizeImage(croppedBitmap);
            val boundingBox = RectF(face.boundingBox)

            //final boolean goodConfidence = result.getConfidence() >= minimumConfidence;
            val goodConfidence = true //face.get;
            if (boundingBox != null && goodConfidence) {

                // maps crop coordinates to original
                //cropToFrameTransform!!.mapRect(boundingBox)

                // maps original coordinates to portrait coordinates
                val faceBB = RectF(boundingBox)
                transform.mapRect(faceBB)

                // translates portrait to origin and scales to fit input inference size
                //cv.drawRect(faceBB, paint);
                val sx: Float =
                    TF_OD_API_INPUT_SIZE.toFloat() / faceBB.width()
                val sy: Float =
                    TF_OD_API_INPUT_SIZE.toFloat() / faceBB.height()
                val matrix = Matrix()
                matrix.postTranslate(-faceBB.left, -faceBB.top)
                matrix.postScale(sx, sy)
                cvFace.drawBitmap(portraitBmp!!, matrix, null)

                //canvas.drawRect(faceBB, paint);
                var label = ""
                var confidence = -1f
                var color = Color.BLUE
                var extra: Any? = null
                var crop: Bitmap? = null
                if (add) {
                    crop = Bitmap.createBitmap(
                        portraitBmp!!,
                        faceBB.left.toInt(),
                        faceBB.top.toInt(),
                        faceBB.width().toInt(),
                        faceBB.height().toInt()
                    )
                }
                val startTime = SystemClock.uptimeMillis()
                val bounds = face.boundingBox
                val mutableBitmap = bitmap?.copy(Bitmap.Config.ARGB_8888, true)

                val croppedFaceBitmap = Bitmap.createBitmap(
                    mutableBitmap!!,
                    bounds.left,
                    bounds.top,
                    bounds.width(),
                    bounds.height()
                )


                val resultsAux = detector!!.recognizeImage(croppedFaceBitmap, add)
                lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime
                if (resultsAux!!.isNotEmpty()) {
                    val result = resultsAux!![0]
                    extra = result.extra
                    //          Object extra = result.getExtra();
//          if (extra != null) {
//            Logger().i("embeeding retrieved " + extra.toString());
//          }
                    val conf: Float = result.distance ?: 0f
                    if (conf < 1.0f) {
                        if (detector?.registeredList(result.title.orEmpty()) != null) {
                            Logger().d("i'm working name - ${result.title}")
                        } else {
                            counter + 1
                        }
                        confidence = conf
                        label = result.title ?: "dummy"
                        color = if (result.id.equals("0")) {
                            Color.GREEN
                        } else {
                            Color.RED
                        }
                    }
                }

                val result = Recognition(
                    "0", label, confidence, boundingBox
                )
                //result.setColor(color)
                result.setLocation(boundingBox)
                result.extra = extra
                //result.setCrop(crop)
                mappedRecognitions.add(result)
            }
        }

        //    if (saved) {
//      lastSaved = System.currentTimeMillis();
//    }
        updateResults(mappedRecognitions)
    }


    private suspend fun getIMGSize(uri: Uri, context: Context) {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        val contentResolver: ContentResolver = context.contentResolver

        // Get the image's input stream using the URI
        val inputStream = contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream, null, options)
        val previewHeight = options.outHeight
        val previewWidth = options.outWidth

        Logger().i(
            "Initializing at size %dx%d",
            previewWidth,
            previewHeight
        )
        val rgbBytes = IntArray(previewWidth * previewHeight)
        rgbFrameBitmap =
            Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888)
        bitmap = if (Build.VERSION.SDK_INT < 28) {
            MediaStore.Images.Media.getBitmap(contentResolver, uri);
        } else {
            val source = ImageDecoder.createSource(contentResolver, uri);
            ImageDecoder.decodeBitmap(source);
        }
        val cropW = (previewWidth / 2.0).toInt()
        val cropH = (previewHeight / 2.0).toInt()

        frameToCropTransform =
            ImageUtils.getTransformationMatrix(
                previewWidth,
                previewHeight,
                cropW,
                cropH,
                sensorOrientation,
                MAINTAIN_ASPECT
            )
        croppedBitmap = Bitmap.createBitmap(cropW, cropH, Bitmap.Config.ARGB_8888)

        portraitBmp = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888)
        rgbFrameBitmap = bitmap
        val canvas = Canvas(croppedBitmap!!)
        canvas.drawBitmap(rgbFrameBitmap!!, frameToCropTransform!!, null)

    }


    private suspend fun fetchImageFromMediaUri(
        context: Context,
        uri: Uri
    ) {

        var image: InputImage? = null
        try {
            image = InputImage.fromFilePath(context, uri)

            //image = InputImage.fromBitmap(croppedBitmap!!, 0)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        image?.let {
            processImage(it) { faces ->
                GlobalScope.launch(Dispatchers.IO) {
                    onFacesDetected(faces, true)
                }
            }
        }
    }

    fun fetchFaceDetector() = faceDetector

    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun processImage(inputImage: InputImage, onSuccess: (List<Face>) -> Unit) {
        val result = faceDetector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.size == 0) {
                }
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

    suspend fun setUseNNAPI(isChecked: Boolean) {
        detector?.setUseNNAPI(isChecked)
    }

    // Face Processing
    private fun createTransform(
        srcWidth: Int,
        srcHeight: Int,
        dstWidth: Int,
        dstHeight: Int,
        applyRotation: Int
    ): Matrix {
        val matrix = Matrix()
        if (applyRotation != 0) {
            if (applyRotation % 90 != 0) {
                Logger().w("Rotation of %d % 90 != 0", applyRotation)
            }

            // Translate so center of image is at origin.
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f)

            // Rotate around origin.
            matrix.postRotate(applyRotation.toFloat())
        }

//        // Account for the already applied rotation, if any, and then determine how
//        // much scaling is needed for each axis.
//        final boolean transpose = (Math.abs(applyRotation) + 90) % 180 == 0;
//        final int inWidth = transpose ? srcHeight : srcWidth;
//        final int inHeight = transpose ? srcWidth : srcHeight;
        if (applyRotation != 0) {

            // Translate back from origin centered reference to destination frame.
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f)
        }
        return matrix
    }

    private fun showAddFaceDialog(rec: Recognition) {

//
//            val name: String = etName.getText().toString()
//            if (name.isEmpty()) {
//                return
//            }
        detector?.register(rec.title ?: "unknown", rec)
        //knownFaces.put(name, rec);
    }

    private fun updateResults(
        mappedRecognitions: List<Recognition>
    ) {
        //tracker.trackResults(mappedRecognitions, currTimestamp)
        //adding = false;
        if (mappedRecognitions.isNotEmpty()) {
            Logger().i("Adding results")
            val rec: Recognition = mappedRecognitions[0]
            if (rec.extra != null) {
                showAddFaceDialog(rec)
            }
        }

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

private enum class DetectorMode {
    TF_OD_API
}


