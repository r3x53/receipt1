package com.example.receipto.parser

/**
 * Regex patterns and keyword rules for receipt parsing
 */
object ParsingRules {

    // ========== DATE & TIME PATTERNS ==========

    val datePatterns = listOf(
        // MM/DD/YYYY or DD/MM/YYYY
        Regex("""\b(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{4})\b"""),
        // MM/DD/YY
        Regex("""\b(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{2})\b"""),
        // YYYY-MM-DD (ISO format)
        Regex("""\b(\d{4})[/\-.](\d{1,2})[/\-.](\d{1,2})\b"""),
        // Month DD, YYYY (e.g., "Jan 15, 2025")
        Regex("""\b(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\w*\s+(\d{1,2}),?\s+(\d{4})\b""", RegexOption.IGNORE_CASE)
    )

    val timePatterns = listOf(
        // HH:MM:SS or HH:MM
        Regex("""\b(\d{1,2}):(\d{2})(?::(\d{2}))?\s*(AM|PM|am|pm)?\b"""),
        // Military time
        Regex("""\b(\d{4})\b""") // e.g., "1730" for 5:30 PM
    )

    // ========== PRICE PATTERNS ==========

    val pricePatterns = listOf(
        // $12.99 or $12,99
        Regex("""\$\s*(\d+)[.,](\d{2})\b"""),
        // 12.99 or 12,99 (without $)
        Regex("""\b(\d+)[.,](\d{2})\b"""),
        // 12.9 (single decimal)
        Regex("""\b(\d+)\.(\d)\b""")
    )

    // ========== KEYWORDS ==========

    val storeNameIndicators = listOf(
        "store", "market", "shop", "mart", "supermarket", "grocery", "pharmacy", "restaurant", "cafe", "coffee"
    )

    val addressIndicators = listOf(
        "street", "st", "avenue", "ave", "road", "rd", "drive", "dr", "blvd", "boulevard", "lane", "ln", "plaza", "suite", "unit"
    )

    val totalKeywords = listOf(
        "total", "grand total", "amount due", "balance due", "amount", "net total", "final total", "gtotal", "g.total"
    )

    val subtotalKeywords = listOf(
        "subtotal", "sub-total", "sub total", "sub", "merchandise total", "item total"
    )

    val taxKeywords = listOf(
        "tax", "sales tax", "gst", "vat", "hst", "state tax", "local tax", "sales", "levy"
    )

    val paymentKeywords = listOf(
        "cash", "credit", "debit", "card", "visa", "mastercard", "amex", "discover", "payment", "paid", "tender"
    )

    val excludeFromStoreName = listOf(
        "receipt", "invoice", "bill", "ticket", "order", "thank you", "thanks", "welcome", "goodbye"
    )

// ========== ITEM LINE PATTERNS ==========

    /**
     * Detect if a line looks like an item (text followed by price)
     */
    fun isLikelyItemLine(line: String): Boolean {
        val cleanLine = line.trim()
        if (cleanLine.length < 3) return false

        // Must have some text and end with a number
        val hasText = cleanLine.any { it.isLetter() }
        val endsWithNumber = pricePatterns.any { it.find(cleanLine) != null }

        // Exclude lines with total keywords
        val hasExcludedKeyword = totalKeywords.any {
            cleanLine.contains(it, ignoreCase = true)
        } || taxKeywords.any {
            cleanLine.contains(it, ignoreCase = true)
        }

        return hasText && endsWithNumber && !hasExcludedKeyword
    }

    /**
     * Extract price from end of line
     */
    fun extractPriceFromLine(line: String): Double? {
        val matches = pricePatterns.mapNotNull { it.findAll(line).lastOrNull() }
        if (matches.isEmpty()) return null

        val match = matches.last()
        val priceStr = match.value.replace("$", "").replace(",", ".")
        return priceStr.toDoubleOrNull()
    }

    /**
     * Extract item name (everything before the price)
     */
    fun extractItemName(line: String): String? {
        val priceMatch = pricePatterns.mapNotNull {
            it.findAll(line).lastOrNull()
        }.lastOrNull() ?: return null

        val nameEnd = priceMatch.range.first
        val name = line.substring(0, nameEnd).trim()

        return if (name.length >= 2) name else null
    }

// ========== PHONE NUMBER PATTERNS ==========

    val phonePatterns = listOf(
        // (555) 123-4567
        Regex("""\(?\d{3}\)?[\s.-]?\d{3}[\s.-]?\d{4}"""),
        // +1 555 123 4567
        Regex("""\+?\d{1,3}[\s.-]?\d{3}[\s.-]?\d{3}[\s.-]?\d{4}""")
    )

// ========== TRANSACTION ID PATTERNS ==========

    val transactionIdPatterns = listOf(
        // Trans: 123456 or Trans #: 123456
        Regex(

            """(?:trans|transaction|trans#|ref|reference)[:\s#](\d+)""", RegexOption.IGNORE_CASE),
// Invoice #123456
        Regex("""(?:invoice|inv)[:\s#](\d+)""", RegexOption.IGNORE_CASE)
    )

// ========== CASHIER PATTERNS ==========

    val cashierPatterns = listOf(
        Regex("""(?:cashier|served by|server|clerk)[:\s]+(.+?)(?:\n|$)""", RegexOption.IGNORE_CASE),
        Regex("""(?:your cashier was|cashier:)\s+(.+?)(?:\n|$)""", RegexOption.IGNORE_CASE)
    )

}

