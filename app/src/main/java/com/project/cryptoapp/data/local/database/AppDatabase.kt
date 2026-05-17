package com.project.cryptoapp.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.project.cryptoapp.data.local.dao.CryptoHistoryDao
import com.project.cryptoapp.data.local.entity.CryptoHistoryEntity

@Database(
    entities = [CryptoHistoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cryptoHistoryDao(): CryptoHistoryDao
}
