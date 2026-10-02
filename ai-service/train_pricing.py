"""Entraîne le modèle de pricing sur de VRAIES annonces Airbnb Maroc.

Source : DeRent5/ai-service data/morocco_listings_full.csv (65k annonces,
prix/nuit en MAD). Le script télécharge le CSV s'il est absent en local.

Usage :
    python train_pricing.py [chemin_csv]
"""
import os
import sys
import urllib.request

import joblib
import numpy as np
import pandas as pd
from analytics_engine import normalize_city
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sklearn.model_selection import GroupShuffleSplit
from xgboost import XGBRegressor

CSV_URL = ("https://raw.githubusercontent.com/DeRent5/ai-service/main"
           "/data/morocco_listings_full.csv")
LOCAL_CSV = os.path.join(os.path.dirname(__file__), "data", "morocco_listings_full.csv")

FEATURES = ["lat", "lon", "bedrooms", "beds", "guests",
            "rating", "reviews", "images", "city_code", "month"]
TARGET = "nightly_price"


def ensure_csv(path_arg=None):
    path = path_arg or LOCAL_CSV
    if os.path.exists(path):
        print(f"CSV local : {path}")
        return path
    os.makedirs(os.path.dirname(path), exist_ok=True)
    print(f"Téléchargement {CSV_URL} ...")
    urllib.request.urlretrieve(CSV_URL, path)
    print(f"CSV sauvegardé : {path}")
    return path


def train_and_save_model(csv_path=None):
    csv_path = ensure_csv(csv_path)
    df = pd.read_csv(csv_path, usecols=["room_id", "city", "nightly_price", "check_in",
                                        "latitude", "longitude", "bedroom_count",
                                        "bed_count", "rating_value", "rating_count",
                                        "image_count"])
    # Garde-fou : prix strictement positifs (le dataset actuel n'a aucun
    # prix <= 0 ni >= 20000, ce filtre ne retire rien mais protège des
    # erreurs de scrape futures).
    df = df[df["nightly_price"] > 0].copy()
    df["month"] = pd.to_datetime(df["check_in"]).dt.month

    # Normalisation des villes (accents/casse/alias : Tangier->tanger...) :
    # sans elle, 'Meknes' et 'Meknès' apprennent deux city_code différents.
    df["city"] = df["city"].map(normalize_city)
    cities = sorted(df["city"].dropna().unique().tolist())
    city_code = {c: i for i, c in enumerate(cities)}

    work = pd.DataFrame({
        "lat": df["latitude"].fillna(33.0),
        "lon": df["longitude"].fillna(-7.0),
        "bedrooms": df["bedroom_count"].fillna(1).clip(0, 10).astype(int),
        "beds": df["bed_count"].fillna(2).clip(1, 16).astype(int),
        # NOTE : le CSV n'a pas de colonne voyageurs ; on approxime la
        # capacité par le nombre de lits (beds). À remplacer par une vraie
        # colonne guests/maxGuests si le dataset l'ajoute un jour.
        "guests": df["bed_count"].fillna(2).clip(1, 16).astype(int),
        "rating": df["rating_value"].fillna(4.5),
        "reviews": df["rating_count"].fillna(0).astype(int),
        "images": df["image_count"].fillna(10).astype(int),
        "city_code": df["city"].map(city_code).fillna(-1).astype(int),
        "month": df["month"].fillna(6).astype(int),
    })
    y = df["nightly_price"].astype(float)

    defaults = {
        "rating": 4.5,
        "reviews": int(work["reviews"].median()),
        "images": int(work["images"].median()),
    }

    # Split PAR ANNONCE (room_id), pas par ligne : le dataset est un panel
    # (une annonce x plusieurs dates). Un split aléatoire mettrait les mêmes
    # biens en train ET en test (fuite -> métriques gonflées).
    splitter = GroupShuffleSplit(n_splits=1, test_size=0.2, random_state=42)
    train_idx, test_idx = next(splitter.split(work, y, groups=df["room_id"]))
    X_train, X_test = work.iloc[train_idx], work.iloc[test_idx]
    y_train, y_test = y.iloc[train_idx], y.iloc[test_idx]
    model = XGBRegressor(n_estimators=300, learning_rate=0.06,
                         max_depth=7, random_state=42, n_jobs=-1)
    model.fit(X_train, y_train)
    pred = model.predict(X_test)
    print(f"MAE={mean_absolute_error(y_test, pred):.2f} MAD | "
          f"RMSE={np.sqrt(mean_squared_error(y_test, pred)):.2f} | "
          f"R2={r2_score(y_test, pred):.4f} (n={len(work)})")

    bundle = {"model": model, "features": FEATURES,
              "cities": cities, "defaults": defaults}
    out = os.path.join(os.path.dirname(__file__), "pricing_model.joblib")
    joblib.dump(bundle, out)
    print(f"Modèle sauvegardé : {out}")


if __name__ == "__main__":
    train_and_save_model(sys.argv[1] if len(sys.argv) > 1 else None)
