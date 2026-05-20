package com.project.cryptoapp

import com.project.cryptoapp.data.repository.RealECCryptoService
import com.project.cryptoapp.util.toDisplayString
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RealECCryptoServiceTest {
    private val service = RealECCryptoService()

    @Test
    fun encryptThenDecryptReturnsOriginalPlaintext() = runBlocking {
        val keyPair = service.generateKeyPair()
        val plaintext = "ECC real algorithm"

        val cipherText = service.encrypt(plaintext, keyPair.publicKey.toDisplayString())
        val decrypted = service.decrypt(cipherText.toDisplayString(), keyPair.privateKey)

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun signThenVerifyAcceptsOriginalMessage() = runBlocking {
        val keyPair = service.generateKeyPair()
        val message = "message to sign"

        val signature = service.sign(message, keyPair.privateKey)
        val isValid = service.verify(message, keyPair.publicKey.toDisplayString(), signature)

        assertTrue(isValid)
    }

    @Test
    fun verifyRejectsTamperedMessage() = runBlocking {
        val keyPair = service.generateKeyPair()
        val signature = service.sign("original", keyPair.privateKey)

        val isValid = service.verify("tampered", keyPair.publicKey.toDisplayString(), signature)

        assertFalse(isValid)
    }
}
