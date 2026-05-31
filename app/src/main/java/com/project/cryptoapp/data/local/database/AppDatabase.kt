package com.project.cryptoapp.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.project.cryptoapp.data.local.dao.CryptoHistoryDao
import com.project.cryptoapp.data.local.entity.CryptoHistoryEntity

@Database(
    entities = [CryptoHistoryEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cryptoHistoryDao(): CryptoHistoryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE crypto_history ADD COLUMN durationNanos INTEGER")
            }
        }
    }
}
