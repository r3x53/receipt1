package com.example.receipto.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.receipto.util.ImageUtils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/**
 * Processes images to extract text using ML Kit's on-device OCR
 * This runs completely offline - no internet needed!
 */
class OcrProcessor(private val context: Context) {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Extract text from an image URI
     * Returns OcrResult with extracted text and confidence
     */
    suspend fun processImage(imageUri: Uri): OcrResult {
        return try {
            // Load bitmap from URI with orientation correction
            val bitmap = ImageUtils.loadBitmapFromUri(context, imageUri)
                ?: return OcrResult.Error("Failed to load image")

            // Resize for better performance (ML Kit works well with ~1920px images)
            val resizedBitmap = ImageUtils.resizeBitmap(bitmap, maxWidth = 1920, maxHeight = 1920)

            // Process with ML Kit
            processImage(resizedBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            OcrResult.Error("OCR failed: ${e.message}")
        }
    }

    /**
     * Extract text from a Bitmap
     */
    suspend fun processImage(bitmap: Bitmap): OcrResult {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val visionText = textRecognizer.process(inputImage).await()

            if (visionText.text.isEmpty()) {
                OcrResult.Error("No text detected in image")
            } else {
                parseVisionText(visionText)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            OcrResult.Error("OCR failed: ${e.message}")
        }
    }

    /**
     * Parse ML Kit's Text result into our structured format
     */
    private fun parseVisionText(visionText: Text): OcrResult {
        val fullText = visionText.text
        val lines = mutableListOf<TextLine>()
        val words = mutableListOf<String>()

        // Extract text blocks, lines, and elements
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val lineText = line.text
                val confidence = line.confidence ?: 0f

                lines.add(
                    TextLine(
                        text = lineText,
                        confidence = confidence,
                        boundingBox = line.boundingBox
                    )
                )

                // Extract individual words/elements
                for (element in line.elements) {
                    words.add(element.text)
                }
            }
        }

        return OcrResult.Success(
            fullText = fullText,
            lines = lines,
            words = words,
            blockCount = visionText.textBlocks.size
        )
    }

    /**
     * Clean up resources
     */
    fun close() {
        textRecognizer.close()
    }
}

/**
 * Result of OCR processing
 */
sealed class OcrResult {
    data class Success(
        val fullText: String,
        val lines: List<TextLine>,
        val words: List<String>,
        val blockCount: Int
    ) : OcrResult() {

        val lineCount: Int get() = lines.size
        val wordCount: Int get() = words.size
        val avgConfidence: Float get() =
            if (lines.isEmpty()) 0f
            else lines.map { it.confidence }.average().toFloat()
    }

    data class Error(val message: String) : OcrResult()
}

/**
 * Represents a single line of detected text
 */
data class TextLine(
    val text: String,
    val confidence: Float,
    val boundingBox: android.graphics.Rect?
)