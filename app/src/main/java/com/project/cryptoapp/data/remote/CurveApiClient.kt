package com.project.cryptoapp.data.remote

import com.project.cryptoapp.domain.crypto.CurveSource
import com.project.cryptoapp.domain.crypto.ECCurveSpec
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.URL

class CurveApiClient(
    private val baseUrl: String = DEFAULT_BASE_URL,
) {
    fun fetchCurrentCurve(): CurveResponse {
        val url = URL("${baseUrl.trimEnd('/')}/api/ecc/curve/current")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
        }

        return connection.use {
            val responseCode = it.responseCode
            val body = if (responseCode in 200..299) {
                it.inputStream.bufferedReader().use { reader -> reader.readText() }
            } else {
                it.errorStream?.bufferedReader()?.use { reader -> reader.readText() }.orEmpty()
            }
            require(responseCode in 200..299) { "Curve API returned HTTP $responseCode: $body" }
            CurveResponse(body, body.toCurveSpec(CurveSource.SERVER))
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "http://10.0.2.2:8000"
        private const val TIMEOUT_MS = 5_000
    }
}

data class CurveResponse(
    val rawJson: String,
    val spec: ECCurveSpec,
)

fun String.toCurveSpec(source: CurveSource): ECCurveSpec =
    ECCurveSpec(
        id = readString("curveId"),
        fieldSize = readNumber("fieldSize"),
        p = readHex("p"),
        a = readHex("a"),
        b = readHex("b"),
        gx = readHex("gx"),
        gy = readHex("gy"),
        n = readHex("n"),
        h = readOptionalString("h")?.toBigIntegerHex() ?: BigInteger.ONE,
        fingerprint = readString("fingerprint"),
        source = source,
    )

private fun String.readString(name: String): String {
    val regex = Regex(""""${Regex.escape(name)}"\s*:\s*"((?:\\.|[^"\\])*)"""")
    val match = regex.find(this) ?: throw IllegalArgumentException("Curve JSON is missing $name")
    return match.groupValues[1].unescapeJson()
}

private fun String.readOptionalString(name: String): String? {
    val regex = Regex(""""${Regex.escape(name)}"\s*:\s*"((?:\\.|[^"\\])*)"""")
    return regex.find(this)?.groupValues?.get(1)?.unescapeJson()
}

private fun String.readNumber(name: String): Int {
    val regex = Regex(""""${Regex.escape(name)}"\s*:\s*(\d+)"""")
    val match = regex.find(this) ?: throw IllegalArgumentException("Curve JSON is missing $name")
    return match.groupValues[1].toInt()
}

private fun String.readHex(name: String): BigInteger =
    readString(name).toBigIntegerHex()

private fun String.toBigIntegerHex(): BigInteger {
    val normalized = trim().removePrefix("0x").removePrefix("0X")
    require(normalized.isNotBlank() && normalized.all { it in HEX_DIGITS }) { "Curve value must be hex" }
    return BigInteger(normalized, 16)
}

private fun String.unescapeJson(): String =
    replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\t", "\t")
        .replace("\\\"", "\"")
        .replace("\\\\", "\\")

private inline fun <T : HttpURLConnection, R> T.use(block: (T) -> R): R =
    try {
        block(this)
    } finally {
        disconnect()
    }

private const val HEX_DIGITS = "0123456789abcdefABCDEF"

