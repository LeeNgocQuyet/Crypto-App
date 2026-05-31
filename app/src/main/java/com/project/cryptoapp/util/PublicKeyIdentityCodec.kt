package com.project.cryptoapp.util

import com.project.cryptoapp.domain.crypto.ActiveCurveRegistry
import com.project.cryptoapp.domain.model.DigitalSignature
import com.project.cryptoapp.domain.model.ECPoint

data class PublicKeyIdentityProof(
    val version: Int = 1,
    val type: String = "public-key-identity-proof",
    val curve: String = ActiveCurveRegistry.current.id,
    val label: String,
    val publicKey: ECPoint,
    val fingerprint: String,
    val signature: DigitalSignature,
)

object PublicKeyIdentityCodec {
    fun messageToSign(label: String, fingerprint: String): String =
        "CryptoApp public key identity|${ActiveCurveRegistry.current.id}|${label.trim()}|${fingerprint.uppercase()}"

    fun encode(proof: PublicKeyIdentityProof): String =
        """
        {
          "version": ${proof.version},
          "type": "${proof.type}",
          "curve": "${proof.curve}",
          "label": "${proof.label.escapeJson()}",
          "publicKey": {
            "x": "${proof.publicKey.x}",
            "y": "${proof.publicKey.y}"
          },
          "fingerprint": "${proof.fingerprint}",
          "signature": {
            "r": "${proof.signature.r}",
            "s": "${proof.signature.s}"
          }
        }
        """.trimIndent()

    fun decode(source: String): PublicKeyIdentityProof {
        val publicKeyBlock = readObject(source, "publicKey")
        val signatureBlock = readObject(source, "signature")
        return PublicKeyIdentityProof(
            version = readNumber(source, "version") ?: 1,
            type = readString(source, "type"),
            curve = readString(source, "curve"),
            label = readString(source, "label"),
            publicKey = ECPoint(
                x = readString(publicKeyBlock, "x"),
                y = readString(publicKeyBlock, "y"),
            ),
            fingerprint = readString(source, "fingerprint"),
            signature = DigitalSignature(
                r = readString(signatureBlock, "r"),
                s = readString(signatureBlock, "s"),
            ),
        )
    }

    private fun readString(source: String, name: String): String {
        val match = Regex(""""${Regex.escape(name)}"\s*:\s*"((?:\\.|[^"\\])*)"""")
            .find(source)
            ?: throw IllegalArgumentException("Identity proof is missing $name")
        return match.groupValues[1].unescapeJson()
    }

    private fun readNumber(source: String, name: String): Int? =
        Regex(""""${Regex.escape(name)}"\s*:\s*(\d+)"""")
            .find(source)
            ?.groupValues
            ?.get(1)
            ?.toInt()

    private fun readObject(source: String, name: String): String {
        val start = Regex(""""${Regex.escape(name)}"\s*:\s*\{""")
            .find(source)
            ?: throw IllegalArgumentException("Identity proof is missing $name")
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
        throw IllegalArgumentException("Identity proof has malformed $name")
    }

    private fun String.escapeJson(): String =
        replace("\\", "\\\\").replace("\"", "\\\"")

    private fun String.unescapeJson(): String =
        replace("\\\"", "\"").replace("\\\\", "\\")
}
