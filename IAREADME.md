<div align="center">

  <h1>AI-Service</h1>

  <p>
    Microservice IA pour la plateforme de location immobilière :<br/>
    <b>recommandations de propriétés</b>, <b>tarification dynamique</b>, <b>scoring de risque</b><br/>
    et <b>analytics de marché</b>, propulsés par Python, FastAPI et des modèles de Machine Learning.
  </p>

</div>

---
## 👤 Auteur & Contacts

<div align="center">
  <p>Réalisé par <b>FAIK MAROUANE</b></p>
  <p>
    <a href="https://github.com/marouanof" target="_blank" rel="noreferrer">
      <img src="https://skillicons.dev/icons?i=github" alt="GitHub" />
    </a>
  </p>
</div>

---

<div align="center">
  <h3>Tech Stack</h3>
  <p><img src="https://skillicons.dev/icons?i=python,fastapi,pandas,numpy" alt="Python, FastAPI, pandas, numpy" /></p>
  <p><small>ML : scikit-learn (RandomForest, K-Means, cosine similarity), XGBoost, statsmodels (Holt-Winters)</small></p>
</div>

---

## 🔍 About the Project

AI-Service est un microservice d’**intelligence artificielle** dédié à la plateforme de location immobilière du projet.
Il fournit des capacités avancées de :

- Recommandation de propriétés en fonction du profil et du budget de l’utilisateur.
- **Tarification dynamique** pour optimiser le revenu des propriétaires et la conversion côté locataire.
- **Scoring de risque** pour évaluer la fiabilité des locataires dans le processus de réservation.
- Analytics et tendances de marché exploitables par les autres microservices.

Il est développé en **Python/FastAPI**, consomme et enrichit les données des microservices Java,
et expose une API REST propre, pensée pour s’intégrer facilement dans une architecture microservices existante.

---

## ✨ Key Features

- 🔮 **Property Recommendations** – recommandations basées sur le budget et le profil, via <b>K-Means</b> + <b>cosine similarity</b> sur les propriétés.
- 💸 **Dynamic Pricing Engine** – suggestion de prix optimisés grâce à un modèle <b>XGBoost (XGBRegressor)</b> combiné à des règles de saisonnalité et de week-end.
- 🛡️ **Tenant Risk Scoring** – score de risque utilisateur calculé avec un <b>RandomForestClassifier</b> entraîné sur des données synthétiques (annulations, mauvais avis).
- 📊 **Market Analytics** – prévisions et <b>clustering temporel</b> des villes via <b>RandomForestRegressor</b> vs <b>Holt-Winters (Exponential Smoothing)</b> et <b>K-Means</b> sur les séries.
- ⚙️ **API REST FastAPI** – endpoints documentés (Swagger / Redoc) intégrables par les services Java.
- 📦 **Modèles ML versionnés** – modèles entraînés et sérialisés (joblib) pour une mise en production simple.

---

## 🚀 Quick Start

### 1. Cloner le dépôt & se placer dans le service IA

```bash
git clone <URL_DU_REPO>
cd ai-service
```

### 2. Installer les dépendances Python

```bash
pip install -r requirements.txt
```

### 3. Lancer le serveur FastAPI

```bash
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

L’API sera accessible sur `http://localhost:8000` 
---

## 🧠 Architecture & Challenges

Le service est structuré autour de plusieurs composants clés :

- `main.py` pour la définition des endpoints FastAPI et le chargement des modèles.
- `analytics_engine.py` pour les traitements analytiques avancés (trends, clustering…).
- `train_pricing.py` et `train_risk_model.py` pour l’entraînement et la mise à jour des modèles.
- Des artefacts ML sérialisés (`*.joblib`) utilisés en inférence temps réel.


Cette section met en avant la capacité à concevoir un service IA **industriel**, connecté à des microservices Java tout en restant maintenable, testable et extensible.

---

# AI Service – Architecture & Réalisations

Ce document présente en détail le **service d’IA** de la plateforme, développé en **Python / FastAPI** et intégré au reste de l’architecture microservices Java.

L’objectif de ce service est de fournir :
- Des **recommandations de propriétés** personnalisées.
- Une **tarification dynamique** des logements (pricing).
- Un **scoring de risque utilisateur** (tenant risk).
- Des **analytics de marché** (tendances, clustering, prévisions).

---

## 1. Contexte dans l’architecture globale

Le AI Service est un microservice indépendant, exposé en HTTP (REST) et consommé principalement par :
- **Booking Service && User Service** : pour la tarification des réservations et le scoring de risque.
- **Property Service** : pour la recommandation et les analytics.

```text
Java Microservices (Spring Boot)
   │
   │  HTTP / JSON
   ▼
AI Service (FastAPI, Python)
   ├─ Modèles ML (pricing)
   ├─ Modèles ML (risque utilisateur)
   └─ Recommandations & analytics marché
```

Le service est **découplé** des autres services (technologie différente, cycle de vie propre) mais partage des contrats d’échange (DTO JSON) bien définis.

---

## 2. Structure du projet AI

Dossier : [ai-service/](ai-service)

- `main.py` :
  - Application **FastAPI**.
  - Définition des endpoints principaux (recommandations, tarification, risque, analytics).
  - Chargement des modèles ML (pricing, risque, recommandation).

