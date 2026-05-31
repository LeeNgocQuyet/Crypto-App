import hashlib
import json
import time
from pathlib import Path


FIELD_BITS = 512
CURVE_ID = "server-curve-512-v1"


def to_hex(value):
    return "0x" + hex(Integer(value))[2:].upper()


def fingerprint(data):
    parts = [
        data["curveId"],
        data["p"],
        data["a"],
        data["b"],
        data["gx"],
        data["gy"],
        data["n"],
        data["h"],
    ]
    return hashlib.sha256("|".join(parts).encode("utf-8")).hexdigest().upper()


def generate_curve():
    while True:
        print("Generating 512-bit prime p...")
        p = random_prime(2**FIELD_BITS - 1, lbound=2 ** (FIELD_BITS - 1))
        field = GF(p)

        print("Trying random curve parameters...")
        a = field.random_element()
        b = field.random_element()
        if 4 * a**3 + 27 * b**2 == 0:
            continue

        curve = EllipticCurve(field, [a, b])

        print("Counting points on curve...")
        group_order = Integer(curve.cardinality())
        factors = factor(group_order)
        prime_factors = [Integer(q) for q, _ in factors if q.is_prime()]
        n = max(prime_factors)
        h = group_order // n

        if n.nbits() < 500:
            print("Reject: largest prime factor too small:", n.nbits())
            continue

        print("Searching base point G with order n...")
        for _ in range(1000):
            point = curve.random_point()
            base_point = h * point
            if base_point == curve(0):
                continue
            if n * base_point == curve(0):
                result = {
                    "curveId": CURVE_ID,
                    "fieldSize": FIELD_BITS,
                    "p": to_hex(p),
                    "a": to_hex(Integer(a)),
                    "b": to_hex(Integer(b)),
                    "gx": to_hex(Integer(base_point[0])),
                    "gy": to_hex(Integer(base_point[1])),
                    "n": to_hex(n),
                    "h": to_hex(h),
                    "createdAt": int(time.time()),
                }
                result["fingerprint"] = fingerprint(result)
                return result

        print("Reject: could not find a base point")


curve_data = generate_curve()
output_dir = Path("curves")
output_dir.mkdir(parents=True, exist_ok=True)
output_file = output_dir / "current.json"
output_file.write_text(json.dumps(curve_data, indent=2), encoding="utf-8")

print(json.dumps(curve_data, indent=2))
print("Saved to:", output_file)

