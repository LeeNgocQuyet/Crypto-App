package com.project.cryptoapp.domain.crypto

import com.project.cryptoapp.domain.model.ECCurveParams
import java.math.BigInteger

data class ECCurveSpec(
    val id: String,
    val fieldSize: Int,
    val p: BigInteger,
    val a: BigInteger,
    val b: BigInteger,
    val gx: BigInteger,
    val gy: BigInteger,
    val n: BigInteger,
    val h: BigInteger = BigInteger.ONE,
    val fingerprint: String,
    val source: CurveSource,
) {
    val basePoint: CurvePointSpec = CurvePointSpec(gx, gy)

    fun toParams(): ECCurveParams =
        ECCurveParams(
            p = p.toHex(fieldSize / 4),
            a = a.toHex(fieldSize / 4),
            b = b.toHex(fieldSize / 4),
            gx = gx.toHex(fieldSize / 4),
            gy = gy.toHex(fieldSize / 4),
            n = n.toHex(fieldSize / 4),
            h = h.toHex(),
            curveId = id,
            fingerprint = fingerprint,
            source = source.label,
        )
}

data class CurvePointSpec(
    val x: BigInteger,
    val y: BigInteger,
)

enum class CurveSource(val label: String) {
    SERVER("Server"),
    CACHE("Cache"),
    FALLBACK("Fallback BrainpoolP512r1"),
}

object ActiveCurveRegistry {
    @Volatile
    var current: ECCurveSpec = BrainpoolP512r1.spec
        private set

    fun update(curve: ECCurveSpec) {
        current = curve
    }
}

