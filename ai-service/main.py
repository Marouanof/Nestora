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

# --- REAL PROPERTY DATA (fetched from property-service) ---
# Les vraies propriétés actives proviennent désormais du property-service.
# pricePerNight est en EUR. Le client retombe sur des données de secours si le
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
    global MODEL, RISK_MODEL
    if os.path.exists(MODEL_PATH):
        MODEL = joblib.load(MODEL_PATH)
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
    Calcule un prix basé sur : l'IA (XGBoost) + Saisonnalité + Week-end + rendement.
    Utilise les vraies caractéristiques de la propriété (lat/lon/chambres/...) et
    ancre la suggestion autour du prix réel du listing pour rester cohérent.
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
            rooms = prop["bedrooms"]
            baths = prop["bathrooms"]
            guests = prop["maxGuests"]
            amenities = prop["amenities_count"]
            real_price = prop["pricePerNight"]
        else:
            # Propriété inconnue du catalogue : valeurs par défaut
            lat, lon, rooms, baths, guests = 33.0, -7.0, 1, 1, 2
            amenities = 3
            real_price = None

        # 1. Prédiction "base" avec premium neutre (1.0)
        base_features = pd.DataFrame([{
            "lat": lat,
            "lon": lon,
            "bedrooms": rooms,
            "bathrooms": baths,
            "maxGuests": guests,
            "amenities_count": amenities,
            "rating": 4.5,
            "city_premium": 1.0
        }])
        base_prediction = float(MODEL.predict(base_features)[0])

        # 2. Premium de ville dérivé du prix réel du listing (pour ancrer la suggestion)
        city_premium = 1.0
        if real_price and base_prediction > 0:
            city_premium = real_price / base_prediction
            city_premium = float(np.clip(city_premium, 0.7, 2.2))
        elif prop is not None:
            # Heuristique simple par notoriété si pas de prix exploitable
            known = {"Marrakech": 1.5, "Casablanca": 1.4, "Tanger": 1.2,
                     "Agadir": 1.1, "Essaouira": 1.2, "Maroc": 1.0}
            city_premium = known.get((prop.get("city") or "").strip(), 1.0)

        price_with_premium = base_prediction * city_premium

        # 3. Règles dynamiques (Saisonnalité)
        multiplier = 1.0
        if month in [6, 7, 8]:      # Boost été
            multiplier += 0.30
        elif month == 12:           # Boost fêtes
            multiplier += 0.20

        if is_weekend:              # Boost week-end (+10%)
            multiplier += 0.10

        # 4. Rendement (+12%)
        yield_multiplier = 1.12
        final_price = price_with_premium * multiplier * yield_multiplier

        return {
            "propertyId": property_id,
            "property_id": property_id,
            "suggested_price_mad": round(float(final_price), 2),
            "currency": "MAD",
            "details": {
                "base_ai_price": round(float(price_with_premium), 2),
                "city_premium": round(city_premium, 3),
                "season_impact": f"+{int((multiplier-1)*100)}%",
                "yield_bonus": "12%",
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
    """
    try:
        results = analytics_engine.get_market_analysis()
        return {"status": "success", "data": results}
    except Exception as e:
        print(f"Analytics Error: {e}")
        raise HTTPException(status_code=500, detail=str(e))


if __name__ == "__main__":
    # Lancement du serveur avec configuration .env
    host = os.getenv("HOST", "0.0.0.0")
    port = int(os.getenv("PORT", 8000))
    uvicorn.run(app, host=host, port=port)
