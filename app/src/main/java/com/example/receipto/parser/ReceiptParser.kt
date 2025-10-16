package com.example.receipto.parser

import com.example.receipto.model.ParseResult
import com.example.receipto.model.ReceiptData
import com.example.receipto.model.ReceiptItem
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Main receipt parsing engine
 * Takes messy OCR text and extracts structured data
 */
class ReceiptParser {

    private val rules = ParsingRules

    /**
     * Parse raw OCR text into structured receipt data
     */
    fun parse(rawText: String): ParseResult {
        if (rawText.isBlank()) {
            return ParseResult.Failure("Empty text provided")
        }

        val cleanText = TextCleaner.clean(rawText)
        val lines = TextCleaner.getCleanLines(cleanText)

        if (lines.isEmpty()) {
            return ParseResult.Failure("No readable lines found")
        }

        val warnings = mutableListOf<String>()

        // Extract all components
        val storeName = extractStoreName(lines)
        val storeAddress = extractAddress(lines)
        val storePhone = extractPhone(cleanText)
        val date = extractDate(cleanText)
        val time = extractTime(cleanText)
        val items = extractItems(lines)
        val total = extractTotal(lines, cleanText)
        val subtotal = extractSubtotal(lines, cleanText)
        val tax = extractTax(lines, cleanText)
        val paymentMethod = extractPaymentMethod(cleanText)
        val transactionId = extractTransactionId(cleanText)
        val cashier = extractCashier(cleanText)

        // Build receipt data
        val receiptData = ReceiptData(
            storeName = storeName,
            storeAddress = storeAddress,
            storePhone = storePhone,
            date = date,
            time = time,
            items = items,
            subtotal = subtotal,
            tax = tax,
            total = total

            ,
            paymentMethod = paymentMethod,
            transactionId = transactionId,
            cashier = cashier,
            rawText = rawText
        )

        // Generate warnings
        if (storeName == null) warnings.add("Could not detect store name")
        if (date == null) warnings.add("Could not parse date")
        if (total == null) warnings.add("Could not find total amount")
        if (items.isEmpty()) warnings.add("No items detected")

        return when {
            !receiptData.isValid() -> ParseResult.Failure("Insufficient data extracted")
            warnings.isNotEmpty() -> ParseResult.PartialSuccess(receiptData, warnings)
            else -> ParseResult.Success(receiptData)
        }
    }

    /**
     * Extract store name (usually first few lines)
     */
    private fun extractStoreName(lines: List<String>): String? {
        // Look in first 5 lines
        val candidates = lines.take(5)

        for (line in candidates) {
            // Skip lines that look like addresses or common header text
            if (rules.addressIndicators.any { line.contains(it, ignoreCase = true) }) continue
            if (rules.excludeFromStoreName.any { line.contains(it, ignoreCase = true) }) continue
            if (line.length < 3 || line.length > 50) continue

            // Good candidate: mostly letters, not too many numbers
            val letterRatio = line.count { it.isLetter() }.toFloat() / line.length
            if (letterRatio > 0.5) {
                return line.trim()
            }
        }

        return null
    }

    /**
     * Extract address
     */
    private fun extractAddress(lines: List<String>): String? {
        val addressLines = lines.filter { line ->
            rules.addressIndicators.any { line.contains(it, ignoreCase = true) } ||
                    line.matches(Regex(""".*\d+.*""")) // Contains numbers (street numbers)
        }.take(2)

        return if (addressLines.isNotEmpty()) {
            addressLines.joinToString(", ")
        } else null
    }

    /**
     * Extract phone number
     */
    private fun extractPhone(text: String): String? {
        return rules.phonePatterns.firstNotNullOfOrNull { pattern ->
            pattern.find(text)?.value
        }
    }

    /**
     * Extract date
     */
    private fun extractDate(text: String): LocalDate? {
        for (pattern in rules.datePatterns) {
            val match = pattern.find(text) ?: continue

            return try {
                // Try different date formats
                val dateStr = match.value
                parseDateString(dateStr)
            } catch (e: Exception) {
                null
            }
        }
        return null
    }

    private fun parseDateString(dateStr: String): LocalDate? {
        val formats = listOf(
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM-dd-yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy")
        )

        for (format in formats) {
            try {
                return LocalDate.parse(dateStr.replace(".", "/").replace("-", "/"), format)
            } catch (e: Exception) {
                continue
            }
        }

        return null
    }

    /**
     * Extract time
     */
    private fun extractTime(text: String): LocalTime? {
        val timePattern = rules.timePatterns.first()
        val match = timePattern.find(text) ?: return null

        return try {
            val timeStr = match.value.replace(" ", "")
            LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extract items from receipt
     */
    private fun extractItems(lines: List<String>): List<ReceiptItem> {
        val items = mutableListOf<ReceiptItem>()

        for (line in lines) {
            if (!rules.isLikelyItemLine(line)) continue

            val price = rules.extractPriceFromLine(line) ?: continue
            val name = rules.extractItemName(line) ?: continue

            items.add(
                ReceiptItem(
                    name = name,
                    price = price
                )
            )
        }

        return items
    }

    /**
     * Extract total amount
     */
    private fun extractTotal(lines: List<String>, fullText: String): Double? {
        // Look for lines with "total" keyword
        val totalLines = lines.filter { line ->
            rules.totalKeywords.any { keyword ->
                line.contains(keyword, ignoreCase = true)
            }
        }

        // Extract highest price from total lines
        return totalLines.mapNotNull { line ->
            rules.extractPriceFromLine(line)
        }.maxOrNull() ?: findLargestPrice(fullText)
    }

    /**
     * Extract subtotal
     */
    private

    fun extractSubtotal(lines: List<String>, fullText: String): Double? {
        val subtotalLines = lines.filter { line ->
            rules.subtotalKeywords.any { keyword ->
                line.contains(keyword, ignoreCase = true)
            }
        }

        return subtotalLines.firstNotNullOfOrNull { line ->
            rules.extractPriceFromLine(line)
        }
    }

    /**
     * Extract tax amount
     */
    private fun extractTax(lines: List<String>, fullText: String): Double? {
        val taxLines = lines.filter { line ->
            rules.taxKeywords.any { keyword ->
                line.contains(keyword, ignoreCase = true)
            }
        }

        return taxLines.firstNotNullOfOrNull { line ->
            rules.extractPriceFromLine(line)
        }
    }

    /**
     * Find largest price in text (fallback for total)
     */
    private fun findLargestPrice(text: String): Double? {
        return TextCleaner.extractNumbers(text).maxOrNull()
    }

    /**
     * Extract payment method
     */
    private fun extractPaymentMethod(text: String): String? {
        return rules.paymentKeywords.firstOrNull { keyword ->
            text.contains(keyword, ignoreCase = true)
        }?.replaceFirstChar { it.uppercase() }
    }

    /**
     * Extract transaction ID
     */
    private fun extractTransactionId(text: String): String? {
        return rules.transactionIdPatterns.firstNotNullOfOrNull { pattern ->

            pattern.find(text)?.groupValues?.get(1)
        }
    }

    /**
     * Extract cashier name
     */
    private fun extractCashier(text: String): String? {
        return rules.cashierPatterns.firstNotNullOfOrNull { pattern ->
            pattern.find(text)?.groupValues?.get(1)?.trim()
        }
    }

}

