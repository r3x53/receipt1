package com.example.receipto.util

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

object ImagePicker {

    /**
     * Composable function to launch gallery picker
     */
    @Composable
    fun rememberGalleryLauncher(
        onImageSelected: (Uri?) -> Unit
    ) = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onImageSelected(uri)
    }

    /**
     * Copy image from Uri to app's cache directory
     * This ensures we have a persistent copy
     */
    fun copyImageToCache(context: Context, uri: Uri): Uri? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = java.io.File(
                context.cacheDir,
                "selected_${System.currentTimeMillis()}.jpg"
            )
            inputStream?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}