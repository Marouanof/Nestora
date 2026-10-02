import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestRegressor
from sklearn.cluster import KMeans
from sklearn.preprocessing import StandardScaler, MinMaxScaler
from sklearn.metrics import mean_squared_error
from statsmodels.tsa.holtwinters import ExponentialSmoothing
import joblib
import json
import os
import random
from datetime import datetime, timedelta

# --- 0. REAL MARKET REFERENTIAL (DeRent5 Airbnb listings, Morocco) ---
# ai-service/data/morocco_market.json : médianes réelles par ville x mois.
# Généré par scripts hors repo depuis le CSV 65k annonces (voir meta.source).
_MARKET_REF = None

# Le CSV DeRent5 utilise parfois les noms anglais (Tangier) : on les rabat
# sur les noms utilisés par CITIES.
CITY_ALIASES = {
    "Tangier": "Tanger",
}

def load_market_referential():
    """Charge (une fois) les médianes mensuelles réelles par ville."""
    global _MARKET_REF
    if _MARKET_REF is not None:
        return _MARKET_REF
    _MARKET_REF = {}
    try:
        path = os.path.join(os.path.dirname(__file__), "data", "morocco_market.json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        for city, entry in (data.get("cities") or {}).items():
            city = CITY_ALIASES.get(city, city)
            _MARKET_REF[city] = {
                "median": entry.get("median"),
                "by_month": {m: v.get("median") for m, v in (entry.get("by_month") or {}).items()
                             if v.get("median")},
            }
    except Exception as e:
        print(f"Referentiel marche introuvable ({e}) : repli synthétique")
    return _MARKET_REF


def monthly_price_for(city, when, city_fallback):
    """Prix du mois réel (même mois une autre année si besoin), sinon médiane ville."""
    ref = load_market_referential().get(city) or {}
    by_month = ref.get("by_month") or {}
    key = when.strftime("%Y-%m")
    if by_month.get(key):
        return by_month[key]
    same_mm = [v for k, v in by_month.items() if k.endswith(when.strftime("-%m")) and v]
    if same_mm:
        return same_mm[0]
    return city_fallback

# --- 1. HISTORY (real monthly medians anchored, synthetic fallback) ---
# 12 mois de médianes réelles par ville (DeRent5 Airbnb Maroc) quand le
# référentiel data/morocco_market.json est présent, sinon repli synthétique.

CITIES = ["Casablanca", "Rabat", "Agadir", "Fes", "Tanger"]

# Prix de base par ville (MAD/nuit). Valeurs par défaut réalistes, mais
# get_market_analysis() préfère les moyennes calculées sur les vraies
# annonces (voir main.py) quand elles sont disponibles.
DEFAULT_BASE_PRICES = {
    "Casablanca": 950.0,
    "Rabat": 800.0,
    "Agadir": 1100.0,
    "Fes": 700.0,
    "Tanger": 900.0
}

def generate_historical_data(days=365, base_prices=None):
    """
    Historique quotidien par ville, ancré sur les médianes mensuelles RÉELLES
    (référentiel DeRent5) quand disponibles : chaque jour vaut la médiane de
    son mois. Repli synthétique (saisonnalité + bruit) sinon.
    base_prices: dict ville -> prix moyen MAD/nuit (priorité sur les défauts).
    """
    end_date = datetime.now()
    start_date = end_date - timedelta(days=days)
    dates = pd.date_range(start=start_date, end=end_date, freq='D')
    
    all_data = []

    bases = base_prices or DEFAULT_BASE_PRICES
    ref = load_market_referential()
    use_real = any(ref.get(c, {}).get("by_month") for c in CITIES)
    for city in CITIES:
        base = bases.get(city) or DEFAULT_BASE_PRICES[city]

        if use_real and ref.get(city, {}).get("by_month"):
            # Ancrage réel : médiane du mois observé (forme saisonnière vraie)
            prices = [monthly_price_for(city, d, base) for d in dates]
        else:
            # Repli synthétique
            seasonality = []
            for d in dates:
                m = d.month
                factor = 1.0
                if m in [6, 7, 8]: factor = 1.3
                elif m == 12: factor = 1.2

                # Weekend bump
                if d.weekday() >= 4: factor += 0.1

                # Random market fluctuation (-10% to +10%)
                noise = random.uniform(0.9, 1.1)

                seasonality.append(factor * noise)

            prices = [base * f for f in seasonality]
        
        city_df = pd.DataFrame({
            'date': dates,
            'city': city,
            'avg_price': prices
        })
        all_data.append(city_df)
        
    return pd.concat(all_data, ignore_index=True)

# --- 2. MODEL SELECTION & FORECASTING ---

def align_features(X, expected_features):
    """
    Ensures X has exactly the expected_features columns.
    Adds missing columns with 0, removes extra columns, and reorders.
    """
    # 1. Add missing cols
    for col in expected_features:
        if col not in X.columns:
            X[col] = 0

    # 2. Drop extra cols (that are NOT in expected)
    # Be careful not to drop ALL cols if something is wrong.
    # intersection keeps only what's in expected, but we need to match ORDER too.
    
    # 3. Reorder to match expected_features exactly
    return X[expected_features]

def train_and_forecast(city_df, forecast_days=30):
    """
    Empirical Model Selection:
    1. Trains Random Forest
    2. Trains Holt-Winters (Exponential Smoothing)
    3. Compares RMSE on last 30 days validation set
    4. Picks Winner & Forecasts next 30 days
    """
    # Split Train/Test (Last 30 days for validation)
    train_size = len(city_df) - 30
    train = city_df.iloc[:train_size].copy()
    test = city_df.iloc[train_size:].copy()
    
    # --- Model A: Random Forest (ML) ---
    # Feature Engineering for RF
    def create_features(df):
        df = df.copy()
        df['day_of_year'] = df['date'].dt.dayofyear
        df['month'] = df['date'].dt.month
        df['day_of_week'] = df['date'].dt.dayofweek
        df['lag_1'] = df['avg_price'].shift(1)
        df['lag_7'] = df['avg_price'].shift(7)
        df = df.dropna()
        return df

    df_rf = create_features(city_df)
    
    # We need to re-split after feature engineering (rows dropped due to lags)
    train_rf = df_rf.iloc[:len(df_rf)-30]
    test_rf = df_rf.iloc[len(df_rf)-30:]
    
    features = ['day_of_year', 'month', 'day_of_week', 'lag_1', 'lag_7']
    X_train = train_rf[features]
    y_train = train_rf['avg_price']
    X_test = test_rf[features]
    y_test = test_rf['avg_price']
    
    rf = RandomForestRegressor(n_estimators=100, max_depth=10, random_state=42)
    rf.fit(X_train, y_train)
    pred_rf = rf.predict(X_test)
    rmse_rf = np.sqrt(mean_squared_error(y_test, pred_rf))
    
    # --- Model B: Exponential Smoothing (Statistical) ---
    # HW works on univariate time series
    try:
        # Ensure index has frequency for statsmodels
        ts_train = train.set_index('date')['avg_price']
        ts_train.index.freq = 'D'
        
        hw = ExponentialSmoothing(ts_train, seasonal_periods=7, trend='add', seasonal='add').fit()
        pred_hw = hw.forecast(steps=len(test))
        rmse_hw = np.sqrt(mean_squared_error(test['avg_price'], pred_hw))
    except Exception as e:
        print(f"HW Forecast Error: {e}")
        rmse_hw = float('inf') # Fallback if fails
        
    # --- Selection ---
    winner = "Random Forest" if rmse_rf < rmse_hw else "Holt-Winters"
    
    # --- Final Forecast (Next 30 Days) ---
    last_date = city_df['date'].max()
    future_dates = pd.date_range(start=last_date + timedelta(days=1), periods=forecast_days, freq='D')
    
    future_prices = []
    
    if winner == "Holt-Winters":
        try:
            # Refit on FULL data
            ts_full = city_df.set_index('date')['avg_price']
            ts_full.index.freq = 'D'
            
            full_hw = ExponentialSmoothing(ts_full, seasonal_periods=7, trend='add', seasonal='add').fit()
            future_prices = full_hw.forecast(steps=forecast_days).tolist()
        except:
             # Fallback to RF if HW refit fails
             winner = "Random Forest (Fallback)"
    else:
        # Refit RF on FULL data
        X_full = df_rf[features]
        y_full = df_rf['avg_price']
        rf.fit(X_full, y_full)
        
        # Recursive forecasting for RF (since we need lags)
        last_row = df_rf.iloc[-1]
        current_lag_1 = last_row['avg_price']
        # Approximate lag_7 for future using recent history
        recent_prices = list(df_rf['avg_price'].values[-7:])
        
        for i in range(forecast_days):
            next_date = future_dates[i]
            feat_dict = {
                'day_of_year': next_date.dayofyear,
                'month': next_date.month,
                'day_of_week': next_date.dayofweek,
                'lag_1': current_lag_1,
                'lag_7': recent_prices[0] # Very simple rolling
            }
            # Update rolling
            pred = rf.predict(pd.DataFrame([feat_dict]))[0]
            future_prices.append(pred)
            
            current_lag_1 = pred
            recent_prices.pop(0)
            recent_prices.append(pred)

    return {
        "city": city_df['city'].iloc[0],
        "model_used": winner,
        "rmse_error": float(min(rmse_rf, rmse_hw)),
        "forecast": [{"date": str(d.date()), "price": float(p)} for d, p in zip(future_dates, future_prices)]
    }

# --- 3. CLUSTERING ---

def cluster_cities(df_history):
    """
    Groups cities by their price shape (Normalized).
    """
    # Pivot: Index=Date, Cols=City, Values=Price
    pivoted = df_history.pivot(index='date', columns='city', values='avg_price')
    
    # Normalize each city's curve (MinMax) so we cluster by SHAPE not Magnitude
    scaler = MinMaxScaler()
    scaled_data = scaler.fit_transform(pivoted) # Returns shape (days, cities)
    
    # We want to cluster Cities (Columns), so transpose
    X = scaled_data.T 
    
    # KMeans (Fixed 3 clusters for simplicity: Stable, Seasonally High, Volatile/Growing)
    kmeans = KMeans(n_clusters=3, random_state=42)
    labels = kmeans.fit_predict(X)
    
    results = {}
    for city, label in zip(pivoted.columns, labels):
        # Auto-labeling (Simplified)
        cluster_name = f"Cluster {label}" 
        results[city] = cluster_name
        
    return results

def get_market_analysis(base_prices=None):
    df = generate_historical_data(base_prices=base_prices)
    
    # 1. Forecasting
    forecasts = []
    for city in CITIES:
        city_data = df[df['city'] == city]
        forecasts.append(train_and_forecast(city_data))
        
    # 2. Clustering
    clusters = cluster_cities(df)
    
    # 3. Combine
    for f in forecasts:
        f['market_cluster'] = clusters.get(f['city'], "Unknown")
        
    return forecasts
