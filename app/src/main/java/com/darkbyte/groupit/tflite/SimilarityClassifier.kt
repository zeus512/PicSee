package com.darkbyte.groupit.tflite;

import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import java.net.URI

interface SimilarityClassifier {

    fun register(name: String, face: UserFace)
    fun recognizeImage(
        bitmap: Bitmap?,
    ): UserFace?

    fun close()
    fun registeredList(name: String): UserFace?
    fun fetchRegisteredList(): List<UserFace>
    fun fetchAllPhotosFromSingleFace(name: String): List<UserFace>?
}

data class Faces(
    val list: List<UserFace> = listOf()
)

data class UserFace(
    val id: String?,
    val title: String?,
    val distance: Float?,
    val location: RectF?,
    var extra: Any? = null,
    val bitmap: Bitmap? = null,
    val originalUri: Uri? = null,
    val facesFoundAlong: Int = Int.MAX_VALUE
)