package com.darkbyte.groupit.tflite;

import android.graphics.Bitmap
import android.graphics.RectF

interface SimilarityClassifier {

    fun register(name: String, recognition: Recognition)
    fun recognizeImage(bitmap: Bitmap?, getExtra: Boolean): List<Recognition>?
    fun close()
    fun registeredList(name: String): Recognition?
}

data class Recognition(
    val id: String?,
    val title: String?,
    val distance: Float?,
    val location: RectF?,
    var extra: Any? = null
)