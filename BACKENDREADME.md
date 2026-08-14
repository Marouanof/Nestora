<div align="center">

  <h1>RentChain · Backend Microservices</h1>

  <p>
    Backend d’une dapp de location immobilière moderne,<br/>
    construit autour de <b>microservices Spring Boot</b> sécurisés par JWT<br/>
    et de <b>paiements par carte bancaire (Stripe)</b> pour les réservations.
  </p>

</div>

---

<div align="center">
  <h3>Tech Stack</h3>
  <p>
    <img src="https://skillicons.dev/icons?i=java,spring,postgres,rabbitmq" alt="Java, Spring, PostgreSQL, RabbitMQ" />
  </p>
  <p><small>Backend : Spring Boot · Sécurité : JWT · Asynchronous Messaging : RabbitMQ · DB : PostgreSQL · Paiements : Stripe</small></p>
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

## Architecture backend

```mermaid
graph LR
    Client["Frontend Client"]
    Client -->|REST+JWT| Gateway["API Gateway"]
    
    Gateway --> UserSvc["User Service"]
    Gateway --> PropertySvc["Property Service"]
    Gateway --> BookingSvc["Booking Service"]
    Gateway --> NotifSvc["Notification Service"]
    
    UserSvc --> UserDB["User DB"]
    PropertySvc --> PropertyDB["Property DB"]
    BookingSvc --> BookingDB["Booking DB"]
    NotifSvc --> NotifDB["Notification DB"]
    
    BookingSvc -->|Payment Events| RabbitMQ["RabbitMQ"]
    PropertySvc -->|Events| RabbitMQ
    RabbitMQ --> NotifSvc
    
    style Gateway fill:#4CAF50
    style UserSvc fill:#2196F3
    style PropertySvc fill:#2196F3
    style BookingSvc fill:#2196F3
    style NotifSvc fill:#2196F3
```

Chaque microservice possède sa propre base de données, sa configuration, et communique principalement via **REST** (via la gateway) et **RabbitMQ** pour les événements.

---

## 2. Microservices et responsabilités

| Service            | Dossier            | Port (par défaut) | Rôle principal |
|--------------------|--------------------|-------------------|----------------|
| API Gateway        | `gateway/`         | 8080              | Point d’entrée unique, routage, filtrage JWT, délégation vers les microservices |
| User Service       | `user-service/`    | 8081              | Authentification, gestion utilisateurs, profils, rôles, fichiers KYC & avatars |
| Property Service   | `property-service/`| 8082              | Propriétés, images, disponibilité, reviews, recherche, analytics |
| Booking Service    | `booking-service/` | 8083              | Réservations, cohérence des états, gestion du risque, intégration paiement |
| Notification Serv. | `notification-service/` | 8086         | Notifications persistées, emails, consommation d’événements RabbitMQ |
| Payment Service    | `payment-service/` (prévu) | 8084       | Webhooks de paiement carte (Stripe), confirmation des réservations |

---

## 3. Mon travail en tant que développeur backend

### 3.1. Architecture & communication
- Conception de l’**architecture microservices** autour de domaines métier clairs : utilisateurs, propriétés, réservations, notifications, paiements.
- Mise en place de la **API Gateway** (Spring Cloud Gateway) avec routage dynamique, filtres custom (`JwtAuthenticationFilter`), et propagation des informations utilisateur.
- Définition des **contrats REST** (DTO, conventions d’URL, statuts HTTP, gestion d’erreurs centralisée) pour chaque service.
- Utilisation de **RabbitMQ** pour les événements asynchrones (réservations créées/annulées, statut de paiement, notifications).

### 3.2. Sécurité & identité
- Mise en place de l’**authentification JWT** (génération, validation, refresh) dans `user-service` et intégration dans la gateway.
- Gestion des **rôles et permissions** (TENANT, OWNER, ADMIN) et des routes publiques / protégées.
- Création de filtres et endpoints internes pour la **communication inter-service sécurisée**.

### 3.3. Services métier
- **User Service** :
  - APIs d’inscription, login, refresh, reset password, gestion du profil.
  - Gestion des **fichiers utilisateurs** (avatars, KYC recto/verso), stockage local dans `uploads/` (et préparation pour S3 via `S3Config` et un `FileStorageService`).
  - Vérification d’email, endpoints internes pour les autres services.

