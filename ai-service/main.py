from fastapi import FastAPI, HTTPException, Query
import joblib
import pandas as pd
import numpy as np
import uvicorn
from datetime import datetime
import os
from sklearn.cluster import KMeans
from sklearn.metrics.pairwise import cosine_similarity
from sklearn.preprocessing import StandardScaler
from typing import List, Optional
import analytics_engine  # Import the new module
from dotenv import load_dotenv
import property_client

# Load environment variables
load_dotenv()

app = FastAPI(title="AI Pricing & Recommendation & Analytics Service")

MODEL_PATH = os.getenv("PRICING_MODEL_PATH", "pricing_model.joblib")
RISK_MODEL_PATH = os.getenv("RISK_MODEL_PATH", "risk_model.joblib")

MODEL = None
RISK_MODEL = None
MODEL_FEATURES = ["lat", "lon", "bedrooms", "beds", "guests",
                  "rating", "reviews", "images", "city_code", "month"]
MODEL_CITIES: list = []
MODEL_DEFAULTS = {"rating": 4.5, "reviews": 0, "images": 10}

# --- REAL PROPERTY DATA (fetched from property-service) ---
# Les vraies propriétés actives proviennent désormais du property-service.
# pricePerNight est en MAD. Le client retombe sur des données de secours si le
# service est injoignable (le démarrage ne doit jamais échouer).
PROPERTIES = property_client.fetch_properties()
PROPERTY_BY_ID = {p["id"]: p for p in PROPERTIES}

print(f"✅ Catégorie chargée : {len(PROPERTIES)} vraies propriétés depuis le property-service")

# --- ML MODELS INITIALIZATION ---
scaler = StandardScaler()
kmeans = KMeans(n_clusters=3, random_state=42)
df_properties = pd.DataFrame(PROPERTIES)

# Type -> code numérique pour le clustering
df_properties['type_code'] = df_properties['type'].astype('category').cat.codes
features_for_clustering = df_properties[['pricePerNight', 'bedrooms', 'type_code']]
scaled_features = scaler.fit_transform(features_for_clustering)

# Fit K-Means
df_properties['cluster'] = kmeans.fit_predict(scaled_features)

print("✅ K-Means Clustering complete. Clusters created on real properties.")


def load_models():
    global MODEL, RISK_MODEL, MODEL_FEATURES, MODEL_CITIES, MODEL_DEFAULTS
    if os.path.exists(MODEL_PATH):
        bundle = joblib.load(MODEL_PATH)
        if hasattr(bundle, "predict"):
            MODEL = bundle  # ancien format : régresseur seul
        else:
            MODEL = bundle.get("model")
            MODEL_FEATURES = bundle.get("features", MODEL_FEATURES)
            MODEL_CITIES = bundle.get("cities", [])
            MODEL_DEFAULTS = bundle.get("defaults", MODEL_DEFAULTS)
        print("✅ Modèle Pricing chargé")
    if os.path.exists(RISK_MODEL_PATH):
        RISK_MODEL = joblib.load(RISK_MODEL_PATH)
        print("✅ Modèle Risk chargé")


load_models()


@app.get("/api/v1/recommendations")
async def get_recommendations(user_budget: float = Query(..., gt=0)):
    """
    Retourne les IDs des propriétés réelles recommandées en fonction du budget
    utilisateur (EUR), via similarité cosinus sur (prix/bedrooms/type).
    """
    try:
        # Vecteur utilisateur : (budget, 2 chambres moyennes, type moyen 0)
        user_vector = np.array([[user_budget, 2, 0]])
        user_vector_scaled = scaler.transform(user_vector)

        similarities = cosine_similarity(user_vector_scaled, scaled_features)

        df = df_properties.copy()
        df['similarity'] = similarities[0]

        # On recommande toutes les vraies propriétés, triées par similarité
        recs = df.sort_values(by='similarity', ascending=False)

        recommended_ids = recs['id'].astype(int).tolist()

        return {
            "user_budget": user_budget,
            "recommended_property_ids": recommended_ids,
            "debug_scores": recs[['id', 'city', 'pricePerNight', 'similarity']].to_dict(orient='records')
        }

    except Exception as e:
        print(f"Erreur Recommandation: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/v1/pricing/suggest")
