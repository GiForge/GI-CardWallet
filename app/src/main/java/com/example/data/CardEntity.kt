package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallet_cards")
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String,
    val format: String, // "QR_CODE", "EAN_13", "EAN_8", "CODE_128", "CODE_39", "UPC_A", "ITF"
    val logoBase64: String?,
    val logoUrl: String?,
    val colorHex: String, // "#1C1C1E" etc.
    val createdAt: Long = System.currentTimeMillis()
)
