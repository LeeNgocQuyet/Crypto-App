package com.project.cryptoapp.data.repository

import com.project.cryptoapp.domain.crypto.ECCryptoService
import com.project.cryptoapp.domain.model.CipherText
import com.project.cryptoapp.domain.model.DigitalSignature
import com.project.cryptoapp.domain.model.ECCKeyPair
import com.project.cryptoapp.domain.model.ECPoint
import kotlinx.coroutines.delay
import kotlin.random.Random

class MockECCryptoService : ECCryptoService {
    override suspend fun generateKeyPair(): ECCKeyPair {
        delay(MOCK_DELAY_MS)
        return ECCKeyPair(
            privateKey = "0x${mockHex(64)}",
            publicKey = ECPoint(
                x = "0x${mockHex(128)}",
                y = "0x${mockHex(128)}",
            ),
        )
    }

    override suspend fun encrypt(plaintext: String, publicKey: String): CipherText {
        delay(MOCK_DELAY_MS)
        return CipherText(
            c1 = ECPoint(
                x = "0x${mockHex(128)}",
                y = "0x${mockHex(128)}",
            ),
            c2 = ECPoint(
                x = "0x${mockHex(128)}",
                y = "0x${mockHex(128)}",
            ),
        )
    }

    override suspend fun decrypt(cipherText: String, privateKey: String): String {
        delay(MOCK_DELAY_MS)
        return "Decrypted mock plaintext"
    }

    override suspend fun sign(message: String, privateKey: String): DigitalSignature {
        delay(MOCK_DELAY_MS)
        return DigitalSignature(
            r = "0x${mockHex(128)}",
            s = "0x${mockHex(128)}",
        )
    }

    override suspend fun verify(
        message: String,
        publicKey: String,
        signature: DigitalSignature,
    ): Boolean {
        delay(MOCK_DELAY_MS)
        return message.isNotBlank() &&
            publicKey.isNotBlank() &&
            signature.r.isNotBlank() &&
            signature.s.isNotBlank()
    }

    private fun mockHex(length: Int): String =
        buildString(length) {
            repeat(length) {
                append(HEX[Random.nextInt(HEX.length)])
            }
        }

    private companion object {
        const val MOCK_DELAY_MS = 350L
        const val HEX = "0123456789abcdef"
    }
}
