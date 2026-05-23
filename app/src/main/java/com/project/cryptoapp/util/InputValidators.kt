package com.project.cryptoapp.util

import com.project.cryptoapp.domain.crypto.BrainpoolP512r1
import java.math.BigInteger

private val hexRegex = Regex("^(0x)?[0-9a-fA-F]+$")
private val labeledHexRegex = Regex("""(?im)^\s*%s\s*:\s*((?:0x)?[0-9a-fA-F]+)\s*$""")
private const val HYBRID_ALGORITHM = "ECDH-HKDF-SHA512-AES-256-GCM"
private const val CURVE_NAME = "BrainpoolP512r1"
private const val FIELD_HEX_LENGTH = 128
private const val SALT_BYTES = 32
private const val NONCE_BYTES = 12
private const val TAG_BYTES = 16

fun validatePrivateKey(value: String): String? = when {
    value.isBlank() -> "Private key is required"
    !value.trim().matches(hexRegex) -> "Private key must be hex, for example 0x1a2b"
    !isScalarInRange(value) -> "Private key must be in range [1, n - 1] for BrainpoolP512r1"
    else -> null
}

fun validatePublicKey(value: String): String? {
    if (value.isBlank()) return "Public key is required"
    val x = readLabeledHex(value, "x") ?: return "Public key should include x coordinate"
    val y = readLabeledHex(value, "y") ?: return "Public key should include y coordinate"
    return validateFieldElement("Public key x", x)
        ?: validateFieldElement("Public key y", y)
}

fun validateCipherText(value: String): String? {
    if (value.isBlank()) return "Ciphertext is required"
    val payload = runCatching { CryptoPayloadCodec.decode(value) }
        .getOrElse { return it.message ?: "Ciphertext should be hybrid JSON from Encrypt" }

    return when {
        payload.version != 1 -> "Unsupported ciphertext version: ${payload.version}"
        payload.algorithm != HYBRID_ALGORITHM -> "Unsupported ciphertext algorithm: ${payload.algorithm}"
        payload.curve != CURVE_NAME -> "Unsupported ciphertext curve: ${payload.curve}"
        else -> validateFieldElement("Ephemeral public key x", payload.ephemeralPublicKey.x)
            ?: validateFieldElement("Ephemeral public key y", payload.ephemeralPublicKey.y)
            ?: validateByteHex("salt", payload.salt, SALT_BYTES)
            ?: validateByteHex("nonce", payload.nonce, NONCE_BYTES)
            ?: validateByteHex("aad", payload.aad, expectedBytes = null, allowEmpty = true)
            ?: validateByteHex("ciphertext", payload.cipherText, expectedBytes = null)
            ?: validateByteHex("tag", payload.tag, TAG_BYTES)
    }
}

fun validateSignaturePart(label: String, value: String): String? = when {
    value.isBlank() -> "Signature $label is required"
    !value.trim().matches(hexRegex) -> "Signature $label must be hex"
    !isScalarInRange(value) -> "Signature $label must be in range [1, n - 1]"
    else -> null
}

fun validatePayload(label: String, value: String, encoding: String): String? = when {
    value.isBlank() -> "$label is required"
    encoding == "Hex" && !value.trim().matches(hexRegex) -> "$label must be hex because Default Encoding is Hex"
    encoding == "Hex" && value.normalizedHex().length % 2 != 0 -> "$label hex must contain complete bytes"
    else -> null
}

fun validateAad(value: String, encoding: String): String? = when {
    value.isBlank() -> null
    encoding == "Hex" && !value.trim().matches(hexRegex) -> "AAD must be hex because Default Encoding is Hex"
    encoding == "Hex" && value.normalizedHex().length % 2 != 0 -> "AAD hex must contain complete bytes"
    else -> null
}

private fun readLabeledHex(source: String, label: String): String? =
    Regex(labeledHexRegex.pattern.format(Regex.escape(label)), RegexOption.IGNORE_CASE)
        .find(source)
        ?.groupValues
        ?.get(1)

private fun validateFieldElement(label: String, value: String): String? {
    val normalized = value.normalizedHex()
    return when {
        normalized.isBlank() || !value.trim().matches(hexRegex) -> "$label must be hex"
        normalized.length > FIELD_HEX_LENGTH -> "$label must be at most $FIELD_HEX_LENGTH hex characters"
        BigInteger(normalized, 16) >= BrainpoolP512r1.p -> "$label must be inside Fp"
        else -> null
    }
}

private fun validateByteHex(
    label: String,
    value: String,
    expectedBytes: Int?,
    allowEmpty: Boolean = false,
): String? {
    val normalized = value.normalizedHex()
    return when {
        normalized.isEmpty() && allowEmpty -> null
        normalized.isBlank() -> "$label is required"
        !value.trim().matches(hexRegex) -> "$label must be hex"
        normalized.length % 2 != 0 -> "$label hex must contain complete bytes"
        expectedBytes != null && normalized.length != expectedBytes * 2 ->
            "$label must be $expectedBytes bytes"
        else -> null
    }
}

private fun isScalarInRange(value: String): Boolean {
    val normalized = value.normalizedHex()
    if (normalized.isBlank() || !value.trim().matches(hexRegex)) return false
    val scalar = BigInteger(normalized, 16)
    return scalar >= BigInteger.ONE && scalar < BrainpoolP512r1.n
}

private fun String.normalizedHex(): String =
    trim().removePrefix("0x").removePrefix("0X")
