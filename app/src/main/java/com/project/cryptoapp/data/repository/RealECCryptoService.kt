package com.project.cryptoapp.data.repository

import com.project.cryptoapp.domain.crypto.BrainpoolP512r1
import com.project.cryptoapp.domain.crypto.ECCryptoService
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
    private val secureRandom: SecureRandom = SecureRandom(),
) : ECCryptoService {
    override suspend fun generateKeyPair(): ECCKeyPair {
        val privateKey = randomScalar()
        val publicKey = multiply(privateKey, BASE_POINT).toDomainPoint()
        return ECCKeyPair(
            privateKey = privateKey.toHex(),
            publicKey = publicKey,
        )
    }

    override suspend fun encrypt(plaintext: String, publicKey: String, aad: String): CipherText {
        val recipientPublicKey = parsePublicKey(publicKey)
        requirePointOnCurve(recipientPublicKey, "Public key")

        val ephemeralPrivateKey = randomScalar()
        val ephemeralPublicKey = multiply(ephemeralPrivateKey, BASE_POINT)
        val sharedSecret = multiply(ephemeralPrivateKey, recipientPublicKey)
        require(!sharedSecret.isInfinity) { "ECDH shared secret is invalid" }

        val salt = randomBytes(SALT_BYTES)
        val nonce = randomBytes(GCM_NONCE_BYTES)
        val aadBytes = decodeInputPayload(aad)
        val key = deriveAesKey(sharedSecret, salt)
        val encrypted = aesGcmEncrypt(key, nonce, decodeInputPayload(plaintext), aadBytes)

        return CipherText(
            algorithm = ALGORITHM,
            curve = CURVE_NAME,
            ephemeralPublicKey = ephemeralPublicKey.toDomainPoint(),
            salt = salt.toHexString(),
            nonce = nonce.toHexString(),
            aad = aadBytes.toHexString(),
            cipherText = encrypted.cipherText.toHexString(),
            tag = encrypted.tag.toHexString(),
        )
    }

    override suspend fun decrypt(cipherText: String, privateKey: String, aad: String): String {
        val secret = parsePrivateKey(privateKey)
        val payload = CryptoPayloadCodec.decode(cipherText)
        require(payload.algorithm == ALGORITHM) { "Unsupported algorithm: ${payload.algorithm}" }
        require(payload.curve == CURVE_NAME) { "Unsupported curve: ${payload.curve}" }

        val ephemeralPublicKey = CurvePoint(
            x = parseHex(payload.ephemeralPublicKey.x, "ephemeralPublicKey.x"),
            y = parseHex(payload.ephemeralPublicKey.y, "ephemeralPublicKey.y"),
        )
        requirePointOnCurve(ephemeralPublicKey, "Ephemeral public key")

        val sharedSecret = multiply(secret, ephemeralPublicKey)
        require(!sharedSecret.isInfinity) { "ECDH shared secret is invalid" }

        val salt = parseHexBytes(payload.salt, "salt")
        val nonce = parseHexBytes(payload.nonce, "nonce")
        val aadBytes = if (aad.isBlank()) parseHexBytes(payload.aad, "aad") else decodeInputPayload(aad)
        val encryptedBytes = parseHexBytes(payload.cipherText, "ciphertext")
        val tag = parseHexBytes(payload.tag, "tag")
        val key = deriveAesKey(sharedSecret, salt)
        return encodeOutputPayload(aesGcmDecrypt(key, nonce, encryptedBytes, tag, aadBytes))
    }

    override suspend fun sign(message: String, privateKey: String): DigitalSignature {
        val secret = parsePrivateKey(privateKey)
        val digestBytes = hashMessage(message)
        val digest = hashToScalar(digestBytes)
        val nonceGenerator = DeterministicNonceGenerator(secret, digestBytes)

        while (true) {
            val k = nonceGenerator.next()
            val r = multiply(k, BASE_POINT).x.mod(N)
            if (r == BigInteger.ZERO) {
                nonceGenerator.reject()
                continue
            }

            val s = k.modInverse(N)
                .multiply(digest.add(r.multiply(secret)))
                .mod(N)
            if (s != BigInteger.ZERO) {
                return DigitalSignature(r = r.toHex(), s = s.toHex())
            }
            nonceGenerator.reject()
        }
    }

    override suspend fun verify(
        message: String,
        publicKey: String,
        signature: DigitalSignature,
    ): Boolean {
        val q = parsePublicKey(publicKey)
        requirePointOnCurve(q, "Public key")

        val r = parseHex(signature.r, "Signature r")
        val s = parseHex(signature.s, "Signature s")
        if (!isScalarInRange(r) || !isScalarInRange(s)) return false

        val digest = hashToScalar(message)
        val w = s.modInverse(N)
        val u1 = digest.multiply(w).mod(N)
        val u2 = r.multiply(w).mod(N)
        val point = add(multiply(u1, BASE_POINT), multiply(u2, q))
        if (point.isInfinity) return false

        return point.x.mod(N) == r
    }

    private fun parsePrivateKey(value: String): BigInteger {
        val key = parseHex(value, "Private key")
        require(isScalarInRange(key)) { "Private key must be in range [1, n - 1]" }
        return key
    }

    private fun isScalarInRange(value: BigInteger): Boolean =
        value >= ONE && value < N

    private fun parsePublicKey(value: String): CurvePoint {
        val x = readLabeledHex(value, "x") ?: throw IllegalArgumentException("Public key is missing x")
        val y = readLabeledHex(value, "y") ?: throw IllegalArgumentException("Public key is missing y")
        return CurvePoint(x, y)
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

    private fun requirePointOnCurve(point: CurvePoint, label: String) {
        require(!point.isInfinity) { "$label cannot be point at infinity" }
        require(point.x >= BigInteger.ZERO && point.x < P && point.y >= BigInteger.ZERO && point.y < P) {
            "$label coordinates must be inside Fp"
        }
        val left = point.y.multiply(point.y).mod(P)
        val right = point.x.modPow(THREE, P)
            .add(A.multiply(point.x))
            .add(B)
            .mod(P)
        require(left == right) { "$label is not on BrainpoolP512r1" }
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

    private fun hashToScalar(message: String): BigInteger {
        return hashToScalar(hashMessage(message))
    }

    private fun hashMessage(message: String): ByteArray =
        MessageDigest.getInstance("SHA-512").digest(decodeInputPayload(message))

    private fun hashToScalar(digest: ByteArray): BigInteger {
        return BigInteger(1, digest).mod(N)
    }

    private fun randomScalar(): BigInteger {
        while (true) {
            val candidate = BigInteger(N.bitLength(), secureRandom).mod(N)
            if (candidate != BigInteger.ZERO) return candidate
        }
    }

    private fun randomBytes(size: Int): ByteArray =
        ByteArray(size).also(secureRandom::nextBytes)

    private fun deriveAesKey(sharedSecret: CurvePoint, salt: ByteArray): ByteArray {
        val inputKeyMaterial = sharedSecret.x.toFixedBytes(FIELD_BYTES) + sharedSecret.y.toFixedBytes(FIELD_BYTES)
        return hkdfSha512(
            inputKeyMaterial = inputKeyMaterial,
            salt = salt,
            info = HKDF_INFO.toByteArray(StandardCharsets.UTF_8),
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

    private fun add(left: CurvePoint, right: CurvePoint): CurvePoint {
        if (left.isInfinity) return right
        if (right.isInfinity) return left

        if (left.x == right.x) {
            if (left.y.add(right.y).mod(P) == BigInteger.ZERO) return CurvePoint.INFINITY
            return double(left)
        }

        val lambda = right.y.subtract(left.y)
            .multiply(right.x.subtract(left.x).mod(P).modInverse(P))
            .mod(P)
        val x3 = lambda.multiply(lambda).subtract(left.x).subtract(right.x).mod(P)
        val y3 = lambda.multiply(left.x.subtract(x3)).subtract(left.y).mod(P)
        return CurvePoint(x3, y3)
    }

    private fun double(point: CurvePoint): CurvePoint {
        if (point.isInfinity || point.y == BigInteger.ZERO) return CurvePoint.INFINITY

        val numerator = THREE.multiply(point.x.multiply(point.x)).add(A).mod(P)
        val denominator = TWO.multiply(point.y).mod(P).modInverse(P)
        val lambda = numerator.multiply(denominator).mod(P)
        val x3 = lambda.multiply(lambda).subtract(TWO.multiply(point.x)).mod(P)
        val y3 = lambda.multiply(point.x.subtract(x3)).subtract(point.y).mod(P)
        return CurvePoint(x3, y3)
    }

    private fun multiply(scalar: BigInteger, point: CurvePoint): CurvePoint {
        var k = scalar.mod(N)
        var addend = point
        var result = CurvePoint.INFINITY

        while (k.signum() > 0) {
            if (k.testBit(0)) result = add(result, addend)
            addend = double(addend)
            k = k.shiftRight(1)
        }

        return result
    }

    private fun CurvePoint.toDomainPoint(): ECPoint =
        ECPoint(x = x.toHex(), y = y.toHex(), isInfinity = isInfinity)

    private fun BigInteger.toFixedBytes(size: Int): ByteArray {
        val raw = toByteArray()
        val unsigned = if (raw.size > 1 && raw.first() == 0.toByte()) raw.copyOfRange(1, raw.size) else raw
        require(unsigned.size <= size) { "Integer is too large" }
        return ByteArray(size - unsigned.size) + unsigned
    }

    private fun ByteArray.toHexString(): String =
        "0x" + joinToString("") { "%02X".format(it) }

    private inner class DeterministicNonceGenerator(
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
                if (isScalarInRange(candidate)) return candidate

                k = hmac(k, v + byteArrayOf(0x00))
                v = hmac(k, v)
            }
        }

        fun reject() {
            k = hmac(k, v + byteArrayOf(0x00))
            v = hmac(k, v)
        }

        private fun bitsToOctets(bytes: ByteArray): ByteArray =
            bitsToInt(bytes).mod(N).toFixedBytes(SCALAR_BYTES)

        private fun bitsToInt(bytes: ByteArray): BigInteger {
            val value = BigInteger(1, bytes)
            val extraBits = bytes.size * Byte.SIZE_BITS - N.bitLength()
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

    private data class CurvePoint(
        val x: BigInteger,
        val y: BigInteger,
        val isInfinity: Boolean = false,
    ) {
        companion object {
            val INFINITY = CurvePoint(BigInteger.ZERO, BigInteger.ZERO, isInfinity = true)
        }
    }

    private companion object {
        val P: BigInteger = BrainpoolP512r1.p
        val A: BigInteger = BrainpoolP512r1.a
        val B: BigInteger = BrainpoolP512r1.b
        val N: BigInteger = BrainpoolP512r1.n
        val BASE_POINT = CurvePoint(BrainpoolP512r1.gx, BrainpoolP512r1.gy)
        val ONE: BigInteger = BigInteger.ONE
        val TWO: BigInteger = BigInteger.valueOf(2)
        val THREE: BigInteger = BigInteger.valueOf(3)
        const val HEX_DIGITS = "0123456789abcdefABCDEF"
        const val CURVE_NAME = "BrainpoolP512r1"
        const val ALGORITHM = "ECDH-HKDF-SHA512-AES-256-GCM"
        const val HKDF_INFO = "CryptoApp|BrainpoolP512r1|ECDH-HKDF-SHA512-AES-256-GCM|v1"
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
