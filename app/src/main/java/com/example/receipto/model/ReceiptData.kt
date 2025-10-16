package com.example.receipto.model

data class ReceiptData(
    val store: String? = null,
    val date: String? = null,
    val time: String? = null,
    val items: List<ItemData> = emptyList(),
    val tax: Double? = null,
    val total: Double? = null,
    val holder: String? = null,
    val barcode: String? = null,
    val location: String? = null
)

data class ItemData(
    val name: String,
    val qty: Int,
    val price: Double
)
