package com.darkbyte.groupit.tflite;

import android.graphics.Bitmap
import android.graphics.RectF

interface SimilarityClassifier {

    fun register(name: String, recognition: Recognition)
    fun recognizeImage(
        bitmap: Bitmap?,
        getExtra: Boolean,
    ): Recognition?

    fun close()
    fun registeredList(name: String): Recognition?
    fun fetchRegisteredList(): List<Recognition>
}

data class Recognition(
    val id: String?,
    val title: String?,
    val distance: Float?,
    val location: RectF?,
    var extra: Any? = null,
    val bitmap: Bitmap? = null
)