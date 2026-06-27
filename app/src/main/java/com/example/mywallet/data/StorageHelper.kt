package com.example.mywallet.data

import android.content.Context
import android.net.Uri

fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val fileName = "profile_photo.jpg"
        val file = java.io.File(context.filesDir, fileName)
        val outputStream = java.io.FileOutputStream(file)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        file.absolutePath
    } catch (e: Exception) {
        null
    }
}