async def get_suggested_price(property_id: int, date: str):
    """
    Endpoint appelé par le property-service.
    Modèle XGBoost entraîné sur 65k vraies annonces Airbnb Maroc (MAD/nuit) :
    ville, mois, chambres, lits, notes... puis ancrage autour du prix réel
    du listing pour rester cohérent avec le catalogue.
    """
    if MODEL is None:
        raise HTTPException(status_code=500, detail="Modèle IA non chargé sur le serveur")

    try:
        prop = PROPERTY_BY_ID.get(property_id)

        # Analyse de la date
        dt = datetime.strptime(date, "%Y-%m-%d")
        month = dt.month
        is_weekend = dt.weekday() >= 4  # Vendredi, Samedi, Dimanche

        if prop is not None:
            lat = prop["lat"] or 33.0
            lon = prop["lon"] or -7.0
            rooms = prop["bedrooms"] or 1
            guests = prop["maxGuests"] or 2
            beds = max(1, round(guests / 2))
            city = (prop.get("city") or "").strip()
            real_price = prop["pricePerNight"]
        else:
            # Propriété inconnue du catalogue : valeurs par défaut
            lat, lon, rooms, guests = 33.0, -7.0, 1, 2
            beds, city = 1, ""
            real_price = None

        # 1. Prédiction du modèle (effets ville/mois/chambres appris sur données réelles)
        try:
            city_code = MODEL_CITIES.index(city)
        except ValueError:
            city_code = -1
        base_features = pd.DataFrame([{
            "lat": lat,
            "lon": lon,
            "bedrooms": rooms,
            "beds": beds,
            "guests": guests,
            "rating": MODEL_DEFAULTS.get("rating", 4.5),
            "reviews": MODEL_DEFAULTS.get("reviews", 0),
            "images": MODEL_DEFAULTS.get("images", 10),
            "city_code": city_code,
            "month": month,
        }])
        base_features = base_features[MODEL_FEATURES]
        base_prediction = float(MODEL.predict(base_features)[0])

        # 2. Ancrage autour du prix réel du listing (le modèle donne la
        # justesse relative, le listing donne le niveau)
        anchor_source = "model"
        if real_price and base_prediction > 0:
            city_premium = float(np.clip(real_price / base_prediction, 0.7, 2.2))
            anchor_source = "listing"
        else:
            # Repli : médiane réelle de la ville (référentiel DeRent5)
            city_premium = 1.0
            try:
                ref = analytics_engine.load_market_referential().get(city) or {}
                if ref.get("median") and base_prediction > 0:
                    city_premium = float(np.clip(ref["median"] / base_prediction, 0.7, 2.2))
                    anchor_source = "city-median"
            except Exception:
                pass

        final_price = base_prediction * city_premium

        return {
            "propertyId": property_id,
            "property_id": property_id,
            "suggested_price_mad": round(float(final_price), 2),
            "currency": "MAD",
            "details": {
                "base_ai_price": round(float(base_prediction), 2),
                "anchor": anchor_source,
                "city": city,
                "is_weekend": is_weekend,
                "month": month
            },
            "status": "success"
        }

    except Exception as e:
        print(f"Erreur lors de la prédiction: {e}")
        import traceback
        traceback.print_exc()
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/v1/risk/score/{user_id}")
async def get_risk_score(user_id: int, cancel_count: int, bad_reviews: int):
    try:
        features = pd.DataFrame([{
            "cancel_count": cancel_count,
            "bad_reviews": bad_reviews
        }])

        if RISK_MODEL:
            proba_trust = RISK_MODEL.predict_proba(features)[0][1]
        else:
            proba_trust = 0.8  # Fallback if model not loaded

        score = int(proba_trust * 100)

        risk_level = "LOW"
        if score < 40: risk_level = "HIGH"
        elif score < 70: risk_level = "MEDIUM"

        return {
            "userId": user_id,
            "score": score,
            "risk_level": risk_level,
            "details": {
                "cancel_impact": cancel_count * -5
            }
        }
    except Exception as e:
        return {"userId": user_id, "score": 100, "risk_level": "UNKNOWN", "error": str(e)}


@app.get("/api/v1/analytics/trends")
async def get_market_trends():
    """
    Returns market analysis:
    1. Forecasted prices for next 30 days for each major city.
    2. Best Model selected empirically (RandomForest vs Holt-Winters).
    3. Market Cluster (Grouping cities by trend similarity).

    Les prix de base viennent des moyennes réelles par ville du catalogue
    (MAD/nuit), avec repli sur les défauts réalistes du moteur sinon.
    """
    try:
        from collections import defaultdict
        city_prices: dict[str, list[float]] = defaultdict(list)
        for p in PROPERTIES:
            city = (p.get("city") or "").strip()
            price = float(p.get("pricePerNight") or 0)
            if city in analytics_engine.CITIES and price > 0:
                city_prices[city].append(price)
        base_prices = {
            city: sum(prices) / len(prices)
            for city, prices in city_prices.items()
            if prices
        } or None
        results = analytics_engine.get_market_analysis(base_prices=base_prices)
        return {"status": "success", "data": results}
    except Exception as e:
        print(f"Analytics Error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    # Lancement du serveur avec configuration .env
    host = os.getenv("HOST", "0.0.0.0")
    port = int(os.getenv("PORT", 8000))
    uvicorn.run(app, host=host, port=port)
