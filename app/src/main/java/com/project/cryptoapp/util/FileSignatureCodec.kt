package com.project.cryptoapp.util

import com.project.cryptoapp.domain.crypto.ActiveCurveRegistry
import com.project.cryptoapp.domain.model.DigitalSignature

data class FileSignaturePayload(
    val version: Int = 1,
    val algorithm: String = "deterministic-ECDSA-SHA512",
    val curve: String = ActiveCurveRegistry.current.id,
    val fileName: String,
    val sha512: String,
    val signature: DigitalSignature,
)

object FileSignatureCodec {
    fun encode(payload: FileSignaturePayload): String =
        """
        {
          "version": ${payload.version},
          "algorithm": "${payload.algorithm}",
          "curve": "${payload.curve}",
          "fileName": "${payload.fileName.escapeJson()}",
          "sha512": "${payload.sha512}",
          "signature": {
            "r": "${payload.signature.r}",
            "s": "${payload.signature.s}"
          }
        }
        """.trimIndent()

    fun decode(source: String): FileSignaturePayload {
        val signatureBlock = readObject(source, "signature")
        return FileSignaturePayload(
            version = readNumber(source, "version") ?: 1,
            algorithm = readString(source, "algorithm"),
            curve = readString(source, "curve"),
            fileName = readString(source, "fileName"),
            sha512 = readString(source, "sha512"),
            signature = DigitalSignature(
                r = readString(signatureBlock, "r"),
                s = readString(signatureBlock, "s"),
            ),
        )
    }

    private fun readString(source: String, name: String): String {
        val regex = Regex(""""${Regex.escape(name)}"\s*:\s*"((?:\\.|[^"\\])*)"""")
        val match = regex.find(source) ?: throw IllegalArgumentException("Signature file is missing $name")
        return match.groupValues[1].unescapeJson()
    }

    private fun readNumber(source: String, name: String): Int? {
        val regex = Regex(""""${Regex.escape(name)}"\s*:\s*(\d+)"""")
        return regex.find(source)?.groupValues?.get(1)?.toInt()
    }

    private fun readObject(source: String, name: String): String {
        val startRegex = Regex(""""${Regex.escape(name)}"\s*:\s*\{""")
        val start = startRegex.find(source) ?: throw IllegalArgumentException("Signature file is missing $name")
        var depth = 0
        for (index in start.range.last until source.length) {
            when (source[index]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return source.substring(start.range.last, index + 1)
                }
            }
        }
        throw IllegalArgumentException("Signature file has malformed $name")
    }

    private fun String.escapeJson(): String =
        replace("\\", "\\\\").replace("\"", "\\\"")

    private fun String.unescapeJson(): String =
        replace("\\\"", "\"").replace("\\\\", "\\")
}
