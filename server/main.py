from pathlib import Path
import json

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel


app = FastAPI(title="ECC Curve Server")
CURVE_FILE = Path(__file__).parent / "curves" / "current.json"


class CurveResponse(BaseModel):
    curveId: str
    fieldSize: int
    p: str
    a: str
    b: str
    gx: str
    gy: str
    n: str
    h: str
    fingerprint: str


@app.get("/api/ecc/curve/current", response_model=CurveResponse)
def get_current_curve():
    if not CURVE_FILE.exists():
        raise HTTPException(status_code=404, detail="No curve file found")

    return json.loads(CURVE_FILE.read_text(encoding="utf-8"))

