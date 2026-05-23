package com.project.cryptoapp.util

private val hexRegex = Regex("^(0x)?[0-9a-fA-F]+$")

fun validatePrivateKey(value: String): String? = when {
    value.isBlank() -> "Private key is required"
    !value.trim().matches(hexRegex) -> "Private key must be hex, for example 0x1a2b"
    value.trim().removePrefix("0x").removePrefix("0X").length < 16 -> "Private key is too short"
    else -> null
}

fun validatePublicKey(value: String): String? = when {
    value.isBlank() -> "Public key is required"
    !value.contains("x", ignoreCase = true) || !value.contains("y", ignoreCase = true) ->
        "Public key should include x and y coordinates"
    !value.contains("0x", ignoreCase = true) -> "Public key coordinates should be hex values"
    else -> null
}

fun validateCipherText(value: String): String? = when {
    value.isBlank() -> "Ciphertext is required"
    !value.contains("algorithm", ignoreCase = true) ||
        !value.contains("ephemeralPublicKey", ignoreCase = true) ||
        !value.contains("ciphertext", ignoreCase = true) ||
        !value.contains("tag", ignoreCase = true) ->
        "Ciphertext should be the hybrid JSON payload produced by Encrypt"
    !value.contains("ECDH-HKDF-SHA512-AES-256-GCM") -> "Unsupported ciphertext algorithm"
    else -> null
}

fun validateSignaturePart(label: String, value: String): String? = when {
    value.isBlank() -> "Signature $label is required"
    !value.trim().matches(hexRegex) -> "Signature $label must be hex"
    else -> null
}

fun validatePayload(label: String, value: String, encoding: String): String? = when {
    value.isBlank() -> "$label is required"
    encoding == "Hex" && !value.trim().matches(hexRegex) -> "$label must be hex because Default Encoding is Hex"
    else -> null
}
