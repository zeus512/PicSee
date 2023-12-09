package com.darkbyte.groupit.tflite

import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Trace
import android.util.Pair
import com.darkbyte.groupit.Logger
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.Vector
import kotlin.math.sqrt

//private static final int OUTPUT_SIZE = 512;
private const val OUTPUT_SIZE = 192

// Only return this many results.
private const val NUM_DETECTIONS = 1

// Float model
private const val IMAGE_MEAN = 128.0f
private const val IMAGE_STD = 128.0f

// Number of threads in the java app
private const val NUM_THREADS = 4

class TFLiteObjectDetectionAPIModel : SimilarityClassifier {

    var embeddings: Array<FloatArray> = arrayOf(floatArrayOf())
    private var isModelQuantized = false

    // Config values.
    private var inputSize = 0

    // Pre-allocated buffers.
    private val labels = Vector<String>()
    private lateinit var intValues: IntArray

    // outputLocations: array of shape [Batchsize, NUM_DETECTIONS,4]
    // contains the location of detected boxes
    private var outputLocations: Array<Array<FloatArray>> = arrayOf(arrayOf(floatArrayOf()))

    // outputClasses: array of shape [Batchsize, NUM_DETECTIONS]
    // contains the classes of detected boxes
    private var outputClasses: Array<FloatArray> = arrayOf(floatArrayOf())

    // outputScores: array of shape [Batchsize, NUM_DETECTIONS]
    // contains the scores of detected boxes
    private var outputScores: Array<FloatArray> = arrayOf(floatArrayOf())

    // numDetections: array of shape [Batchsize]
    // contains the number of detected boxes
    private var numDetections: FloatArray = floatArrayOf()
    private var embeedings: Array<FloatArray> = arrayOf(floatArrayOf())
    private lateinit var imgData: ByteBuffer
    private var tfLite: Interpreter? = null

    // Face Mask Detector Output
    private val output: MutableList<MutableList<Float>> = mutableListOf()

    private var registered: HashMap<String, SimilarityClassifier.Recognition> = HashMap()
    override fun register(name: String, recognition: SimilarityClassifier.Recognition) {
        registered.getOrPut(name) { recognition }
    }

    override fun recognizeImage(
        bitmap: Bitmap?,
        getExtra: Boolean
    ): List<SimilarityClassifier.Recognition>? {
        // Log this method so that it can be analyzed with systrace.
        Trace.beginSection("recognizeImage")
        Trace.beginSection("preprocessBitmap")
        // Preprocess the image data from 0-255 int to normalized float based
        // on the provided parameters.
        if (bitmap == null) return null
        val pixels = IntArray(bitmap.width * bitmap.height)
        imgData.rewind()
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)


