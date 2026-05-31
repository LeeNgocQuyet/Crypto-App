package com.project.cryptoapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "crypto_history")
data class CryptoHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val operationType: String,
    val inputText: String,
    val outputText: String,
    val status: String,
    val timestamp: Long,
    val durationNanos: Long? = null,
)
