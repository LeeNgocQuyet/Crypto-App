package com.project.cryptoapp.util

import com.project.cryptoapp.domain.model.CipherText
import com.project.cryptoapp.domain.model.ECPoint

object CryptoPayloadCodec {
    fun encode(payload: CipherText, pretty: Boolean = true): String {
        val fields = listOfNotNull(
            "version" to payload.version.toString(),
            "algorithm" to payload.algorithm.quoted(),
            "curve" to payload.curve.quoted(),
            payload.curveFingerprint?.let { "curveFingerprint" to it.quoted() },
            "ephemeralPublicKey" to encodePoint(payload.ephemeralPublicKey, pretty),
            "salt" to payload.salt.quoted(),
            "nonce" to payload.nonce.quoted(),
            "aad" to payload.aad.quoted(),
            "ciphertext" to payload.cipherText.quoted(),
            "tag" to payload.tag.quoted(),
        )
        return if (pretty) {
            fields.joinToString(prefix = "{\n", postfix = "\n}", separator = ",\n") { (name, value) ->
                "  \"${name}\": ${value.prependIndentIfMultiline("  ")}"
            }
        } else {
            fields.joinToString(prefix = "{", postfix = "}", separator = ",") { (name, value) ->
                "\"$name\":${value.compactJson()}"
            }
        }
    }

    fun decode(source: String): CipherText {
        val version = readNumber(source, "version") ?: 1
        val algorithm = readString(source, "algorithm")
        val curve = readString(source, "curve")
        val ephemeralPublicKeyBlock = readObject(source, "ephemeralPublicKey")
        val ephemeralPublicKey = ECPoint(
            x = readString(ephemeralPublicKeyBlock, "x"),
            y = readString(ephemeralPublicKeyBlock, "y"),
        )
        return CipherText(
            version = version,
            algorithm = algorithm,
            curve = curve,
            curveFingerprint = readOptionalString(source, "curveFingerprint"),
            ephemeralPublicKey = ephemeralPublicKey,
            salt = readString(source, "salt"),
            nonce = readString(source, "nonce"),
            aad = readString(source, "aad"),
            cipherText = readString(source, "ciphertext"),
            tag = readString(source, "tag"),
        )
    }

    private fun encodePoint(point: ECPoint, pretty: Boolean): String =
        if (pretty) {
            "{\n    \"x\": ${point.x.quoted()},\n    \"y\": ${point.y.quoted()}\n  }"
        } else {
            "{\"x\":${point.x.quoted()},\"y\":${point.y.quoted()}}"
        }

    private fun readString(source: String, name: String): String {
        val regex = Regex(""""${Regex.escape(name)}"\s*:\s*"((?:\\.|[^"\\])*)"""")
        val match = regex.find(source) ?: throw IllegalArgumentException("Ciphertext is missing $name")
        return match.groupValues[1].unescapeJson()
    }

    private fun readOptionalString(source: String, name: String): String? {
        val regex = Regex(""""${Regex.escape(name)}"\s*:\s*"((?:\\.|[^"\\])*)"""")
        return regex.find(source)?.groupValues?.get(1)?.unescapeJson()
    }

    private fun readNumber(source: String, name: String): Int? {
        val regex = Regex(""""${Regex.escape(name)}"\s*:\s*(\d+)"""")
        return regex.find(source)?.groupValues?.get(1)?.toInt()
    }

    private fun readObject(source: String, name: String): String {
        val startRegex = Regex(""""${Regex.escape(name)}"\s*:\s*\{""")
        val start = startRegex.find(source) ?: throw IllegalArgumentException("Ciphertext is missing $name")
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
        throw IllegalArgumentException("Ciphertext has malformed $name")
    }

    private fun String.quoted(): String = "\"" + escapeJson() + "\""

    private fun String.escapeJson(): String =
        replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")

    private fun String.unescapeJson(): String =
        replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")

    private fun String.prependIndentIfMultiline(indent: String): String =
        if (contains('\n')) lines().joinToString("\n") { if (it == lines().first()) it else indent + it } else this

    private fun String.compactJson(): String = replace(Regex("""\s*\n\s*"""), "")
}