        for (i in 0..<inputSize) {
            for (j in 0..<inputSize) {
                val pixelValue = pixels[i * inputSize + j]
                Logger().d("dummy $pixelValue")
                if (isModelQuantized) {
                    // Quantized model
                    imgData.put((pixelValue shr 16 and 0xFF).toByte())
                    imgData.put((pixelValue shr 8 and 0xFF).toByte())
                    imgData.put((pixelValue and 0xFF).toByte())
                } else { // Float model
                    imgData.putFloat(((pixelValue shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    imgData.putFloat(((pixelValue shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    imgData.putFloat(((pixelValue and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                }
            }
        }
        Trace.endSection() // preprocessBitmap

        // Copy the input data into TensorFlow.
        Trace.beginSection("feed")
        val inputArray = arrayOf<Any?>(imgData)
        Trace.endSection()

// Here outputMap is changed to fit the Face Mask detector
        val outputMap: MutableMap<Int, Any> = HashMap()
        embeddings = Array(1) { FloatArray(OUTPUT_SIZE) }
        outputMap[0] = embeddings


        // Run the inference call.
        Trace.beginSection("run")
        //tfLite.runForMultipleInputsOutputs(inputArray, outputMapBack);
        tfLite?.runForMultipleInputsOutputs(inputArray, outputMap)
        Trace.endSection()

//    String res = "[";
//    for (int i = 0; i < embeedings[0].length; i++) {
//      res += embeedings[0][i];
//      if (i < embeedings[0].length - 1) res += ", ";
//    }
//    res += "]";
        var distance = Float.MAX_VALUE
        val id = "0"
        var label: String? = "?"
        if (registered.size > 0) {
            //LOGGER.i("dataset SIZE: " + registered.size());
            val nearest = findNearest(embeddings[0])
            if (nearest != null) {
                val name = nearest.first
                label = name
                distance = nearest.second
                Logger().i("nearest: $name - distance: $distance")
            }
        }
        val recognitions: MutableList<SimilarityClassifier.Recognition> = mutableListOf()
        val rec = SimilarityClassifier.Recognition(
            id,
            label,
            distance,
            RectF()
        )
        recognitions.add(rec)
        if (getExtra) {
            rec.extra = embeddings
        }
        Trace.endSection()
        return recognitions
    }


    override fun enableStatLogging(debug: Boolean) {
        TODO("Not yet implemented")
    }

    override val statString: String?
        get() = TODO("Not yet implemented")

    override fun close() {
        TODO("Not yet implemented")
    }

    override fun setUseNNAPI(isChecked: Boolean) {
        TODO("Not yet implemented")
    }

    override fun registeredList(name: String) = registered[name]
// looks for the nearest embeeding in the dataset (using L2 norm)
    // and retrurns the pair <id, distance>

    private fun findNearest(emb: FloatArray): Pair<String, Float>? {
        var ret: Pair<String, Float>? = null
        for ((name, value) in registered) {
            val knownEmb = (value.extra as? Array<FloatArray>)?.getOrNull(0) ?: floatArrayOf()
            var distance = 0f
            for (i in emb.indices) {
                if (i !in knownEmb.indices) continue
                val diff = emb[i] - knownEmb[i]
                distance += diff * diff
            }
            distance = sqrt(distance.toDouble()).toFloat()
            if (ret == null || distance < ret.second) {
                ret = Pair(name, distance)
            }
        }
        return ret
    }


    /** Memory-map the model file in Assets.  */
    @Throws(IOException::class)
    private fun loadModelFile(assets: AssetManager, modelFilename: String): MappedByteBuffer {
        val fileDescriptor: AssetFileDescriptor = assets.openFd(modelFilename)
        val inputStream: FileInputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset: Long = fileDescriptor.startOffset
        val declaredLength: Long = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Initializes a native TensorFlow session for classifying images.
     *
     * @param assetManager The asset manager to be used to load assets.
     * @param modelFilename The filepath of the model GraphDef protocol buffer.
     * @param labelFilename The filepath of label file for classes.
     * @param inputSize The size of image input
     * @param isQuantized Boolean representing model is quantized or not
     */
    @Throws(IOException::class)
    fun create(
        assetManager: AssetManager,
        modelFilename: String,
        labelFilename: String,
        inputSize: Int,
        isQuantized: Boolean
    ): SimilarityClassifier {
        val d = TFLiteObjectDetectionAPIModel()
        val actualFilename = labelFilename.split("file:///android_asset/".toRegex())
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()[1]
        val labelsInput: InputStream = assetManager.open(actualFilename)
        val br = BufferedReader(InputStreamReader(labelsInput))
        var line: String?
        while (br.readLine().also { line = it } != null) {
            Logger().w(line)
            d.labels.add(line)
        }
        br.close()
        d.inputSize = inputSize
        try {
            d.tfLite = Interpreter(
                loadModelFile(assetManager, modelFilename), Interpreter.Options()
                    .setNumThreads(NUM_THREADS)
            )

        } catch (e: Exception) {
            throw RuntimeException(e)
        }
        d.isModelQuantized = isQuantized
        // Pre-allocate buffers.
        val numBytesPerChannel: Int = if (isQuantized) {
            1 // Quantized
        } else {
            4 // Floating point
        }
        d.imgData =
            ByteBuffer.allocateDirect(1 * d.inputSize * d.inputSize * 3 * numBytesPerChannel)
        d.imgData.order(ByteOrder.nativeOrder())
        d.intValues = IntArray(d.inputSize * d.inputSize)

        d.outputLocations =
            Array<Array<FloatArray>>(1) { Array<FloatArray>(NUM_DETECTIONS) { FloatArray(4) } }
        d.outputClasses = Array<FloatArray>(1) { FloatArray(NUM_DETECTIONS) }
        d.outputScores = Array<FloatArray>(1) { FloatArray(NUM_DETECTIONS) }
        d.numDetections = FloatArray(1)
        return d
    }
}
