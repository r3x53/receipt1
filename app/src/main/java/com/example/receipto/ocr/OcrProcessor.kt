package com.example.receipto.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.example.receipto.util.ImageUtils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import kotlin.text.lines

/**
 * Processes images to extract text using ML Kit's on-device OCR
 * Now with OpenCV preprocessing and layout detection!
 * This runs completely offline - no internet needed!
 */
class OcrProcessor(private val context: Context) {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Extract text from an image URI
     * Returns OcrResult with extracted text, confidence, and detected regions
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
     * Extract text from a Bitmap with OpenCV preprocessing
     */
    suspend fun processImage(bitmap: Bitmap): OcrResult {
        return try {
            Log.d("OcrProcessor", "Starting OCR processing with OpenCV pipeline")

            // Process ORIGINAL image (without OpenCV)
            val originalInputImage = InputImage.fromBitmap(bitmap

                , 0)
            val originalVisionText = textRecognizer.process(originalInputImage).await()
            Log.d("OcrProcessor", "ORIGINAL OCR detected ${originalVisionText.text.length} characters")

            // Process with OpenCV preprocessing
            val preprocessedBitmap = try {
                ImagePreprocessor.preprocess(bitmap)
            } catch (e: Exception) {
                Log.e("OcrProcessor", "OpenCV preprocessing failed", e)
                bitmap
            }

            val preprocessedInputImage = InputImage.fromBitmap(preprocessedBitmap, 0)
            val preprocessedVisionText = textRecognizer.process(preprocessedInputImage).await()
            Log.d("OcrProcessor", "PREPROCESSED OCR detected ${preprocessedVisionText.text.length} characters")

            // Detect layout regions
            val detectedRegions = try {
                LayoutDetector.detectRegions(preprocessedBitmap)
            } catch (e: Exception) {
                Log.e("OcrProcessor", "Layout detection failed", e)
                emptyList()
            }

            val classifiedRegions = try {
                RegionClassifier.classify(detectedRegions)
            } catch (e: Exception) {
                Log.e("Ocr Processor", "Region classification failed", e)
                        emptyList()
            }

            Log.d("OcrProcessor", "Detected ${classifiedRegions.size} regions")

            // USE THE BETTER RESULT (more characters detected)
            val visionText = if (originalVisionText.text.length > preprocessedVisionText.text.length) {
                Log.d("OcrProcessor", "Using ORIGINAL (better)")
                originalVisionText
            } else {
                Log.d("OcrProcessor", "Using PREPROCESSED (better)")
                preprocessedVisionText
            }

            if (visionText.text.isEmpty()) {
                OcrResult.Error("No text detected in image")
            } else {
                parseVisionTextWithRegions(visionText, classifiedRegions)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            OcrResult.Error("OCR failed: ${e.message}")
        }
    }

    /**
     * Parse ML Kit's Text result into our structured format with region information
     */
    private fun parseVisionTextWithRegions(
        visionText: Text,
        regions: List<ClassifiedRegion>
    ): OcrResult {
        val fullText = visionText.text
        val lines = mutableListOf<TextLine>()
        val words = mutableListOf<String>()

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

                for (element in line.elements) {
                    words.add(element.text)
                }
            }
        }

        return OcrResult.Success(
            fullText = fullText,
            lines = lines,
            words = words,
            blockCount = visionText.textBlocks.size,
            detectedRegions = regions
        )
    }


    /**
     * Parse ML Kit's Text result into our structured format (legacy method without regions)
     */
    private fun parseVisionText(visionText: Text): OcrResult {
        return parseVisionTextWithRegions(visionText, emptyList())
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
        val blockCount: Int,
        val detectedRegions: List<ClassifiedRegion> = emptyList()  // NEW: Layout regions
    ) : OcrResult() {

        val lineCount: Int get() = lines.size
        val wordCount: Int get() = words.size
        val avgConfidence: Float get() =
            if (lines.isEmpty()) 0f
            else lines.map { it.confidence }.average().toFloat()

        // NEW: Helper methods for region-based parsing
        fun getTextInRegion(regionType: RegionType): String {
            return detectedRegions
                .filter { it.type == regionType }
                .joinToString("\n") { it.text }
        }

        fun hasRegions(): Boolean = detectedRegions.isNotEmpty()
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