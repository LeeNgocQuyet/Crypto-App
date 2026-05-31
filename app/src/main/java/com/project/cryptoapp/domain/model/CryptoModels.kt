package com.project.cryptoapp.domain.model

data class ECPoint(
    val x: String,
    val y: String,
    val isInfinity: Boolean = false,
)

data class ECCurveParams(
    val p: String,
    val a: String,
    val b: String,
    val gx: String,
    val gy: String,
    val n: String? = null,
    val h: String? = null,
    val curveId: String? = null,
    val fingerprint: String? = null,
    val source: String? = null,
)

data class ECCKeyPair(
    val privateKey: String,
    val publicKey: ECPoint,
)

data class CipherText(
    val version: Int = 1,
    val algorithm: String,
    val curve: String,
    val curveFingerprint: String? = null,
    val ephemeralPublicKey: ECPoint,
    val salt: String,
    val nonce: String,
    val aad: String,
    val cipherText: String,
    val tag: String,
)

data class DigitalSignature(
    val r: String,
    val s: String,
)

enum class OperationType {
    ENCRYPT,
    DECRYPT,
    SIGN,
    VERIFY,
    KEY_GENERATION,
}

enum class OperationStatus {
    SUCCESS,
    FAILED,
}

data class CryptoHistory(
    val id: Long = 0,
    val operationType: OperationType,
    val inputText: String,
    val outputText: String,
    val status: OperationStatus,
    val timestamp: Long,
    val durationNanos: Long? = null,
)
