package com.project.cryptoapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.project.cryptoapp.data.local.entity.CryptoHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CryptoHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: CryptoHistoryEntity)

    @Query("SELECT * FROM crypto_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<CryptoHistoryEntity>>

    @Query("DELETE FROM crypto_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM crypto_history")
    suspend fun clearHistory()
}
