package com.project.cryptoapp

import com.project.cryptoapp.data.repository.RealECCryptoService
import com.project.cryptoapp.util.FileSignatureCodec
import com.project.cryptoapp.util.FileSignaturePayload
import com.project.cryptoapp.util.KeyPayloadCodec
import com.project.cryptoapp.util.toDisplayString
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PayloadCodecsTest {
    private val service = RealECCryptoService()

    @Test
    fun keyPayloadRoundTripPreservesPublicAndPrivateKeys() = runBlocking {
        val keyPair = service.generateKeyPair()
        val publicKey = keyPair.publicKey.toDisplayString()

        val encoded = KeyPayloadCodec.encode(KeyPayloadCodec.privateKeyPayload(keyPair.privateKey, publicKey))
        val decoded = KeyPayloadCodec.decode(encoded)

        assertEquals("private", decoded.type)
        assertEquals(keyPair.privateKey, decoded.privateKey)
        assertEquals(keyPair.publicKey.x, decoded.publicKey.x)
        assertEquals(keyPair.publicKey.y, decoded.publicKey.y)
        assertNotNull(decoded.fingerprint)
    }

    @Test
    fun fileSignaturePayloadRoundTripPreservesSignature() = runBlocking {
        val keyPair = service.generateKeyPair()
        val digest = "0x" + "AA".repeat(64)
        val signature = service.sign(digest, keyPair.privateKey)

        val encoded = FileSignatureCodec.encode(
            FileSignaturePayload(
                fileName = "document.bin",
                sha512 = digest,
                signature = signature,
            ),
        )
        val decoded = FileSignatureCodec.decode(encoded)

        assertEquals("document.bin", decoded.fileName)
        assertEquals(digest, decoded.sha512)
        assertEquals(signature, decoded.signature)
    }
}
