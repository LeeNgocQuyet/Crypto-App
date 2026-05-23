package com.project.cryptoapp.util

import android.content.Context
import androidx.room.Room
import com.project.cryptoapp.data.local.database.AppDatabase
import com.project.cryptoapp.data.repository.CryptoHistoryRepositoryImpl
import com.project.cryptoapp.data.repository.RealECCryptoService
import com.project.cryptoapp.domain.crypto.ECCryptoService
import com.project.cryptoapp.domain.repository.CryptoHistoryRepository
import com.project.cryptoapp.domain.usecase.ClearHistoryUseCase
import com.project.cryptoapp.domain.usecase.DecryptMessageUseCase
import com.project.cryptoapp.domain.usecase.DeleteHistoryByIdUseCase
import com.project.cryptoapp.domain.usecase.EncryptMessageUseCase
import com.project.cryptoapp.domain.usecase.GenerateKeyPairUseCase
import com.project.cryptoapp.domain.usecase.GetHistoryUseCase
import com.project.cryptoapp.domain.usecase.SaveHistoryUseCase
import com.project.cryptoapp.domain.usecase.SignMessageUseCase
import com.project.cryptoapp.domain.usecase.VerifySignatureUseCase

class AppContainer(context: Context) {
    private val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "ecc512_crypto.db",
    ).build()

    val cryptoService: ECCryptoService = RealECCryptoService()
    val cryptoSessionStore = CryptoSessionStore()
    val appSettingsStore = AppSettingsStore()

    val historyRepository: CryptoHistoryRepository =
        CryptoHistoryRepositoryImpl(database.cryptoHistoryDao())

    val generateKeyPairUseCase = GenerateKeyPairUseCase(cryptoService)
    val encryptMessageUseCase = EncryptMessageUseCase(cryptoService)
    val decryptMessageUseCase = DecryptMessageUseCase(cryptoService)
    val signMessageUseCase = SignMessageUseCase(cryptoService)
    val verifySignatureUseCase = VerifySignatureUseCase(cryptoService)
    val getHistoryUseCase = GetHistoryUseCase(historyRepository)
    val saveHistoryUseCase = SaveHistoryUseCase(historyRepository)
    val deleteHistoryByIdUseCase = DeleteHistoryByIdUseCase(historyRepository)
    val clearHistoryUseCase = ClearHistoryUseCase(historyRepository)
}
