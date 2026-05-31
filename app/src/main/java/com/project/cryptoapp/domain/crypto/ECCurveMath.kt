package com.project.cryptoapp.domain.crypto

import java.math.BigInteger

data class RuntimeCurvePoint(
    val x: BigInteger,
    val y: BigInteger,
    val isInfinity: Boolean = false,
) {
    companion object {
        val INFINITY = RuntimeCurvePoint(BigInteger.ZERO, BigInteger.ZERO, isInfinity = true)
    }
}

class ECCurveMath(private val curve: ECCurveSpec) {
    fun requirePointOnCurve(point: RuntimeCurvePoint, label: String) {
        require(!point.isInfinity) { "$label cannot be point at infinity" }
        require(point.x >= BigInteger.ZERO && point.x < curve.p && point.y >= BigInteger.ZERO && point.y < curve.p) {
            "$label coordinates must be inside Fp"
        }
        require(isOnCurve(point)) { "$label is not on ${curve.id}" }
    }

    fun isOnCurve(point: RuntimeCurvePoint): Boolean {
        if (point.isInfinity) return true
        val left = point.y.multiply(point.y).mod(curve.p)
        val right = point.x.modPow(THREE, curve.p)
            .add(curve.a.multiply(point.x))
            .add(curve.b)
            .mod(curve.p)
        return left == right
    }

    fun add(left: RuntimeCurvePoint, right: RuntimeCurvePoint): RuntimeCurvePoint {
        if (left.isInfinity) return right
        if (right.isInfinity) return left

        if (left.x == right.x) {
            if (left.y.add(right.y).mod(curve.p) == BigInteger.ZERO) return RuntimeCurvePoint.INFINITY
            return double(left)
        }

        val lambda = right.y.subtract(left.y)
            .multiply(right.x.subtract(left.x).mod(curve.p).modInverse(curve.p))
            .mod(curve.p)
        val x3 = lambda.multiply(lambda).subtract(left.x).subtract(right.x).mod(curve.p)
        val y3 = lambda.multiply(left.x.subtract(x3)).subtract(left.y).mod(curve.p)
        return RuntimeCurvePoint(x3, y3)
    }

    fun double(point: RuntimeCurvePoint): RuntimeCurvePoint {
        if (point.isInfinity || point.y == BigInteger.ZERO) return RuntimeCurvePoint.INFINITY

        val numerator = THREE.multiply(point.x.multiply(point.x)).add(curve.a).mod(curve.p)
        val denominator = TWO.multiply(point.y).mod(curve.p).modInverse(curve.p)
        val lambda = numerator.multiply(denominator).mod(curve.p)
        val x3 = lambda.multiply(lambda).subtract(TWO.multiply(point.x)).mod(curve.p)
        val y3 = lambda.multiply(point.x.subtract(x3)).subtract(point.y).mod(curve.p)
        return RuntimeCurvePoint(x3, y3)
    }

    fun multiply(scalar: BigInteger, point: RuntimeCurvePoint, reduceScalar: Boolean = true): RuntimeCurvePoint {
        var k = if (reduceScalar) scalar.mod(curve.n) else scalar
        var addend = point
        var result = RuntimeCurvePoint.INFINITY

        while (k.signum() > 0) {
            if (k.testBit(0)) result = add(result, addend)
            addend = double(addend)
            k = k.shiftRight(1)
        }

        return result
    }

    companion object {
        val TWO: BigInteger = BigInteger.valueOf(2)
        val THREE: BigInteger = BigInteger.valueOf(3)
    }
}

object ECCurveValidator {
    fun validate(curve: ECCurveSpec): ECCurveSpec {
        require(curve.fieldSize == 512) { "Curve fieldSize must be 512" }
        require(curve.p.bitLength() == 512) { "Curve p must be a 512-bit prime" }
        require(curve.p.isProbablePrime(80)) { "Curve p must be prime" }
        require(curve.n.bitLength() >= 500) { "Curve n must be at least 500 bits" }
        require(curve.n.isProbablePrime(80)) { "Curve n must be prime" }
        require(curve.h >= BigInteger.ONE) { "Curve cofactor h must be positive" }
        require(isFieldElement(curve.a, curve.p)) { "Curve a must be inside Fp" }
        require(isFieldElement(curve.b, curve.p)) { "Curve b must be inside Fp" }
        val discriminant = BigInteger.valueOf(4).multiply(curve.a.modPow(ECCurveMath.THREE, curve.p))
            .add(BigInteger.valueOf(27).multiply(curve.b.modPow(ECCurveMath.TWO, curve.p)))
            .mod(curve.p)
        require(discriminant != BigInteger.ZERO) { "Curve is singular" }

        val math = ECCurveMath(curve)
        val basePoint = RuntimeCurvePoint(curve.gx, curve.gy)
        math.requirePointOnCurve(basePoint, "Base point")
        require(math.multiply(curve.n, basePoint, reduceScalar = false).isInfinity) { "Base point order check failed" }
        return curve
    }

    private fun isFieldElement(value: BigInteger, p: BigInteger): Boolean =
        value >= BigInteger.ZERO && value < p
}

