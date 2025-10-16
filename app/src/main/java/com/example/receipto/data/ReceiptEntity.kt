package com.example.receipto.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val store: String?,
    val date: String?,
    val time: String?,
    val total: Double?,
    val json: String
)
