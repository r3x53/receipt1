package com.example.receipto.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.receipto.util.ImageUtils
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.tasks.await

/**
 * Scans images for barcodes/QR codes using ML Kit
 * Supports all common barcode formats (QR, UPC, EAN, Code128, etc.)
 */
class BarcodeScanner(private val context: Context) {

    private val scanner = BarcodeScanning.getClient()

    /**
     * Scan for barcodes in an image URI
     */
    suspend fun scanImage(imageUri: Uri): BarcodeScanResult {
        return try {
            val bitmap = ImageUtils.loadBitmapFromUri(context, imageUri)
                ?: return BarcodeScanResult.Error("Failed to load image")

            scanImage(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            BarcodeScanResult.Error("Barcode scan failed: ${e.message}")
        }
    }

    /**
     * Scan for barcodes in a Bitmap
     */
    suspend fun scanImage(bitmap: Bitmap): BarcodeScanResult {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val barcodes = scanner.process(inputImage).await()

            if (barcodes.isEmpty()) {
                BarcodeScanResult.NoBarcodes
            } else {
                val detectedBarcodes = barcodes.map { barcode ->
                    DetectedBarcode(
                        rawValue = barcode.rawValue ?: "",
                        displayValue = barcode.displayValue ?: "",
                        format = getBarcodeFormatName(barcode.format),
                        valueType = getBarcodeTypeName(barcode.valueType),
                        boundingBox = barcode.boundingBox
                    )
                }
                BarcodeScanResult.Success(detectedBarcodes)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            BarcodeScanResult.Error("Barcode scan failed: ${e.message}")
        }
    }

    /**
     * Convert barcode format code to readable name
     */
    private fun getBarcodeFormatName(format: Int): String {
        return when (format) {
            Barcode.FORMAT_QR_CODE -> "QR Code"
            Barcode.FORMAT_EAN_13 -> "EAN-13"
            Barcode.FORMAT_EAN_8 -> "EAN-8"
            Barcode.FORMAT_UPC_A -> "UPC-A"
            Barcode.FORMAT_UPC_E -> "UPC-E"
            Barcode.FORMAT_CODE_128 -> "Code 128"
            Barcode.FORMAT_CODE_39 -> "Code 39"
            Barcode.FORMAT_CODE_93 -> "Code 93"
            Barcode.FORMAT_ITF -> "ITF"
            Barcode.FORMAT_CODABAR -> "Codabar"
            Barcode.FORMAT_PDF417 -> "PDF417"
            Barcode.FORMAT_AZTEC -> "Aztec"
            Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
            else -> "Unknown"
        }
    }

    /**
     * Convert barcode value type to readable name
     */
    private fun getBarcodeTypeName(valueType: Int): String {
        return when (valueType) {
            Barcode.TYPE_TEXT -> "Text"
            Barcode.TYPE_URL -> "URL"
            Barcode.TYPE_EMAIL -> "Email"
            Barcode.TYPE_PHONE -> "Phone"
            Barcode.TYPE_SMS -> "SMS"
            Barcode.TYPE_WIFI -> "WiFi"
            Barcode.TYPE_PRODUCT -> "Product"
            Barcode.TYPE_ISBN -> "ISBN"
            else -> "Unknown"
        }
    }

    /**
     * Clean up resources
     */
    fun close() {
        scanner.close()
    }
}

/**
 * Result of barcode scanning
 */
sealed class BarcodeScanResult {
    data class Success(val barcodes: List<DetectedBarcode>) : BarcodeScanResult()
    object NoBarcodes : BarcodeScanResult()
    data class Error(val message: String) : BarcodeScanResult()
}

/**
 * Represents a detected barcode
 */
data class DetectedBarcode(
    val rawValue: String,
    val displayValue: String,
    val format: String,
    val valueType: String,
    val boundingBox: android.graphics.Rect?
)