- **Property Service** :
  - APIs de **CRUD propriétés**, recherche avancée, gestion de la disponibilité et des images.
  - Gestion des **reviews** (création, modération admin, statistiques) et d’**analytics** (exposition des données au frontend ou à l’IA).
  - Intégration avec le user-service pour enrichir les réponses (propriétaires, profils, etc.).

- **Booking Service** :
  - APIs de **création, modification, annulation et consultation des réservations**.
  - Gestion de l’état des réservations (pending, confirmed, completed, cancelled…) et validation métier.
  - Intégration avec **property-service** et **user-service** via clients HTTP.
  - Production d’événements de **statut de paiement** (succès/échec) vers les autres services.

- **Notification Service** :
  - Stockage des notifications utilisateur (PostgreSQL), listing, marquage comme lues, suppression.
  - Envoi d’**emails** via SMTP.
  - Consommation des messages RabbitMQ émis par booking/property.

---

## 4. Détails par service (résumé technique)

### 4.1. Gateway – [gateway/](gateway)
- **Technos** : Spring Boot, Spring Cloud Gateway, JWT.
- **Responsabilités** :
  - Routage vers les microservices (`GatewayRoutes.java`).
  - Validation des tokens (`JwtAuthenticationFilter`, `JwtTokenProvider`).
  - Définition des routes publiques/privées et propagation des headers utilisateur.

### 4.2. User Service – [user-service/](user-service)
- **Technos** : Spring Boot (Web, Security, Data JPA, Mail), PostgreSQL, Lombok.
- **Responsabilités** :
  - Authentification JWT, refresh tokens, gestion de session côté backend.
  - Gestion des profils, avatars, fichiers KYC.
  - Rôles, permissions, endpoints admin, endpoints internes.

### 4.3. Property Service – [property-service/](property-service)
- **Technos** : Spring Boot (Web, Data JPA, Validation, AMQP), PostgreSQL, Lombok.
- **Responsabilités** :
  - CRUD propriétés, filtres de recherche, pagination.
  - Gestion des images et stockage dans `uploads/`.
  - Reviews, analytics, intégration RabbitMQ.

### 4.4. Booking Service – [booking-service/](booking-service)
- **Technos** : Spring Boot (Web, Data JPA, Validation, AMQP, OpenFeign), PostgreSQL.
- **Responsabilités** :
  - Modèle de réservation central du système.
  - Communication avec user-service et property-service via clients HTTP.
  - Gestion des erreurs métier spécialisées (conflits de réservation, profils incomplets, etc.).
  - Écoute des événements de statut de paiement (succès → `CONFIRMED`, échec → `PENDING_PAYMENT`).

### 4.5. Notification Service – [notification-service/](notification-service)
- **Technos** : Spring Boot (Web, Data JPA, AMQP, Mail, OpenFeign), PostgreSQL, RabbitMQ.
- **Responsabilités** :
  - Stockage et exposition des notifications.
  - Envoi d’emails selon les événements reçus.

---

## 5. Communication, persistance et fichiers

- **REST + JSON** : communication synchronisée entre les services via la gateway.
- **RabbitMQ** : événements métier (réservations, statut de paiement, notifications).
- **Bases de données** : chaque microservice possède son propre schéma (approche microservices réelle).
- **Fichiers** :
  - Avatars & KYC dans `user-service/uploads/`.
  - Documents & images de propriétés dans `property-service/uploads/`.
  - Architecture prête pour une migration vers **S3** via les configs déjà présentes.

---

## 6. Lancer le backend en local

### 6.1. Prérequis globaux
- Java 21+.
- Maven.
- PostgreSQL (une base par service, ou schémas séparés).
- RabbitMQ.

### 6.2. Démarrage des microservices Java
Depuis chaque dossier de service (`gateway/`, `user-service/`, `property-service/`, `booking-service/`, `notification-service/`) :

```bash
mvn clean package
java -jar target/<nom-du-jar>.jar
```

Les ports par défaut sont listés dans les README de chaque service et dans leurs fichiers `application.yml` / `application.properties`.

## 7. Technologies clés

- **Langages** : Java (Spring Boot).
- **Frameworks** : Spring Boot, Spring Cloud Gateway, Spring Security.
- **Messaging** : RabbitMQ.
- **Base de données** : PostgreSQL (JPA/Hibernate).
- **Paiements** : Stripe (paiement par carte bancaire).
- **Infra** : Profils Spring, configuration via variables d’environnement / fichiers `.env`.


Ce README présente le travail backend réalisé autour de cette architecture microservices et du paiement par carte, dans un contexte proche d’une application de production moderne.
