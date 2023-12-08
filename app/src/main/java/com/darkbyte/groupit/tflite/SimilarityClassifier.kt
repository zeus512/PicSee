package com.darkbyte.groupit.tflite;

import android.graphics.Bitmap
import android.graphics.RectF

interface SimilarityClassifier {

    fun register(name: String, recognition: Recognition)
    fun recognizeImage(bitmap: Bitmap?, getExtra: Boolean): List<Recognition>?
    fun enableStatLogging(debug: Boolean)
    val statString: String?
    fun close()

    fun registeredList(name: String): Recognition?
    fun setUseNNAPI(isChecked: Boolean)

    /** An immutable result returned by a Classifier describing what was recognized.  */
    class Recognition(
        /**
         * A unique identifier for what has been recognized. Specific to the class, not the instance of
         * the object.
         */
        val id: String?,
        /** Display name for the recognition.  */
        val title: String?,
        /**
         * A sortable score for how good the recognition is relative to others. Lower should be better.
         */
        val distance: Float?,
        /** Optional location within the source image for the location of the recognized object.  */
        private var location: RectF?,
        var extra: Any? = null
    ) {

        fun setLocation(location: RectF?) {
            this.location = location
        }

        override fun toString(): String {
            var resultString = ""
            if (id != null) {
                resultString += "[$id] "
            }
            if (title != null) {
                resultString += "$title "
            }
            if (distance != null) {
                resultString += String.format("(%.1f%%) ", distance * 100.0f)
            }
            if (location != null) {
                resultString += location.toString() + " "
            }
            return resultString.trim { it <= ' ' }
        }
    }

}