package com.project.cryptoapp.util

import com.project.cryptoapp.domain.model.CipherText
import com.project.cryptoapp.domain.model.DigitalSignature
import com.project.cryptoapp.domain.model.ECPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun ECPoint.toDisplayString(): String =
    if (isInfinity) {
        "Point at infinity"
    } else {
        "Q(x=$x, y=$y)"
    }

fun CipherText.toDisplayString(): String =
    "C1=${c1.toDisplayString()}\nC2=${c2.toDisplayString()}"

fun DigitalSignature.toDisplayString(): String = "r=$r\ns=$s"

fun Long.toReadableDateTime(): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(this))
