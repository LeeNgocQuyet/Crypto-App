package com.project.cryptoapp.domain.repository

import com.project.cryptoapp.domain.model.CryptoHistory
import kotlinx.coroutines.flow.Flow

interface CryptoHistoryRepository {
    fun getAllHistory(): Flow<List<CryptoHistory>>
    suspend fun saveHistory(history: CryptoHistory)
    suspend fun deleteHistoryById(id: Long)
    suspend fun clearHistory()
}
