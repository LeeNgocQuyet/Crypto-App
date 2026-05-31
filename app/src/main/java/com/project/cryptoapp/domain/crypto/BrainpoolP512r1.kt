package com.project.cryptoapp.domain.crypto

import com.project.cryptoapp.domain.model.ECCurveParams
import java.math.BigInteger

object BrainpoolP512r1 {
    const val CURVE_ID = "BrainpoolP512r1"
    const val FINGERPRINT = "0xBRAINPOOL-P512R1-RFC5639"

    val p: BigInteger = hex(
        "AADD9DB8DBE9C48B3FD4E6AE33C9FC07CB308DB3B3C9D20E" +
            "D6639CCA703308717D4D9B009BC66842AECDA12AE6A380E" +
            "62881FF2F2D82C68528AA6056583A48F3",
    )
    val a: BigInteger = hex(
        "7830A3318B603B89E2327145AC234CC594CBDD8D3DF91610" +
            "A83441CAEA9863BC2DED5D5AA8253AA10A2EF1C98B9AC8" +
            "B57F1117A72BF2C7B9E7C1AC4D77FC94CA",
    )
    val b: BigInteger = hex(
        "3DF91610A83441CAEA9863BC2DED5D5AA8253AA10A2EF1C" +
            "98B9AC8B57F1117A72BF2C7B9E7C1AC4D77FC94CADC083" +
            "E67984050B75EBAE5DD2809BD638016F723",
    )
    val gx: BigInteger = hex(
        "81AEE4BDD82ED9645A21322E9C4C6A9385ED9F70B5D916C" +
            "1B43B62EEF4D0098EFF3B1F78E2D0D48D50D1687B93B97" +
            "D5F7C6D5047406A5E688B352209BCB9F822",
    )
    val gy: BigInteger = hex(
        "7DDE385D566332ECC0EABFA9CF7822FDF209F70024A57B" +
            "1AA000C55B881F8111B2DCDE494A5F485E5BCA4BD88A27" +
            "63AED1CA2B2FA8F0540678CD1E0F3AD80892",
    )
    val n: BigInteger = hex(
        "AADD9DB8DBE9C48B3FD4E6AE33C9FC07CB308DB3B3C9D20E" +
            "D6639CCA70330870553E5C414CA92619418661197FAC104" +
            "71DB1D381085DDADDB58796829CA90069",
    )

    val params = ECCurveParams(
        p = p.toHex(),
        a = a.toHex(),
        b = b.toHex(),
        gx = gx.toHex(),
        gy = gy.toHex(),
        n = n.toHex(),
        h = BigInteger.ONE.toHex(),
        curveId = CURVE_ID,
        fingerprint = FINGERPRINT,
        source = CurveSource.FALLBACK.label,
    )

    val spec = ECCurveSpec(
        id = CURVE_ID,
        fieldSize = 512,
        p = p,
        a = a,
        b = b,
        gx = gx,
        gy = gy,
        n = n,
        h = BigInteger.ONE,
        fingerprint = FINGERPRINT,
        source = CurveSource.FALLBACK,
    )

    private fun hex(value: String): BigInteger = BigInteger(value, 16)
}

fun BigInteger.toHex(width: Int = 128): String =
    "0x" + toString(16).padStart(width, '0').uppercase()
