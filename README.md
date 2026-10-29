# Meevy Backend

Backend di **Meevy**, un'app in sviluppo per organizzare lo studio. Al momento permette di gestire un account, le proprie materie e le sessioni Pomodoro personali.

Progetto di Ingegneria del Software sviluppato da Domenico e Rita.

## Tecnologie

Java 21, Spring Boot 4.1.1, Spring Data JPA, PostgreSQL, Flyway, Spring Security (JWT) e Maven. Per i test usiamo JUnit, Mockito e JaCoCo.

## Avvio in locale

Servono **Java 21**, **PostgreSQL** e **Maven**.

1. Clona il repository:
   ```bash
   git clone https://github.com/Dom-Doms/MeevyBackend.git
   cd MeevyBackend
   ```
2. In PostgreSQL crea un database chiamato `meevy`. La connessione configurata è `jdbc:postgresql://localhost:5432/meevy`.
3. Imposta queste variabili d'ambiente:

   | Variabile | Contenuto |
   | --- | --- |
   | `DB_USERNAME` | Utente PostgreSQL |
   | `DB_PASSWORD` | Password PostgreSQL |
   | `MEEVY_JWT_SECRET_BASE64` | Chiave segreta Base64 (almeno 32 byte dopo la decodifica) |

   Su PowerShell, ad esempio:
   ```powershell
   $env:DB_USERNAME = "postgres"
   $env:DB_PASSWORD = "la-tua-password"

   # Genera una chiave JWT casuale per lo sviluppo locale
   $bytes = New-Object byte[] 32
   [System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
   $env:MEEVY_JWT_SECRET_BASE64 = [Convert]::ToBase64String($bytes)
   ```

4. Avvia il backend dalla stessa sessione del terminale:
   ```bash
   mvn spring-boot:run
   ```

Il server parte normalmente su `http://localhost:8080`. **Flyway** applica automaticamente le migration al database durante l'avvio. Non inserire credenziali o chiavi JWT nel repository.

Se avvii l'app da IntelliJ, imposta le stesse variabili nella *Run Configuration*: quelle definite nel terminale non vengono passate automaticamente all'IDE.

## API disponibili

Gli endpoint riportati sotto sono quelli attualmente presenti nel progetto. I percorsi sono relativi a `http://localhost:8080`.

### Utenti e autenticazione

| Metodo | Endpoint | Funzione |
| --- | --- | --- |
| `POST` | `/user/register` | Registrazione utente |
| `POST` | `/auth/login` | Login, restituisce access token e refresh token |
| `POST` | `/auth/refresh` | Rinnova i token |
| `POST` | `/auth/logout` | Revoca il refresh token |

La registrazione richiede `email` e `password` (minimo 8 caratteri); `firstName` e `lastName` sono facoltativi. Login: `email` e `password`. Refresh e logout: `refreshToken`.

### Materie personali

| Metodo | Endpoint | Funzione |
| --- | --- | --- |
| `POST` | `/subject` | Crea una materia |
| `GET` | `/subject` | Elenca le proprie materie |
| `GET` | `/subject/{id}` | Recupera una materia |
| `PUT` | `/subject/{id}` | Modifica una materia |
| `DELETE` | `/subject/{id}` | Elimina una materia |

Per creare o modificare una materia, il body JSON contiene il campo `name`.

### Pomodoro personale

| Metodo | Endpoint | Funzione |
| --- | --- | --- |
| `POST` | `/pomodoro-personal-sessions` | Crea una sessione in stato READY |
| `PATCH` | `/pomodoro-personal-sessions/{id}/update` | Modifica una sessione READY |
| `POST` | `/pomodoro-personal-sessions/{id}/start` | Avvia |
| `POST` | `/pomodoro-personal-sessions/{id}/pause` | Mette in pausa |
| `POST` | `/pomodoro-personal-sessions/{id}/resume` | Riprende |
| `POST` | `/pomodoro-personal-sessions/{id}/complete` | Completa |
| `POST` | `/pomodoro-personal-sessions/{id}/cancel` | Annulla |
| `GET` | `/pomodoro-personal-sessions/current` | Recupera la sessione attiva (204 se assente) |
| `GET` | `/pomodoro-personal-sessions/{id}` | Recupera una sessione |
| `GET` | `/pomodoro-personal-sessions?page=0&size=20` | Storico paginato |

Per creare o modificare una sessione, il body è:

```json
{
  "subjectId": 1,
  "studyDurationSeconds": 1500,
  "breakDurationSeconds": 300,
  "targetDurationSeconds": 10800
}
```

Le durate sono in **secondi** e la materia deve appartenere all'utente autenticato. Il Pomodoro conserva gli stati e i tempi sul server; il progresso viene calcolato dagli istanti registrati, senza un timer attivo lato server.

**Autenticazione:** in modalità normale, registrazione, login, refresh e logout sono pubblici; per gli altri endpoint serve l'header `Authorization: Bearer <accessToken>`. Per i body JSON usare `Content-Type: application/json`.

## Test

Con PostgreSQL avviato e le variabili d'ambiente impostate:

```bash
mvn clean verify
```

Il report di coverage viene generato in `target/site/jacoco/index.html`. Alcuni test sono d'integrazione e si collegano al PostgreSQL configurato: eseguirli su un database locale di sviluppo.

Il progetto è ancora in fase di sviluppo: questo README descrive solo le API già presenti nel repository.
