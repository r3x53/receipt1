package com.example.receipto.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * Complete receipt data structure
 */
data class ReceiptData(
    val storeName: String? = null,
    val storeAddress: String? = null,
    val storePhone: String? = null,
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val items: List<ReceiptItem>

    = emptyList(),
    val subtotal: Double? = null,
    val tax: Double? = null,
    val total: Double? = null,
    val paymentMethod: String? = null,
    val transactionId: String? = null,
    val cashier: String? = null,
    val rawText: String = ""
) {
    /**
     * Check if receipt has minimum required data
     */
    fun isValid(): Boolean {
        return storeName != null || total != null || items.isNotEmpty()
    }

    /**
     * Get

    confidence score (0.0 to 1.0)
     */
    fun getConfidenceScore(): Float {
        var score = 0f
        var maxScore = 0f

        // Store name (20%)
        maxScore += 0.2f
        if (storeName != null) score += 0.2f

        // Date (15%)
        maxScore += 0.15f
        if (date != null) score += 0.15f

        // Total (30%)
        maxScore += 0.3f
        if (total != null) score += 0.3f

        // Items (25%)
        maxScore += 0.25f
        if (items.isNotEmpty()) score += 0.25f

        // Tax (10%)
        maxScore += 0.1f
        if (tax != null) score += 0.1f

        return if (maxScore > 0) score / maxScore else 0f
    }

}

/**

Individual item on receipt
 */
data class ReceiptItem(
    val name: String,
    val quantity: Double? = null,
    val unit: String? = null,
    val unitPrice: Double? = null,
    val price: Double,
    val taxable: Boolean = true
)

/**

Result of parsing operation
 */
sealed class ParseResult {
    data class Success(val data: ReceiptData) : ParseResult()
    data class PartialSuccess(val data: ReceiptData, val warnings: List<String>) : ParseResult()
    data class Failure(val error: String) : ParseResult()
}
