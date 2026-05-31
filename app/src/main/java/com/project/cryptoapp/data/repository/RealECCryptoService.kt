package com.project.cryptoapp.data.repository

import com.project.cryptoapp.domain.crypto.ActiveCurveRegistry
import com.project.cryptoapp.domain.crypto.ECCryptoService
import com.project.cryptoapp.domain.crypto.ECCurveMath
import com.project.cryptoapp.domain.crypto.ECCurveSpec
import com.project.cryptoapp.domain.crypto.RuntimeCurvePoint
import com.project.cryptoapp.domain.crypto.toHex
import com.project.cryptoapp.domain.model.CipherText
import com.project.cryptoapp.domain.model.DigitalSignature
import com.project.cryptoapp.domain.model.ECCKeyPair
import com.project.cryptoapp.domain.model.ECPoint
import com.project.cryptoapp.util.CryptoPayloadCodec
import java.math.BigInteger
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class RealECCryptoService(
    private val activeCurveRepository: ActiveCurveRepository? = null,
    private val secureRandom: SecureRandom = SecureRandom(),
) : ECCryptoService {
    override suspend fun generateKeyPair(): ECCKeyPair {
        val curve = activeCurve()
        val math = ECCurveMath(curve)
        val privateKey = randomScalar(curve)
        val publicKey = math.multiply(privateKey, curve.basePoint.toRuntimePoint()).toDomainPoint(curve)
        return ECCKeyPair(
            privateKey = privateKey.toHex(curve.scalarHexWidth()),
            publicKey = publicKey,
        )
    }

    override suspend fun encrypt(plaintext: String, publicKey: String, aad: String): CipherText {
        val curve = activeCurve()
        val math = ECCurveMath(curve)
        val recipientPublicKey = parsePublicKey(publicKey)
        math.requirePointOnCurve(recipientPublicKey, "Public key")

        val ephemeralPrivateKey = randomScalar(curve)
        val ephemeralPublicKey = math.multiply(ephemeralPrivateKey, curve.basePoint.toRuntimePoint())
        val sharedSecret = math.multiply(ephemeralPrivateKey, recipientPublicKey)
        require(!sharedSecret.isInfinity) { "ECDH shared secret is invalid" }

        val salt = randomBytes(SALT_BYTES)
        val nonce = randomBytes(GCM_NONCE_BYTES)
        val aadBytes = decodeInputPayload(aad)
        val key = deriveAesKey(curve, sharedSecret, salt)
        val encrypted = aesGcmEncrypt(key, nonce, decodeInputPayload(plaintext), aadBytes)

        return CipherText(
            algorithm = ALGORITHM,
            curve = curve.id,
            curveFingerprint = curve.fingerprint,
            ephemeralPublicKey = ephemeralPublicKey.toDomainPoint(curve),
            salt = salt.toHexString(),
            nonce = nonce.toHexString(),
            aad = aadBytes.toHexString(),
            cipherText = encrypted.cipherText.toHexString(),
            tag = encrypted.tag.toHexString(),
        )
    }

    override suspend fun decrypt(cipherText: String, privateKey: String, aad: String): String {
        val curve = activeCurve()
        val math = ECCurveMath(curve)
        val secret = parsePrivateKey(curve, privateKey)
        val payload = CryptoPayloadCodec.decode(cipherText)
        require(payload.algorithm == ALGORITHM) { "Unsupported algorithm: ${payload.algorithm}" }
        require(payload.curve == curve.id) { "Unsupported curve: ${payload.curve}; active curve is ${curve.id}" }
        payload.curveFingerprint?.let {
            require(it.equals(curve.fingerprint, ignoreCase = true)) { "Curve fingerprint does not match active curve" }
        }

        val ephemeralPublicKey = RuntimeCurvePoint(
            x = parseHex(payload.ephemeralPublicKey.x, "ephemeralPublicKey.x"),
            y = parseHex(payload.ephemeralPublicKey.y, "ephemeralPublicKey.y"),
        )
        math.requirePointOnCurve(ephemeralPublicKey, "Ephemeral public key")

        val sharedSecret = math.multiply(secret, ephemeralPublicKey)
        require(!sharedSecret.isInfinity) { "ECDH shared secret is invalid" }

        val salt = parseHexBytes(payload.salt, "salt")
        val nonce = parseHexBytes(payload.nonce, "nonce")
        val aadBytes = if (aad.isBlank()) parseHexBytes(payload.aad, "aad") else decodeInputPayload(aad)
        val encryptedBytes = parseHexBytes(payload.cipherText, "ciphertext")
        val tag = parseHexBytes(payload.tag, "tag")
        val key = deriveAesKey(curve, sharedSecret, salt)
        return encodeOutputPayload(aesGcmDecrypt(key, nonce, encryptedBytes, tag, aadBytes))
    }

    override suspend fun sign(message: String, privateKey: String): DigitalSignature {
        val curve = activeCurve()
        val math = ECCurveMath(curve)
        val secret = parsePrivateKey(curve, privateKey)
        val digestBytes = hashMessage(message)
        val digest = hashToScalar(curve, digestBytes)
        val nonceGenerator = DeterministicNonceGenerator(curve, secret, digestBytes)

        while (true) {
            val k = nonceGenerator.next()
            val r = math.multiply(k, curve.basePoint.toRuntimePoint()).x.mod(curve.n)
            if (r == BigInteger.ZERO) {
                nonceGenerator.reject()
                continue
            }

            val s = k.modInverse(curve.n)
                .multiply(digest.add(r.multiply(secret)))
                .mod(curve.n)
            if (s != BigInteger.ZERO) {
                return DigitalSignature(
                    r = r.toHex(curve.scalarHexWidth()),
                    s = s.toHex(curve.scalarHexWidth()),
                )
            }
            nonceGenerator.reject()
        }
    }

    override suspend fun verify(
        message: String,
        publicKey: String,
        signature: DigitalSignature,
    ): Boolean {
        val curve = activeCurve()
        val math = ECCurveMath(curve)
        val q = parsePublicKey(publicKey)
        math.requirePointOnCurve(q, "Public key")

        val r = parseHex(signature.r, "Signature r")
        val s = parseHex(signature.s, "Signature s")
        if (!isScalarInRange(curve, r) || !isScalarInRange(curve, s)) return false

        val digest = hashToScalar(curve, message)
        val w = s.modInverse(curve.n)
        val u1 = digest.multiply(w).mod(curve.n)
        val u2 = r.multiply(w).mod(curve.n)
        val point = math.add(
            math.multiply(u1, curve.basePoint.toRuntimePoint()),
            math.multiply(u2, q),
        )
        if (point.isInfinity) return false

        return point.x.mod(curve.n) == r
    }

    private suspend fun activeCurve(): ECCurveSpec =
        activeCurveRepository?.activeCurve() ?: ActiveCurveRegistry.current

    private fun parsePrivateKey(curve: ECCurveSpec, value: String): BigInteger {
        val key = parseHex(value, "Private key")
        require(isScalarInRange(curve, key)) { "Private key must be in range [1, n - 1]" }
        return key
    }

    private fun isScalarInRange(curve: ECCurveSpec, value: BigInteger): Boolean =
        value >= ONE && value < curve.n

    private fun parsePublicKey(value: String): RuntimeCurvePoint {
        val x = readLabeledHex(value, "x") ?: throw IllegalArgumentException("Public key is missing x")
        val y = readLabeledHex(value, "y") ?: throw IllegalArgumentException("Public key is missing y")
        return RuntimeCurvePoint(x, y)
    }

    private fun readLabeledHex(source: String, label: String): BigInteger? {
        val escapedLabel = Regex.escape(label)
        val regex = Regex("""(?im)^\s*$escapedLabel\s*:\s*(0x[0-9a-f]+|[0-9a-f]+)\s*$""")
        return regex.find(source)?.groupValues?.get(1)?.let { parseHex(it, label) }
    }

    private fun parseHex(value: String, label: String): BigInteger {
        val normalized = value.trim().removePrefix("0x").removePrefix("0X")
        require(normalized.isNotBlank() && normalized.all { it in HEX_DIGITS }) {
            "$label must be hex"
        }
        return BigInteger(normalized, 16)
    }

    private fun parseHexBytes(value: String, label: String): ByteArray {
        val normalized = value.trim().removePrefix("0x").removePrefix("0X")
        require(normalized.length % 2 == 0 && normalized.all { it in HEX_DIGITS }) {
            "$label must be even-length hex"
        }
        return normalized.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    private fun decodeInputPayload(value: String): ByteArray {
        if (value.isBlank()) return ByteArray(0)
        val trimmed = value.trim()
        val hex = trimmed.removePrefix("0x").removePrefix("0X")
        return if (trimmed.startsWith("0x", ignoreCase = true) && hex.length % 2 == 0 && hex.all { it in HEX_DIGITS }) {
            hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        } else {
            value.toByteArray(StandardCharsets.UTF_8)
        }
    }

    private fun encodeOutputPayload(payload: ByteArray): String {
        val decoder = StandardCharsets.UTF_8
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val decoded = runCatching { decoder.decode(java.nio.ByteBuffer.wrap(payload)).toString() }.getOrNull()
        return if (decoded != null && decoded.none { it.isISOControl() && it !in PRINTABLE_CONTROLS }) {
            decoded
        } else {
            "0x" + payload.joinToString("") { "%02X".format(it) }
        }
    }

    private fun hashToScalar(curve: ECCurveSpec, message: String): BigInteger =
        hashToScalar(curve, hashMessage(message))

    private fun hashMessage(message: String): ByteArray =
        MessageDigest.getInstance("SHA-512").digest(decodeInputPayload(message))

    private fun hashToScalar(curve: ECCurveSpec, digest: ByteArray): BigInteger =
        BigInteger(1, digest).mod(curve.n)

    private fun randomScalar(curve: ECCurveSpec): BigInteger {
        while (true) {
            val candidate = BigInteger(curve.n.bitLength(), secureRandom).mod(curve.n)
            if (candidate != BigInteger.ZERO) return candidate
        }
    }

    private fun randomBytes(size: Int): ByteArray =
        ByteArray(size).also(secureRandom::nextBytes)

    private fun deriveAesKey(curve: ECCurveSpec, sharedSecret: RuntimeCurvePoint, salt: ByteArray): ByteArray {
        val inputKeyMaterial = sharedSecret.x.toFixedBytes(FIELD_BYTES) + sharedSecret.y.toFixedBytes(FIELD_BYTES)
        return hkdfSha512(
            inputKeyMaterial = inputKeyMaterial,
            salt = salt,
            info = hkdfInfo(curve).toByteArray(StandardCharsets.UTF_8),
            outputLength = AES_KEY_BYTES,
        )
    }

    private fun hkdfSha512(
        inputKeyMaterial: ByteArray,
        salt: ByteArray,
        info: ByteArray,
        outputLength: Int,
    ): ByteArray {
        val mac = Mac.getInstance(HMAC_SHA512)
        mac.init(SecretKeySpec(salt, HMAC_SHA512))
        val pseudoRandomKey = mac.doFinal(inputKeyMaterial)

        val output = mutableListOf<Byte>()
        var previous = ByteArray(0)
        var counter = 1
        while (output.size < outputLength) {
            mac.init(SecretKeySpec(pseudoRandomKey, HMAC_SHA512))
            mac.update(previous)
            mac.update(info)
            mac.update(counter.toByte())
            previous = mac.doFinal()
            output.addAll(previous.toList())
            counter++
        }
        return output.take(outputLength).toByteArray()
    }

    private fun aesGcmEncrypt(
        key: ByteArray,
        nonce: ByteArray,
        plaintext: ByteArray,
        aad: ByteArray,
    ): AeadResult {
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, AES), GCMParameterSpec(GCM_TAG_BITS, nonce))
        if (aad.isNotEmpty()) cipher.updateAAD(aad)
        val sealed = cipher.doFinal(plaintext)
        return AeadResult(
            cipherText = sealed.copyOfRange(0, sealed.size - GCM_TAG_BYTES),
            tag = sealed.copyOfRange(sealed.size - GCM_TAG_BYTES, sealed.size),
        )
    }

    private fun aesGcmDecrypt(
        key: ByteArray,
        nonce: ByteArray,
        cipherText: ByteArray,
        tag: ByteArray,
        aad: ByteArray,
    ): ByteArray {
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, AES), GCMParameterSpec(GCM_TAG_BITS, nonce))
        if (aad.isNotEmpty()) cipher.updateAAD(aad)
        return cipher.doFinal(cipherText + tag)
    }

    private fun RuntimeCurvePoint.toDomainPoint(curve: ECCurveSpec): ECPoint =
        ECPoint(x = x.toHex(curve.fieldHexWidth()), y = y.toHex(curve.fieldHexWidth()), isInfinity = isInfinity)

    private fun BigInteger.toFixedBytes(size: Int): ByteArray {
        val raw = toByteArray()
        val unsigned = if (raw.size > 1 && raw.first() == 0.toByte()) raw.copyOfRange(1, raw.size) else raw
        require(unsigned.size <= size) { "Integer is too large" }
        return ByteArray(size - unsigned.size) + unsigned
    }

    private fun ByteArray.toHexString(): String =
        "0x" + joinToString("") { "%02X".format(it) }

    private fun com.project.cryptoapp.domain.crypto.CurvePointSpec.toRuntimePoint(): RuntimeCurvePoint =
        RuntimeCurvePoint(x, y)

    private fun ECCurveSpec.fieldHexWidth(): Int = fieldSize / 4

    private fun ECCurveSpec.scalarHexWidth(): Int = maxOf(fieldHexWidth(), n.bitLength().let { (it + 3) / 4 })

    private fun hkdfInfo(curve: ECCurveSpec): String =
        "CryptoApp|${curve.id}|${curve.fingerprint}|$ALGORITHM|v1"

    private inner class DeterministicNonceGenerator(
        private val curve: ECCurveSpec,
        private val privateKey: BigInteger,
        messageDigest: ByteArray,
    ) {
        private var k = ByteArray(HMAC_OUTPUT_BYTES)
        private var v = ByteArray(HMAC_OUTPUT_BYTES) { 0x01 }

        init {
            val seed = privateKey.toFixedBytes(SCALAR_BYTES) + bitsToOctets(messageDigest)
            k = hmac(k, v + byteArrayOf(0x00) + seed)
            v = hmac(k, v)
            k = hmac(k, v + byteArrayOf(0x01) + seed)
            v = hmac(k, v)
        }

        fun next(): BigInteger {
            while (true) {
                var t = ByteArray(0)
                while (t.size < SCALAR_BYTES) {
                    v = hmac(k, v)
                    t += v
                }
                val candidate = bitsToInt(t)
                if (isScalarInRange(curve, candidate)) return candidate

                k = hmac(k, v + byteArrayOf(0x00))
                v = hmac(k, v)
            }
        }

        fun reject() {
            k = hmac(k, v + byteArrayOf(0x00))
            v = hmac(k, v)
        }

        private fun bitsToOctets(bytes: ByteArray): ByteArray =
            bitsToInt(bytes).mod(curve.n).toFixedBytes(SCALAR_BYTES)

        private fun bitsToInt(bytes: ByteArray): BigInteger {
            val value = BigInteger(1, bytes)
            val extraBits = bytes.size * Byte.SIZE_BITS - curve.n.bitLength()
            return if (extraBits > 0) value.shiftRight(extraBits) else value
        }

        private fun hmac(key: ByteArray, data: ByteArray): ByteArray {
            val mac = Mac.getInstance(HMAC_SHA512)
            mac.init(SecretKeySpec(key, HMAC_SHA512))
            return mac.doFinal(data)
        }
    }

    private data class AeadResult(
        val cipherText: ByteArray,
        val tag: ByteArray,
    )

    private companion object {
        val ONE: BigInteger = BigInteger.ONE
        const val HEX_DIGITS = "0123456789abcdefABCDEF"
        const val ALGORITHM = "ECDH-HKDF-SHA512-AES-256-GCM"
        const val HMAC_SHA512 = "HmacSHA512"
        const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        const val AES = "AES"
        const val FIELD_BYTES = 64
        const val SCALAR_BYTES = 64
        const val HMAC_OUTPUT_BYTES = 64
        const val AES_KEY_BYTES = 32
        const val SALT_BYTES = 32
        const val GCM_NONCE_BYTES = 12
        const val GCM_TAG_BYTES = 16
        const val GCM_TAG_BITS = 128
        val PRINTABLE_CONTROLS = setOf('\n', '\r', '\t')
    }
}
