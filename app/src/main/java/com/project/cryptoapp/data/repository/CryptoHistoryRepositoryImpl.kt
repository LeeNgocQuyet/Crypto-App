package com.project.cryptoapp.data.repository

import com.project.cryptoapp.data.local.dao.CryptoHistoryDao
import com.project.cryptoapp.data.local.entity.CryptoHistoryEntity
import com.project.cryptoapp.domain.model.CryptoHistory
import com.project.cryptoapp.domain.model.OperationStatus
import com.project.cryptoapp.domain.model.OperationType
import com.project.cryptoapp.domain.repository.CryptoHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CryptoHistoryRepositoryImpl(
    private val dao: CryptoHistoryDao,
) : CryptoHistoryRepository {
    override fun getAllHistory(): Flow<List<CryptoHistory>> =
        dao.getAllHistory().map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveHistory(history: CryptoHistory) {
        dao.insertHistory(history.toEntity())
    }

    override suspend fun deleteHistoryById(id: Long) {
        dao.deleteHistoryById(id)
    }

    override suspend fun clearHistory() {
        dao.clearHistory()
    }
}

private fun CryptoHistoryEntity.toDomain() = CryptoHistory(
    id = id,
    operationType = OperationType.valueOf(operationType),
    inputText = inputText,
    outputText = outputText,
    status = OperationStatus.valueOf(status),
    timestamp = timestamp,
)

private fun CryptoHistory.toEntity() = CryptoHistoryEntity(
    id = id,
    operationType = operationType.name,
    inputText = inputText,
    outputText = outputText,
    status = status.name,
    timestamp = timestamp,
)
