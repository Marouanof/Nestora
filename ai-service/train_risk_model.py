"""Entraîne le modèle de risque tenant (RandomForest).

Deux modes :

1. DONNÉES RÉELLES (recommandé dès qu'il y a du volume) :
   Exporter un CSV avec les colonnes : cancel_count, bad_reviews, label
   (label : 1 = confiant, 0 = risqué), une ligne par tenant :

       -- cancel_count : annulations faute du tenant
       SELECT tenant_id AS user_id,
              COUNT(*) FILTER (WHERE status = 'CANCELLED') AS cancel_count
         FROM bookings GROUP BY tenant_id;
       -- bad_reviews : notes 1-2/5 reçues (owners -> tenant)
       SELECT reviewee_id AS user_id,
              COUNT(*) FILTER (WHERE rating <= 2) AS bad_reviews
         FROM user_reviews GROUP BY reviewee_id;
       -- label : 0 si incident avéré (litige, no-show, impayé), sinon 1.
       -- Aujourd'hui aucun incident n'est tracé en base : le label reste à
       -- construire (table incidents ou revue manuelle), voir README.

   Puis :  python train_risk_model.py --from-csv risk_training_data.csv

2. BOOTSTRAP SIMULÉ (défaut, en attendant du volume réel) :
   1000 profils synthétiques suivant la règle métier :
   trustworthy si 40 - 10*annulations - 20*mauvais_avis > 0.
   Le modèle mémorise cette règle : il ne « sait » rien de plus.
   À remplacer par le mode 1 dès que possible.
"""
import sys

import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split

FEATURES = ["cancel_count", "bad_reviews"]


def simulated_data(n=1000, seed=42):
    rng = np.random.default_rng(seed)
    cancel_count = rng.integers(0, 10, size=n)
    bad_reviews = rng.integers(0, 5, size=n)
    score_logic = 40 - (cancel_count * 10) - (bad_reviews * 20)
    trust_label = (score_logic > 0).astype(int)
    return pd.DataFrame({
        "cancel_count": cancel_count,
        "bad_reviews": bad_reviews,
        "label": trust_label,
    })


def real_data(csv_path):
    df = pd.read_csv(csv_path, usecols=["cancel_count", "bad_reviews", "label"])
    df = df.dropna()
    df["cancel_count"] = df["cancel_count"].astype(int)
    df["bad_reviews"] = df["bad_reviews"].astype(int)
    df["label"] = df["label"].astype(int)
    return df


def train(df, source):
    X = df[FEATURES]
    y = df["label"]
    if len(df) >= 20:
        X_train, X_test, y_train, y_test = train_test_split(
            X, y, test_size=0.2, random_state=42, stratify=y)
        model = RandomForestClassifier(n_estimators=100, random_state=42)
        model.fit(X_train, y_train)
        print(f"Précision test ({source}) : {model.score(X_test, y_test):.3f} "
              f"(n={len(df)}, test={len(X_test)})")
    else:
        print(f"Volume trop faible ({len(df)} lignes) : entraînement sans test "
              f"({source}) — à ré-entraîner avec plus de volume.")
        model = RandomForestClassifier(n_estimators=100, random_state=42)
        model.fit(X, y)
    joblib.dump(model, "risk_model.joblib")
    print("Modèle 'risk_model.joblib' créé !")


def train_risk_model(csv_path=None):
    if csv_path:
        print(f"Données RÉELLES : {csv_path}")
        train(real_data(csv_path), source="réel")
    else:
        print("Aucun CSV fourni : BOOTSTRAP SIMULÉ "
              "(règle 40-10*annulations-20*avis, voir docstring).")
        train(simulated_data(), source="simulé")


if __name__ == "__main__":
    arg = sys.argv[1] if len(sys.argv) > 1 else None
    if arg in ("-h", "--help"):
        print(__doc__)
    elif arg == "--from-csv" and len(sys.argv) > 2:
        train_risk_model(sys.argv[2])
    elif arg is not None and not arg.startswith("-"):
        train_risk_model(arg)
    else:
        train_risk_model()
