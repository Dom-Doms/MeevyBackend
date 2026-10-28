# MeevyBackend — Docker su server domestico (branch deploy/docker)

Configurazione basata sugli stack esistenti del server `/home/dom`.

## 1. Preparazione del repository da Windows

- Mantieni `main` per lo sviluppo Java/Flyway, e `deploy/docker` per i file di deploy.
- Correggi **su `main`** il bug in `SecurityConfig.java`: il matcher POST deve essere `/user/register`, non `/users/register`. Verifica i test, crea il commit e fai push su `main`.
- Integra poi `main` nel branch `deploy/docker` (`git switch deploy/docker`, `git merge main`).
- Copia nella radice del repository i file di questo pacchetto (al fianco di `pom.xml`).
- Assicurati che `.gitignore` contenga `.env` (già presente nel progetto attuale); **non committare mai `.env`**.
- Verifica `git diff --cached` prima del commit. Esegui `git add Dockerfile compose.yaml .dockerignore .env.example deploy.sh DEPLOYMENT.md`, `git update-index --chmod=+x deploy.sh`, poi commit e `git push -u origin deploy/docker`.
- Le terminazioni di riga di `deploy.sh` devono essere LF, non CRLF. Su Windows verifica con l'editor o configura un attributo Git specifico per gli `.sh`.

## 2. Primo avvio sul server Ubuntu

Solo quando `deploy/docker` è pubblicato su GitHub:

```bash
cd /home/dom
git clone --branch deploy/docker --single-branch \
  https://github.com/Dom-Doms/MeevyBackend.git meevy
cd /home/dom/meevy

# Genera segreti solo sul server. Non mostrarli o incollarli nelle chat.
umask 077
printf 'DB_USERNAME=meevy\nDB_PASSWORD=%s\nMEEVY_JWT_SECRET_BASE64=%s\n' \
  "$(openssl rand -hex 32)" "$(openssl rand -base64 32)" > .env
chmod 600 .env

docker compose config --quiet
docker compose up -d --build
docker compose ps
docker compose logs --tail=100 backend
curl -i http://172.17.0.1:8092/subject
```

Per `/subject` senza JWT è atteso **401 Unauthorized** quando la security è attiva; un 401, da solo, non garantisce che tutte le API funzionino. Controlla anche i log Flyway e che il backend non si riavvii.

## 3. Reverse proxy e DNS, solo dopo il test locale

- In Nginx Proxy Manager, crea un proxy host per `api-meevy.gesu.gay` verso `http://172.17.0.1:8092`, e configura il certificato HTTPS.
- NPM è connesso alla rete Docker `bridge`; l'IP del gateway `172.17.0.1` è stato verificato in precedenza. Controlla di nuovo il gateway se cambia l'infrastruttura.
- Lo script Cloudflare DDNS già esistente è `/home/dom/cloudflare-ddns.sh`: NON crearne un duplicato. Valuta un CNAME `api-meevy` verso `gesu.gay` dopo aver verificato la configurazione DNS corrente.
- Il DB non espone la porta 5432; il backend è pubblicato solo sull'indirizzo del bridge Docker. Non aggiungere regole di port-forwarding del router per la porta 8092.

## 4. Aggiornamenti

Quando una feature su `main` è pronta per il server, da Windows:

```powershell
git switch deploy/docker
git merge main
git push origin deploy/docker
```

Poi sul server:

```bash
cd /home/dom/meevy
bash deploy.sh
```

Lo script controlla il branch, effettua pull solo fast-forward, valida Compose, ricostruisce Meevy e mostra lo stato dei container. Non tocca gli stack `dnd-manager`, `lifeplanner` o `transit-mixed-lab`.

## 5. Note di manutenzione

- Backup di `.env` e volume Docker `meevy_postgres_data` in posizione protetta; prima di migrazioni Flyway potenzialmente distruttive esegui backup e pianifica il rollback.
- Non usare `docker compose down -v` sul server: eliminerebbe il volume del database Meevy.
- Il profilo Spring `prod` attiva `SecurityConfig` / `JwtCurrentUserIdProvider` e disattiva le classi annotate `@Profile("dev")`.
- Java 21 è coerente con il `pom.xml` corrente, mentre LifePlanner usa Java 17: i progetti possono convivere in container separati.
- `mvn -DskipTests package` nel Dockerfile non esegue i test: esegui `mvn verify` prima di pubblicare il branch.
