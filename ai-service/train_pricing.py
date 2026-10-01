import pandas as pd
import numpy as np
import joblib
from xgboost import XGBRegressor

def train_and_save_model():
    print("⏳ Création des données d'entraînement (échelle EUR, réaliste)...")
    data = []

    # Génère 3000 locations fictives réalistes.
    # Le modèle prédit un prix par nuit en EUR. La logique de base reproduit les
    # prix réels du property-service (≈ 50 à 450 EUR/nuit) pour que les suggestions
    # IA soient cohérentes avec le catalogue.
    for _ in range(3000):
        bedrooms = np.random.randint(1, 6)
        bathrooms = np.random.randint(1, 5)
        lat = np.random.uniform(28, 36)     # Maroc
        lon = np.random.uniform(-12, -2)    # Maroc
        amenities_count = np.random.randint(2, 12)
        rating = np.random.uniform(3.0, 5.0)
        city_premium = np.random.uniform(1.0, 1.8)
        maxGuests = bedrooms * 2 + np.random.randint(0, 2)

        # Logique de prix (EUR/nuit) : base + contributions par attribut
        base_price = (35.0
                      + bedrooms * 42.0
                      + bathrooms * 28.0
                      + amenities_count * 4.0
                      + (rating - 3.0) * 15.0)
        price_eur = base_price * city_premium + np.random.normal(0, 18.0)
        price_eur = max(45.0, price_eur)  # Prix minimum réaliste

        data.append({
            "lat": lat,
            "lon": lon,
            "bedrooms": bedrooms,
            "bathrooms": bathrooms,
            "maxGuests": maxGuests,
            "amenities_count": amenities_count,
            "rating": rating,
            "city_premium": city_premium,
            "price_eth": price_eur,  # Libellé conservé mais maintenant en EUR
        })

    df = pd.DataFrame(data)

    X = df.drop("price_eth", axis=1)
    y = df["price_eth"]

    print("🤖 Entraînement du modèle XGBoost (EUR)...")
    model = XGBRegressor(n_estimators=200, learning_rate=0.08, max_depth=6, random_state=42)
    model.fit(X, y)

    joblib.dump(model, "pricing_model.joblib")
    print("✅ Modèle 'pricing_model.joblib' créé (échelle EUR).")

if __name__ == "__main__":
    train_and_save_model()
