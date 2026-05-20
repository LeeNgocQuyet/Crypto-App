package com.project.cryptoapp.data.repository

import com.project.cryptoapp.domain.crypto.BrainpoolP512r1
import com.project.cryptoapp.domain.crypto.ECCryptoService
import com.project.cryptoapp.domain.crypto.toHex
import com.project.cryptoapp.domain.model.CipherText
import com.project.cryptoapp.domain.model.DigitalSignature
import com.project.cryptoapp.domain.model.ECCKeyPair
import com.project.cryptoapp.domain.model.ECPoint
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom

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

    override suspend fun encrypt(plaintext: String, publicKey: String): CipherText {
        val recipientPublicKey = parsePublicKey(publicKey)
        requirePointOnCurve(recipientPublicKey, "Public key")

        val messagePoint = encodeMessagePoint(decodeInputPayload(plaintext))
        val ephemeralPrivateKey = randomScalar()
        val c1 = multiply(ephemeralPrivateKey, BASE_POINT)
        val sharedSecret = multiply(ephemeralPrivateKey, recipientPublicKey)
        val c2 = add(messagePoint, sharedSecret)

        return CipherText(
            c1 = c1.toDomainPoint(),
            c2 = c2.toDomainPoint(),
        )
    }

    override suspend fun decrypt(cipherText: String, privateKey: String): String {
        val secret = parsePrivateKey(privateKey)
        val (c1, c2) = parseCipherText(cipherText)
        requirePointOnCurve(c1, "C1")
        requirePointOnCurve(c2, "C2")

        val sharedSecret = multiply(secret, c1)
        val messagePoint = add(c2, negate(sharedSecret))
        val payload = decodeMessagePoint(messagePoint)
        return encodeOutputPayload(payload)
    }

    override suspend fun sign(message: String, privateKey: String): DigitalSignature {
        val secret = parsePrivateKey(privateKey)
        val digest = hashToScalar(message)

        while (true) {
            val k = randomScalar()
            val r = multiply(k, BASE_POINT).x.mod(N)
            if (r == BigInteger.ZERO) continue

            val s = k.modInverse(N)
                .multiply(digest.add(r.multiply(secret)))
                .mod(N)
            if (s != BigInteger.ZERO) {
                return DigitalSignature(r = r.toHex(), s = s.toHex())
            }
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

    private fun parseCipherText(value: String): Pair<CurvePoint, CurvePoint> {
        val c1x = readLabeledHex(value, "C1.x") ?: throw IllegalArgumentException("Ciphertext is missing C1.x")
        val c1y = readLabeledHex(value, "C1.y") ?: throw IllegalArgumentException("Ciphertext is missing C1.y")
        val c2x = readLabeledHex(value, "C2.x") ?: throw IllegalArgumentException("Ciphertext is missing C2.x")
        val c2y = readLabeledHex(value, "C2.y") ?: throw IllegalArgumentException("Ciphertext is missing C2.y")
        return CurvePoint(c1x, c1y) to CurvePoint(c2x, c2y)
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

    private fun encodeMessagePoint(payload: ByteArray): CurvePoint {
        require(payload.isNotEmpty()) { "Plaintext is required" }
        require(payload.size <= MAX_PAYLOAD_BYTES) {
            "Plaintext is too long for one ECC block. Maximum is $MAX_PAYLOAD_BYTES bytes."
        }

        val encoded = ByteBuffer.allocate(payload.size + 1)
            .put(payload.size.toByte())
            .put(payload)
            .array()
        val baseX = BigInteger(1, encoded).multiply(MAPPING_FACTOR)

        repeat(MAPPING_FACTOR.toInt()) { offset ->
            val x = baseX.add(BigInteger.valueOf(offset.toLong()))
            if (x >= P) return@repeat
            val rhs = x.modPow(THREE, P)
                .add(A.multiply(x))
                .add(B)
                .mod(P)
            val y = sqrtModP(rhs) ?: return@repeat
            return CurvePoint(x, y)
        }

        throw IllegalArgumentException("Unable to map plaintext to a curve point")
    }

    private fun decodeMessagePoint(point: CurvePoint): ByteArray {
        requirePointOnCurve(point, "Decoded message point")
        val messageInteger = point.x.divide(MAPPING_FACTOR)
        val bytes = messageInteger.toByteArray().dropWhile { it == 0.toByte() }.toByteArray()
        require(bytes.isNotEmpty()) { "Decoded message is empty" }

        val length = bytes.first().toInt() and 0xFF
        require(length in 1..MAX_PAYLOAD_BYTES) { "Decoded message length is invalid" }
        require(bytes.size == length + 1) { "Decoded message block is malformed" }
        return bytes.copyOfRange(1, bytes.size)
    }

    private fun decodeInputPayload(value: String): ByteArray {
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
        val digest = MessageDigest.getInstance("SHA-512")
            .digest(decodeInputPayload(message))
        return BigInteger(1, digest).mod(N)
    }

    private fun randomScalar(): BigInteger {
        while (true) {
            val candidate = BigInteger(N.bitLength(), secureRandom).mod(N)
            if (candidate != BigInteger.ZERO) return candidate
        }
    }

    private fun sqrtModP(value: BigInteger): BigInteger? {
        val root = value.modPow(P.add(ONE).shiftRight(2), P)
        return if (root.multiply(root).mod(P) == value.mod(P)) root else null
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

    private fun negate(point: CurvePoint): CurvePoint =
        if (point.isInfinity) point else CurvePoint(point.x, P.subtract(point.y).mod(P))

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
        val MAPPING_FACTOR: BigInteger = BigInteger.valueOf(256)
        const val MAX_PAYLOAD_BYTES = 62
        const val HEX_DIGITS = "0123456789abcdefABCDEF"
        val PRINTABLE_CONTROLS = setOf('\n', '\r', '\t')
    }
}
