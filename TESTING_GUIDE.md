# Guide de Test - Intégration AI Risk Service

Ce guide détaille comment tester le nouveau service de score de risque (AI Service) et son intégration avec le `BookingService`.

## 1. Prérequis

Assurez-vous d'avoir :
- **Python 3.9+** (pour le service IA)
- **Java 17+ & Maven** (pour le backend Spring Boot)
- **Postman** ou **cURL** (pour les tests API)

## 2. Démarrage et Test du Service IA (Python)

Le service IA expose l'endpoint de scoring et entraîne le modèle au démarrage.

### 2.1 Installation et Lancement

1.  Accédez au dossier du service IA :
    ```bash
    cd ai-service
    ```
2.  Installez les dépendances :
    ```bash
    pip install fastapi uvicorn scikit-learn pandas requests
    ```
3.  Lancez le serveur :
    ```bash
    uvicorn main:app --reload --port 8000
    ```
    *Vous devriez voir un log indiquant que le modèle a été entraîné (`✅ Model trained on synthetic data...`).*

### 2.2 Test Unitaire de l'Endpoint

Utilisez `curl` ou Postman pour tester le calcul du score.

**Cas 1 : Utilisateur parfait (Score ~100)**
```bash
curl "http://localhost:8000/api/v1/risk/score/123?cancel_count=0&bad_reviews=0"
```
*Réponse attendue :* `{"userId":123, "score":100, "risk_level":"LOW"}`

**Cas 2 : Utilisateur à risque (Score < 50)**
```bash
curl "http://localhost:8000/api/v1/risk/score/123?cancel_count=3&bad_reviews=2"
```
*Logique d'entraînement :* 40 - (3*10) - (2*20) = 40 - 30 - 40 = -30 → profil "Risky", score faible (Clampé à 0 par le modèle ou les règles)
*Réponse attendue :* `{"userId":123, "score":0 (ou proche), "risk_level":"HIGH"}`

## 3. Test de l'Intégration Backend (Spring Boot)

Le `BookingService` appelle désormais le service IA lors de la validation d'une réservation.

### 3.1 Configuration

Assurez-vous que l'URL du service IA est correcte dans `booking-service/src/main/resources/application.properties` (ou `.yml`) :
```properties
app.services.ai-service-url=http://localhost:8000
```
*(Si la propriété n'existe pas, la valeur par défaut dans `AIServiceClient.java` est déjà http://localhost:8000)*

### 3.2 Lancement du Booking Service

1.  Compilez et lancez le service :
    ```bash
    cd booking-service
    mvn spring-boot:run
    ```

### 3.3 Simulation d'une Réservation

Déclenchez une réservation qui appelle `validateUserForBooking`.

1.  **Préparer un utilisateur** (via User Service ou directement en base si vous testez localement). Assurez-vous qu'il a :
    - un historique propre (peu d'annulations / mauvais avis) → bon score
    - ou un historique chargé → score faible

2.  **Envoyer une requête de création de booking** :
    ```bash
    POST http://localhost:8081/api/bookings
    Content-Type: application/json
    X-Auth-User-Id: 1
    
    {
      "propertyId": 10,
      "checkIn": "2024-06-01",
      "checkOut": "2024-06-05",
      "numberOfGuests": 2
    }
    ```

3.  **Vérifier les Logs (Console Java)**
    Cherchez les lignes suivantes :
    ```
    INFO ... BookingService : ✅ User 1 validated for booking (KYC OK)
    INFO ... BookingService : 🛡️ Risk Analysis for User 1: Score=100, Level=LOW
    ```

## 4. Test de l'Option B (Affichage du Score Frontend)

Vous pouvez maintenant récupérer le score d'un utilisateur sans créer de réservation, pour l'afficher sur son profil.

**Endpoint :** `GET /api/risk/me`

**Via Gateway (Frontend)** : `http://localhost:8080/api/risk/me` (Nécessite Header `Authorization: Bearer <token>`)
**Direct (Test)** : `http://localhost:8081/api/risk/me`

```bash
# Test Direct (Booking Service)
curl "http://localhost:8081/api/risk/me" \
  -H "X-Auth-User-Id: 123"
```

*Réponse attendue :*
```json
{
    "userId": 123,
    "score": 100,
    "risk_level": "LOW_RISK"
}
```

## 5. Test de Résilience (Failover)

Vérifiez que le backend ne plante pas si l'IA est hors ligne.

1.  **Arrêtez le service IA** (Ctrl+C dans le terminal Python).
2.  **Relancez la même requête de booking**.
3.  **Vérifiez les Logs** :
    Vous devriez voir un avertissement au lieu d'une erreur bloquante :
    ```
    WARN ... BookingService : ⚠️ AI Service unavailable or error, proceeding without risk score: ...
    ```
    *La réservation doit continuer normalement (code 200/201).*
