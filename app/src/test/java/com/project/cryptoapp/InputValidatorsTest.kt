package com.project.cryptoapp

import com.project.cryptoapp.data.repository.RealECCryptoService
import com.project.cryptoapp.util.toDisplayString
import com.project.cryptoapp.util.validateCipherText
import com.project.cryptoapp.util.validatePayload
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class InputValidatorsTest {
    private val service = RealECCryptoService()

    @Test
    fun validateCipherTextAcceptsHybridPayload() = runBlocking {
        val keyPair = service.generateKeyPair()
        val cipherText = service.encrypt("validator test", keyPair.publicKey.toDisplayString())

        assertNull(validateCipherText(cipherText.toDisplayString()))
    }

    @Test
    fun validateCipherTextRejectsUnsupportedAlgorithm() = runBlocking {
        val keyPair = service.generateKeyPair()
        val cipherText = service.encrypt("validator test", keyPair.publicKey.toDisplayString())
        val tampered = cipherText.toDisplayString().replace(
            "ECDH-HKDF-SHA512-AES-256-GCM",
            "EC-ElGamal",
        )

        assertNotNull(validateCipherText(tampered))
    }

    @Test
    fun validateCipherTextRejectsWrongTagSize() = runBlocking {
        val keyPair = service.generateKeyPair()
        val cipherText = service.encrypt("validator test", keyPair.publicKey.toDisplayString())
        val tampered = cipherText.toDisplayString().replaceFirst(
            Regex(""""tag":\s*"0x[0-9A-F]+""""),
            """"tag": "0xAA"""",
        )

        assertNotNull(validateCipherText(tampered))
    }

    @Test
    fun validatePayloadRejectsOddHexBytes() {
        assertNotNull(validatePayload("Plaintext", "0xABC", "Hex"))
    }
}
