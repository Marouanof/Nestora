"""Calibre l'intervalle d'ancrage du pricing (np.clip ratio, main.py).

Reproduit EXACTEMENT le feature engineering de train_pricing.py, charge
pricing_model.joblib, prédit chaque annonce et analyse la distribution de
ratio = prix_reel / prediction.

Usage:
    venv/Scripts/python.exe calibrate_anchor.py [chemin_csv]
Sortie:
    data/anchor_calibration.json
"""
import json
import os
import sys

import joblib
import numpy as np
import pandas as pd

from analytics_engine import normalize_city

BASE = os.path.dirname(os.path.abspath(__file__))
LOCAL_CSV = os.path.join(BASE, "data", "morocco_listings_full.csv")
MODEL_PATH = os.path.join(BASE, "pricing_model.joblib")
OUT_PATH = os.path.join(BASE, "data", "anchor_calibration.json")

CURRENT_LO, CURRENT_HI = 0.7, 2.2


def build_features(df, cities):
    city_code = {c: i for i, c in enumerate(cities)}
    norm_cities = df["city"].map(normalize_city)
    work = pd.DataFrame({
        "lat": df["latitude"].fillna(33.0),
        "lon": df["longitude"].fillna(-7.0),
        "bedrooms": df["bedroom_count"].fillna(1).clip(0, 10).astype(int),
        "beds": df["bed_count"].fillna(2).clip(1, 16).astype(int),
        "guests": df["bed_count"].fillna(2).clip(1, 16).astype(int),
        "rating": df["rating_value"].fillna(4.5),
        "reviews": df["rating_count"].fillna(0).astype(int),
        "images": df["image_count"].fillna(10).astype(int),
        "city_code": norm_cities.map(city_code).fillna(-1).astype(int),
        "month": pd.to_datetime(df["check_in"]).dt.month.fillna(6).astype(int),
    })
    return work


def main(csv_path=None):
    csv_path = csv_path or LOCAL_CSV
    df = pd.read_csv(csv_path, usecols=["city", "nightly_price", "check_in",
                                        "latitude", "longitude", "bedroom_count",
                                        "bed_count", "rating_value", "rating_count",
                                        "image_count"])
    # Même filtre que l'entraînement
    df = df[(df["nightly_price"] > 0) & (df["nightly_price"] < 20000)].copy()
    print(f"Annonces utilisées : {len(df)}")

    bundle = joblib.load(MODEL_PATH)
    model = bundle.get("model") if isinstance(bundle, dict) else bundle
    features = bundle.get("features") if isinstance(bundle, dict) else None
    cities = bundle.get("cities", []) if isinstance(bundle, dict) else []

    X = build_features(df, cities)
    if features:
        X = X[features]
    pred = np.asarray(model.predict(X), dtype=float)
    real = df["nightly_price"].astype(float).to_numpy()

    valid = (pred > 0) & np.isfinite(pred)
    print(f"Prédictions valides : {valid.sum()} / {len(pred)}")
    ratio = real[valid] / pred[valid]

    pct = [1, 5, 10, 25, 50, 75, 90, 95, 99]
    vals = {f"p{p}": round(float(np.percentile(ratio, p)), 4) for p in pct}
    stats = {
        "n": int(valid.sum()),
        "mean": round(float(ratio.mean()), 4),
        "std": round(float(ratio.std()), 4),
        "min": round(float(ratio.min()), 4),
        "max": round(float(ratio.max()), 4),
        "percentiles": vals,
        "part_bornee_bas": round(float((ratio < CURRENT_LO).mean()), 4),
        "part_bornee_haut": round(float((ratio > CURRENT_HI).mean()), 4),
        "bornes_actuelles": [CURRENT_LO, CURRENT_HI],
    }
    with open(OUT_PATH, "w", encoding="utf-8") as f:
        json.dump(stats, f, ensure_ascii=False, indent=2)

    print(json.dumps(stats, ensure_ascii=False, indent=2))
    print(f"Sauvegardé : {OUT_PATH}")


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else None)
