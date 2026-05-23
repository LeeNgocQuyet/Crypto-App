package com.project.cryptoapp.util

import com.project.cryptoapp.domain.model.ECPoint
import java.security.MessageDigest

data class KeyPayload(
    val version: Int = 1,
    val type: String,
    val curve: String = "BrainpoolP512r1",
    val publicKey: ECPoint,
    val privateKey: String? = null,
    val fingerprint: String,
)

object KeyPayloadCodec {
    fun publicKeyPayload(publicKey: String): KeyPayload {
        val point = parsePoint(publicKey)
        return KeyPayload(
            type = "public",
            publicKey = point,
            fingerprint = fingerprint(point),
        )
    }

    fun privateKeyPayload(privateKey: String, publicKey: String): KeyPayload {
        val point = parsePoint(publicKey)
        return KeyPayload(
            type = "private",
            publicKey = point,
            privateKey = privateKey,
            fingerprint = fingerprint(point),
        )
    }

    fun encode(payload: KeyPayload, pretty: Boolean = true): String {
        val privateKeyLine = payload.privateKey?.let { ",\n  \"privateKey\": \"${it.escapeJson()}\"" }.orEmpty()
        val json = """
        {
          "version": ${payload.version},
          "type": "${payload.type}",
          "curve": "${payload.curve}",
          "publicKey": {
            "x": "${payload.publicKey.x}",
            "y": "${payload.publicKey.y}"
          }$privateKeyLine,
          "fingerprint": "${payload.fingerprint}"
        }
        """.trimIndent()
        return if (pretty) json else json.replace(Regex("""\s*\n\s*"""), "")
    }

    fun decode(source: String): KeyPayload {
        val pointBlock = readObject(source, "publicKey")
        val point = ECPoint(
            x = readString(pointBlock, "x"),
            y = readString(pointBlock, "y"),
        )
        return KeyPayload(
            version = readNumber(source, "version") ?: 1,
            type = readString(source, "type"),
            curve = readString(source, "curve"),
            publicKey = point,
            privateKey = readOptionalString(source, "privateKey"),
            fingerprint = readString(source, "fingerprint"),
        )
    }

    fun ECPoint.toDisplayKey(): String = "x: $x\ny: $y"

    private fun parsePoint(publicKey: String): ECPoint {
        fun read(label: String): String {
            val regex = Regex("""(?im)^\s*${Regex.escape(label)}\s*:\s*((?:0x)?[0-9a-fA-F]+)\s*$""")
            return regex.find(publicKey)?.groupValues?.get(1)
                ?: throw IllegalArgumentException("Public key is missing $label")
        }
        return ECPoint(x = read("x"), y = read("y"))
    }

    private fun fingerprint(point: ECPoint): String {
        val bytes = "${point.x}|${point.y}".toByteArray(Charsets.UTF_8)
        return "0x" + MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02X".format(it) }
    }

    private fun readString(source: String, name: String): String {
        val match = Regex(""""${Regex.escape(name)}"\s*:\s*"((?:\\.|[^"\\])*)"""")
            .find(source)
            ?: throw IllegalArgumentException("Key payload is missing $name")
        return match.groupValues[1].unescapeJson()
    }

    private fun readOptionalString(source: String, name: String): String? =
        Regex(""""${Regex.escape(name)}"\s*:\s*"((?:\\.|[^"\\])*)"""")
            .find(source)
            ?.groupValues
            ?.get(1)
            ?.unescapeJson()

    private fun readNumber(source: String, name: String): Int? =
        Regex(""""${Regex.escape(name)}"\s*:\s*(\d+)"""")
            .find(source)
            ?.groupValues
            ?.get(1)
            ?.toInt()

    private fun readObject(source: String, name: String): String {
        val start = Regex(""""${Regex.escape(name)}"\s*:\s*\{""")
            .find(source)
            ?: throw IllegalArgumentException("Key payload is missing $name")
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
        throw IllegalArgumentException("Key payload has malformed $name")
    }

    private fun String.escapeJson(): String =
        replace("\\", "\\\\").replace("\"", "\\\"")

    private fun String.unescapeJson(): String =
        replace("\\\"", "\"").replace("\\\\", "\\")
}
