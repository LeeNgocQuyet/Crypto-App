package com.project.cryptoapp.domain.crypto

import com.project.cryptoapp.domain.model.CipherText
import com.project.cryptoapp.domain.model.DigitalSignature
import com.project.cryptoapp.domain.model.ECCKeyPair

interface ECCryptoService {
    suspend fun generateKeyPair(): ECCKeyPair
    suspend fun encrypt(plaintext: String, publicKey: String, aad: String = ""): CipherText
    suspend fun decrypt(cipherText: String, privateKey: String, aad: String = ""): String
    suspend fun sign(message: String, privateKey: String): DigitalSignature
    suspend fun verify(message: String, publicKey: String, signature: DigitalSignature): Boolean
}
