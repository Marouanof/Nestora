import pandas as pd
import numpy as np
import joblib
from sklearn.ensemble import RandomForestClassifier

def train_risk_model():
    print("⏳ Génération des données de risque...")
    data = []
    
    # On simule 1000 profils d'utilisateurs
    for _ in range(1000):
        cancel_count = np.random.randint(0, 10) # Annulations
        bad_reviews = np.random.randint(0, 5)   # Mauvais retours
        
        # Logique de cible (Label) : 
        # 0 = Risque (Mauvais payeur/Annuleur), 1 = Confiance (Top Tenant)
        # On définit un score arbitraire pour l'entraînement
        score_logic = 40 - (cancel_count * 10) - (bad_reviews * 20)
        
        # Si score > 0 alors 'Trustworthy' (1), sinon 'Risky' (0)
        trust_label = 1 if score_logic > 0 else 0
        
        data.append({
            "cancel_count": cancel_count,
            "bad_reviews": bad_reviews,
            "label": trust_label
        })

    df = pd.DataFrame(data)
    X = df.drop("label", axis=1)
    y = df["label"]

    print("🤖 Entraînement du RandomForest...")
    model = RandomForestClassifier(n_estimators=100, random_state=42)
    model.fit(X, y)

    # Sauvegarde
    joblib.dump(model, "risk_model.joblib")
    print("✅ Modèle 'risk_model.joblib' créé !")

if __name__ == "__main__":
    train_risk_model()
