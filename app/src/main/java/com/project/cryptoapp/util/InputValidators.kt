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
    !value.contains("C1", ignoreCase = true) || !value.contains("C2", ignoreCase = true) ->
        "Ciphertext should include C1 and C2 points"
    !value.contains("0x", ignoreCase = true) -> "Ciphertext coordinates should be hex values"
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
