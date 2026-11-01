# FIFA Tournament Manager ⚽🏆

**FIFA Tournament Manager** est une application web moderne de gestion de tournois pour FIFA / EA Sports FC. Elle propose une architecture robuste avec un backend Spring Boot 4 / Java 17, une base de données PostgreSQL et un frontend React.

---

## 🚀 Fonctionnalités principales

- **Gestion des utilisateurs & rôles** : `ADMIN`, `ORGANIZER`, `PLAYER`.
- **Profils de joueurs** : Gamertag, plateforme (`PC`, `PS5`, `XBOX`), identifiant in-game.
- **Organisation de tournois** :
  - Formats supportés : `GROUPS_AND_KNOCKOUT`, `KNOCKOUT_ONLY`, `GROUPS_ONLY`.
  - Cycles de vie : `DRAFT`, `REGISTRATION_OPEN`, `REGISTRATION_CLOSED`, `GROUP_STAGE`, `PLAYOFFS`, `COMPLETED`, `CANCELLED`.
- **Inscriptions des joueurs** : Soumission et révision par l'organisateur (`PENDING`, `CONFIRMED`, `REJECTED`, `WITHDRAWN`).
- **Phases de groupes** : Génération des poules avec contrainte d'unicité stricte empêchant un joueur d'appartenir à plus d'un groupe par tournoi.
- **Matchs & Playoffs** : Matchs de groupes et arbre éliminatoire (`ROUND_OF_32` à `FINAL`), scores, forfaits et qualification automatique vers le match suivant.
- **Résultats & validation** : Soumission de score, confirmation par l’adversaire, contestation, résolution d’un désaccord, historique persistant et gestion des forfaits.
- **Traçabilité & Historique** : Journalisation complète des modifications de scores (`MatchResultHistory`).
- **Architecture DTO & MapStruct** : Séparation stricte entre entités de persistance et modèles d'échange DTO, avec validation Bean Validation (`@Valid`) au niveau des DTOs.

---

## 🛠️ Stack Technique

- **Java** : 17 (LTS)
- **Framework** : Spring Boot
- **Persistance** : Spring Data JPA, Hibernate ORM
- **Base de données** : PostgreSQL 17 / 18
- **Mapping objet** : MapStruct 1.6.3 + Lombok
- **Validation** : Jakarta Bean Validation
- **Sécurité** : Spring Security & OAuth2 Client
- **Build tool** : Apache Maven (wrapper inclus)

---

## 📂 Architecture du Projet

```text
FIFA/
├── back/
│   ├── src/main/java/com/example/FIFA/
│   │   ├── FifaApplication.java
│   │   ├── user/
│   │   │   ├── entity/
│   │   │   ├── enums/
│   │   │   ├── repository/
│   │   │   ├── dto/
│   │   │   └── mapper/
│   │   ├── player/
│   │   ├── tournament/
│   │   ├── registration/
│   │   ├── tournamentgroup/
│   │   └── match/
│   ├── src/main/resources/
│   │   └── application.properties
│   ├── src/test/java/com/example/FIFA/
│   │   ├── FifaApplicationTests.java
│   │   ├── JpaEntityPersistenceTests.java
│   │   └── DtoValidationAndMappingTests.java
│   └── pom.xml
├── front/
├── .gitignore
└── README.md
```

---

## ⚙️ Installation & Démarrage

### 1. Prérequis
- Java JDK 17
- PostgreSQL 17+ actif sur le port `5432` avec une base `FIFA`

### 2. Configuration
Modifiez si besoin [application.properties](file:///c:/Users/abdel/OneDrive/Documents/FIFA/back/src/main/resources/application.properties) :
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/FIFA
spring.datasource.username=postgres
spring.datasource.password=latifi-2001
server.port=8082
```

### 3. Exécution

```powershell
# Cloner le projet
git clone https://github.com/abdelalil544-blip/FIFA.git
cd FIFA/back

# Compiler et lancer les tests
.\mvnw.cmd test

# Lancer le serveur backend
.\mvnw.cmd spring-boot:run
```

---

## 🧪 Tests

La suite de tests automatisés couvre l'intégrité de la persistance, les contraintes SGBD et la validation des DTOs :
```powershell
.\mvnw.cmd test
```