- `analytics_engine.py` :
  - Fonctions d’**analytics avancés** (trends, clustering, prévisions, agrégations sur l’historique).
  - Utilisation de pandas / numpy pour manipuler les données.

- `train_pricing.py` :
  - Script d’**entraînement du modèle de tarification** (XGBoost ou modèle régressif).
  - Génération du fichier `pricing_model.joblib`.

- `train_risk_model.py` :
  - Script d’**entraînement du modèle de risque utilisateur**.
  - Génération du fichier `risk_model.joblib` et `models/tenant_risk_model.joblib`.

- `models/` :
  - `recommender_data.joblib` : données/features pré-calculées pour la recommandation.
  - `tenant_risk_model.joblib` : modèle ML pour le risque utilisateur.

- Fichiers de modèles racine :
  - `pricing_model.joblib` : modèle d’estimation de prix.
  - `risk_model.joblib` : modèle de risque (version / alias).

- `requirements.txt` :
  - Dépendances Python (FastAPI, Uvicorn, scikit-learn, XGBoost, pandas, numpy, python-dotenv, etc.).

---

## 3. Fonctionnalités IA implémentées

### 3.1. Recommandation de propriétés
- Entrées typiques :
  - Budget utilisateur, localisation, caractéristiques de la propriété (taille, type, etc.).
- Traitement :
  - Utilisation de **représentations vectorielles** des propriétés et/ou de l’historique.
  - Calcul de similarités (ex : **cosine similarity**) et éventuellement **clustering** par zone / catégorie.
- Sortie :
  - Liste de propriétés recommandées avec un score de pertinence.

### 3.2. Tarification dynamique (Pricing)
- Objectif : suggérer un **prix optimal** pour une propriété donnée à une date donnée.
- Features utilisées (exemples) :
  - Saison / mois de l’année, jour de la semaine (week-end vs semaine).
  - Caractéristiques du bien (surface, localisation, capacité, etc.).
  - Retour d’expérience sur l’occupation et le revenu.
- Modèle :
  - **XGBoost**, entraîné via `train_pricing.py`.
- Sortie :
  - `PriceCalculationResult` avec prix conseillé et éventuellement des indicateurs (min/max, intervalle de confiance, etc.).

### 3.3. Scoring de risque utilisateur
- Objectif : renvoyer un **score de risque** pour un locataire.
- Features possibles :
  - Nombre d’annulations, d’incidents passés, historique d’avis.
  - Statut KYC, ancienneté du compte.
- Modèle :
  - Classifieur ou régression de probabilité, entraîné via `train_risk_model.py`.
- Sortie :
  - Score numérique (0–100) avec éventuellement une catégorie (LOW, MEDIUM, HIGH).

### 3.4. Analytics de marché
- Clustering de villes / zones selon les prix.
- Agrégations et tendances des prix (moyennes, médianes, saisonnalité).
- Possibilité de produire des séries temporelles pour des dashboards.

---

## 4. Endpoints principaux

Les routes exactes peuvent évoluer, mais la structure principale est la suivante (définie dans `main.py`) :

- `GET /api/v1/recommendations` :
  - Paramètres : budget, localisation, etc.
  - Réponse : liste de propriétés recommandées.

- `GET /api/v1/pricing/suggest` :
  - Paramètres : ID propriété, date, éventuellement contexte (occupations passées).
  - Réponse : prix conseillé + informations complémentaires.

- `GET /api/v1/risk/score/{user_id}` :
  - Paramètres : `user_id`.
  - Réponse : score de risque et niveau.

- `GET /api/v1/analytics/trends` :
  - Paramètres : filtres sur zone, période, etc.
  - Réponse : agrégations et tendances de marché.

Les microservices Java consomment ces endpoints via des **clients HTTP** (OpenFeign ou RestTemplate) et mappent les réponses vers leurs **DTO**.

---

## 5. Lancement du AI Service

### 5.1. Prérequis
- **Python 3.10+**
- `pip` ou `pipenv`

### 5.2. Installation et exécution

```bash
cd ai-service
pip install -r requirements.txt

# Optionnel : réentraîner les modèles
python train_pricing.py
python train_risk_model.py

# Lancement du serveur FastAPI
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

---

## 6. Rôle et compétences démontrées

Dans ce service, j’ai :
- Conçu une **API IA spécialisée** pour un environnement de microservices (contrats clairs, DTO adaptés aux services Java).
- Intégré des **modèles de Machine Learning** dans un service web (chargement, pré/post-traitement, versionnement).
- Structuré les **scripts d’entraînement** (`train_pricing.py`, `train_risk_model.py`) pour séparer entraînement et inférence.
- Assuré l’**interopérabilité** entre le monde Java/Spring et Python/FastAPI (formats JSON, schémas de données, gestion des erreurs).
- Préparé le projet pour une **industrialisation** : gestion des dépendances, variables d’environnement, endpoints documentés, et séparation claire des responsabilités.

Ce service IA complète l’architecture backend globale en apportant une couche de **décision intelligente** (pricing, recommandation, risque) au-dessus des données métiers gérées par les autres microservices.
