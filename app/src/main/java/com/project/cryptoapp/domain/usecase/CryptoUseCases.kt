package com.project.cryptoapp.domain.usecase

import com.project.cryptoapp.domain.crypto.ECCryptoService
import com.project.cryptoapp.domain.model.CryptoHistory
import com.project.cryptoapp.domain.model.DigitalSignature
import com.project.cryptoapp.domain.repository.CryptoHistoryRepository
import kotlinx.coroutines.flow.Flow

class GenerateKeyPairUseCase(
    private val cryptoService: ECCryptoService,
) {
    suspend operator fun invoke() = cryptoService.generateKeyPair()
}

class EncryptMessageUseCase(
    private val cryptoService: ECCryptoService,
) {
    suspend operator fun invoke(plaintext: String, publicKey: String) =
        cryptoService.encrypt(plaintext, publicKey)
}

class DecryptMessageUseCase(
    private val cryptoService: ECCryptoService,
) {
    suspend operator fun invoke(cipherText: String, privateKey: String) =
        cryptoService.decrypt(cipherText, privateKey)
}

class SignMessageUseCase(
    private val cryptoService: ECCryptoService,
) {
    suspend operator fun invoke(message: String, privateKey: String) =
        cryptoService.sign(message, privateKey)
}

class VerifySignatureUseCase(
    private val cryptoService: ECCryptoService,
) {
    suspend operator fun invoke(message: String, publicKey: String, signature: DigitalSignature) =
        cryptoService.verify(message, publicKey, signature)
}

class GetHistoryUseCase(
    private val repository: CryptoHistoryRepository,
) {
    operator fun invoke(): Flow<List<CryptoHistory>> = repository.getAllHistory()
}

class SaveHistoryUseCase(
    private val repository: CryptoHistoryRepository,
) {
    suspend operator fun invoke(history: CryptoHistory) = repository.saveHistory(history)
}

class ClearHistoryUseCase(
    private val repository: CryptoHistoryRepository,
) {
    suspend operator fun invoke() = repository.clearHistory()
}
