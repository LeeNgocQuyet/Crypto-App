package com.project.cryptoapp.util

import com.project.cryptoapp.domain.model.CipherText
import com.project.cryptoapp.domain.model.CryptoHistory
import com.project.cryptoapp.domain.model.DigitalSignature
import com.project.cryptoapp.domain.model.ECCurveParams
import com.project.cryptoapp.domain.model.ECPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun ECPoint.toDisplayString(): String =
    if (isInfinity) {
        "Point at infinity"
    } else {
        "x: $x\ny: $y"
    }

fun CipherText.toDisplayString(pretty: Boolean = true): String =
    CryptoPayloadCodec.encode(this, pretty)

fun DigitalSignature.toDisplayString(): String = "r: $r\ns: $s"

fun Long.toReadableDateTime(): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(this))

fun ECCurveParams.toDisplayString(): String =
    "p: $p\na: $a\nb: $b\nGx: $gx\nGy: $gy\nn: ${n.orEmpty()}"

fun List<CryptoHistory>.toPlainTextExport(): String =
    joinToString(separator = "\n\n") { item ->
        buildString {
            appendLine("id=${item.id}")
            appendLine("type=${item.operationType}")
            appendLine("status=${item.status}")
            appendLine("timestamp=${item.timestamp.toReadableDateTime()}")
            appendLine("input=${item.inputText}")
            append("output=${item.outputText}")
        }
    }

fun List<CryptoHistory>.toJsonExport(): String =
    joinToString(prefix = "[\n", postfix = "\n]", separator = ",\n") { item ->
        """
        {
          "id": ${item.id},
          "operationType": "${item.operationType}",
          "status": "${item.status}",
          "timestamp": ${item.timestamp},
          "inputText": "${item.inputText.escapeJson()}",
          "outputText": "${item.outputText.escapeJson()}"
        }
        """.trimIndent()
    }

private fun String.escapeJson(): String =
    replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
