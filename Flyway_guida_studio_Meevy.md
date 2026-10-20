# Flyway — guida di studio per il progetto Mivy

> Obiettivo: capire Flyway abbastanza bene da **usarlo consapevolmente nel progetto** e da saper spiegare al professore **che problema risolve, come funziona e perché lo abbiamo scelto**.
>
> Questa guida parte molto semplice e diventa progressivamente più tecnica.
>
> Documentazione ufficiale verificata: **15 ottobre 2026**.

---

# 1. Flyway spiegato in modo semplicissimo

## 1.1 Il problema

Immagina di avere un database PostgreSQL.

All'inizio hai solo:

- `users`
- `groups`

Poi durante lo sviluppo aggiungi:

- una colonna a `users`;
- una tabella `reports`;
- una tabella `files`;
- una foreign key;
- un indice;
- un nuovo vincolo.

Il problema è questo:

**come facciamo a essere sicuri che il database di Dom, quello di Rita, quello dei test e quello usato nella demo abbiano tutti esattamente la stessa struttura?**

Se ognuno modifica il database manualmente tramite pgAdmin o comandi SQL, prima o poi succede questo:

> "A me funziona."

> "A me invece manca una colonna."

> "Io quella tabella l'avevo già creata."

> "Sul server la foreign key non esiste."

Flyway serve principalmente a evitare questo problema.

---

## 1.2 Cos'è Flyway

**Flyway è uno strumento di database migration.**

Una *migration* è un cambiamento versionato del database.

Invece di modificare il database manualmente, scriviamo un file che descrive la modifica.

Esempio concettuale:

```text
V1  -> creo le tabelle iniziali
V2  -> aggiungo la tabella reports
V3  -> aggiungo una colonna a users
V4  -> creo un indice
```

Flyway:

1. trova questi file;
2. controlla quali sono già stati eseguiti;
3. esegue quelli mancanti;
4. registra ciò che ha fatto.

Quindi il database evolve in maniera **riproducibile e controllata**.

---

# 2. L'idea fondamentale: versionare anche il database

Con Git versioniamo il codice Java.

Con Flyway versioniamo **l'evoluzione dello schema del database**.

Non significa che Flyway salva una copia completa del database a ogni modifica.

Salva invece la **sequenza delle trasformazioni** che portano il database:

```text
versione 1
   ↓
versione 2
   ↓
versione 3
   ↓
versione 4
```

Questo approccio viene chiamato **database migration**.

---

# 3. Esempio molto semplice

Supponiamo di partire da zero.

Creiamo:

```text
V1__create_users.sql
```

che contiene, ad esempio:

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL
);
```

Successivamente scopriamo che ci serve l'email.

NON modifichiamo `V1`.

Creiamo:

```text
V2__add_email_to_users.sql
```

contenente:

```sql
ALTER TABLE users
ADD COLUMN email VARCHAR(255);
```

Il database ha quindi una storia:

```text
V1 -> creazione users
V2 -> aggiunta email
```

Se Rita avvia l'applicazione con un database fermo a V1, Flyway vede che manca V2 e la applica.

---

# 4. Perché è utile nel nostro progetto

Nel progetto Mivy Flyway ci permette di avere:

- schema PostgreSQL riproducibile;
- stessa struttura database per entrambi gli sviluppatori;
- database dei test coerente;
- aggiornamenti controllati;
- storia delle modifiche;
- integrazione con Git;
- minore rischio di modifiche manuali dimenticate.

Il punto importante da dire all'orale è:

> **Flyway non serve principalmente a creare tabelle più comodamente. Serve a controllare e versionare l'evoluzione del database.**

---

# 5. Cosa succede quando parte l'applicazione

Nel nostro caso Flyway è usato insieme a Spring Boot.

Concettualmente all'avvio:

```text
Spring Boot parte
      ↓
viene configurato il DataSource PostgreSQL
      ↓
Flyway controlla il database
      ↓
legge le migration disponibili
      ↓
confronta le migration con quelle già applicate
      ↓
esegue quelle mancanti
      ↓
l'applicazione usa il database aggiornato
```

Spring Boot può eseguire automaticamente `Flyway.migrate()` durante l'avvio dell'applicazione quando Flyway è configurato nel progetto.

---

# 6. Dove stanno normalmente le migration

Con Spring Boot la posizione standard è:

```text
src/main/resources/db/migration/
```

Esempio:

```text
src/
└── main/
    └── resources/
        └── db/
            └── migration/
                ├── V1__initial_schema.sql
                ├── V2__create_reports.sql
                └── V3__add_file_metadata.sql
```

La posizione è configurabile, ma per un progetto come il nostro non c'è motivo di cambiarla senza una necessità reale.

---

# 7. Come si chiamano i file

La convenzione più importante è:

```text
V<versione>__<descrizione>.sql
```

Esempi:

```text
V1__initial_schema.sql
V2__create_reports.sql
V3__add_group_roles.sql
V4__create_file_metadata.sql
V5__add_report_status.sql
```

Attenzione:

```text
V3__add_report_status.sql
```

contiene **due underscore** tra versione e descrizione.

---

# 8. Versioned Migration

Le migration che iniziano con `V` sono chiamate **Versioned Migrations**.

Esempio:

```text
V1__initial_schema.sql
V2__add_reports.sql
V3__add_report_status.sql
```

Hanno una versione precisa.

Flyway le esegue in ordine di versione.

Una migration versionata viene normalmente applicata **una sola volta** a un determinato database.

---

# 9. La tabella `flyway_schema_history`

Questa è una delle cose più importanti da conoscere.

Quando Flyway viene utilizzato, mantiene nel database una tabella chiamata normalmente:

```text
flyway_schema_history
```

Serve a ricordare quali migration sono state applicate.

Concettualmente può contenere informazioni come:

| Version | Description | Script | Checksum | Success |
|---|---|---|---|---|
| 1 | initial schema | V1__initial_schema.sql | ... | true |
| 2 | create reports | V2__create_reports.sql | ... | true |
| 3 | add status | V3__add_status.sql | ... | true |

Quando Flyway parte confronta:

```text
migration presenti nel progetto
```

con:

```text
migration registrate in flyway_schema_history
```

e capisce quali devono ancora essere eseguite.

---

# 10. Il checksum

Quando una migration viene applicata, Flyway ne memorizza anche un **checksum**.

Il checksum è, semplificando, un'impronta del contenuto del file.

Esempio:

```text
V2__create_reports.sql
```

viene eseguito.

Flyway salva:

```text
versione = 2
checksum = XYZ
```

Successivamente qualcuno modifica il file.

Ora il checksum diventa:

```text
ABC
```

Flyway può rilevare che:

```text
checksum salvato != checksum attuale
```

e quindi capisce che una migration già applicata è stata modificata.

Questo è fondamentale per garantire che la storia del database rimanga coerente.

---

# 11. Regola fondamentale: non modificare migration già applicate

Una delle regole più importanti nell'uso di Flyway è:

> **Una migration già applicata e condivisa non dovrebbe essere modificata.**

Esempio.

Abbiamo già eseguito:

```text
V1
V2
V3
```

Poi scopriamo che V2 contiene una scelta che vogliamo correggere.

Non facciamo:

```text
modifica V2
```

ma creiamo:

```text
V4__correct_something.sql
```

Perché?

Perché V2 potrebbe essere già stata applicata:

- sul computer di Dom;
- sul computer di Rita;
- nel database di test;
- sul server.

La migration rappresenta **un evento storico**, non semplicemente lo stato attuale del database.

---

# 12. `migrate`

Il comando concettualmente più importante è:

```text
flyway migrate
```

`migrate`:

1. legge le migration disponibili;
2. legge `flyway_schema_history`;
3. individua le migration pending;
4. le esegue nell'ordine corretto;
5. aggiorna la history.

Esempio:

Database:

```text
V1
V2
```

Progetto:

```text
V1
V2
V3
V4
```

Dopo `migrate`:

```text
V1
V2
V3
V4
```

---

# 13. Migration pending

Una migration è **pending** quando:

- esiste nel progetto;
- non è ancora stata applicata al database.

Esempio:

```text
V1 -> Applied
V2 -> Applied
V3 -> Pending
```

Alla successiva migrazione Flyway eseguirà V3.

---

# 14. `validate`

`validate` serve a controllare che la storia delle migration sia coerente.

In particolare Flyway può confrontare:

- versioni;
- nomi;
- tipi;
- checksum;
- migration applicate;
- migration disponibili.

Uno dei casi classici:

```text
V2 applicata al database
```

ma qualcuno modifica successivamente il file V2.

Il checksum cambia.

La validazione può fallire.

Questa è una protezione contro modifiche retroattive della storia del database.

---

# 15. `repair`

`repair` modifica/ripara i metadata della schema history.

Può, tra le altre cose:

- riallineare checksum;
- sistemare metadata nella history;
- rimuovere dalla history determinate migration fallite;
- marcare migration mancanti come eliminate.

## ATTENZIONE

`repair` non deve diventare:

> "Flyway si lamenta → lancio repair."

Prima bisogna capire **perché** la history è incoerente.

Per esempio:

```text
checksum mismatch
```

potrebbe significare che qualcuno ha modificato una migration già applicata.

In quel caso il vero problema è la gestione della migration, non Flyway.

All'orale è molto meglio dire:

> "Repair è uno strumento amministrativo da usare consapevolmente; non sostituisce una corretta disciplina delle migration."

---

# 16. `baseline`

`baseline` serve soprattutto quando Flyway viene introdotto su un database **che esiste già**.

Immaginiamo:

```text
database già pieno di tabelle
```

ma Flyway non è mai stato utilizzato.

Non possiamo semplicemente fingere che il database sia vuoto.

La baseline comunica a Flyway:

> "Considera questo stato come punto di partenza della storia gestita da Flyway."

Per un progetto nuovo (*greenfield*) nel quale Flyway viene utilizzato fin dall'inizio, normalmente questo problema non esiste.

---

# 17. `baseline` e Baseline Migration NON sono la stessa cosa

Nelle versioni moderne di Flyway esistono anche le **Baseline Migrations**, ad esempio:

```text
B5__database_state.sql
```

Sono una funzionalità distinta dal comando `baseline`.

Servono soprattutto in progetti con una storia molto lunga, per evitare che un nuovo ambiente debba rieseguire centinaia di vecchie migration.

Per Mivy, almeno nella fase attuale, è molto probabilmente una complessità inutile.

Da sapere:

```text
baseline command       !=      B-version baseline migration
```

---

# 18. Repeatable Migration

Oltre alle versioned migration esistono le **Repeatable Migrations**.

Il nome tipico è:

```text
R__nome.sql
```

Non hanno numero di versione.

Esempi:

```text
R__create_views.sql
R__refresh_reference_data.sql
```

Flyway può rieseguirle quando cambia il loro contenuto/checksum.

Sono utili, ad esempio, per:

- view;
- stored procedure;
- funzioni;
- determinati dati di riferimento.

Per Mivy non dobbiamo usarle solo perché esistono.

Se non abbiamo un caso reale, le normali versioned migration sono sufficienti.

---

# 19. Versioned vs Repeatable

| Versioned | Repeatable |
|---|---|
| prefisso `V` | prefisso `R` |
| ha una versione | non ha versione |
| applicata normalmente una volta | può essere rieseguita |
| rappresenta un cambiamento storico | rappresenta qualcosa da mantenere aggiornato |
| es. ALTER TABLE | es. CREATE OR REPLACE VIEW |

---

# 20. Cosa può contenere una migration

Una migration SQL può modificare sia:

## DDL — Data Definition Language

Struttura del database:

```sql
CREATE TABLE ...
ALTER TABLE ...
DROP TABLE ...
CREATE INDEX ...
```

## DML — Data Manipulation Language

Dati:

```sql
INSERT ...
UPDATE ...
DELETE ...
```

Quindi Flyway non gestisce solo le tabelle.

Può versionare anche trasformazioni dei dati necessarie quando il modello evolve.

---

# 21. Flyway e PostgreSQL

Nel nostro progetto PostgreSQL è il DBMS.

Flyway:

- si collega al database;
- utilizza il driver JDBC;
- invia i comandi SQL delle migration;
- registra il risultato nella schema history.

Flyway non sostituisce PostgreSQL.

I ruoli sono diversi:

```text
PostgreSQL
    = database

Flyway
    = gestore dell'evoluzione/versionamento del database
```

---

# 22. Flyway e Spring Boot

Anche qui i ruoli sono diversi.

```text
Spring Boot
    = framework applicativo

Flyway
    = database migration tool

PostgreSQL
    = DBMS
```

Spring Boot può configurare e avviare Flyway automaticamente.

Questo è comodo, ma è importante comprendere che:

> **Flyway esiste indipendentemente da Spring Boot.**

Può essere utilizzato anche:

- da command line;
- tramite Maven;
- tramite Gradle;
- tramite API;
- in pipeline CI/CD.

---

# 23. Flyway e JPA/Hibernate non sono la stessa cosa

Questo è probabilmente uno dei punti più interessanti per l'orale.

Hibernate può generare/modificare lo schema partendo dalle entity Java.

Ad esempio tramite configurazioni come:

```text
ddl-auto=create
ddl-auto=update
```

Flyway usa invece migration **esplicite e versionate**.

Differenza concettuale:

```text
Hibernate schema generation
    Entity Java
       ↓
    schema generato automaticamente
```

contro:

```text
Flyway
    migration SQL esplicite
       ↓
    evoluzione controllata del database
```

---

# 24. Perché Flyway è preferibile per uno schema serio

`ddl-auto=update` può essere comodo durante piccoli esperimenti, ma non rappresenta una buona strategia di versionamento del database.

Con Flyway sappiamo:

- quale modifica è stata fatta;
- quando;
- in quale ordine;
- quale script l'ha prodotta;
- se è già stata applicata;
- se qualcuno ha alterato retroattivamente lo script.

Quindi nel progetto serio:

```text
Entity JPA
    = modello Java / mapping ORM

Flyway
    = evoluzione controllata del database
```

Sono strumenti complementari.

---

# 25. Hibernate `validate`

Una configurazione concettualmente interessante è usare Hibernate per **validare** che le entity siano compatibili con lo schema, lasciando a Flyway la responsabilità di modificarlo.

Il modello mentale è:

```text
Flyway
    modifica lo schema

Hibernate
    controlla/mappa lo schema
```

invece di:

```text
Hibernate
    modifica automaticamente lo schema
```

Questo separa meglio le responsabilità.

La configurazione concreta va naturalmente scelta in base al nostro progetto e ambiente.

---

# 26. Perché non basta tenere un `schema.sql`

Potremmo avere un unico file:

```text
schema.sql
```

che rappresenta lo stato attuale.

Ma questo dice solo:

> "Il database oggi deve essere così."

Non racconta necessariamente:

> "Come siamo arrivati fino a qui?"

Con le migration abbiamo invece:

```text
V1
V2
V3
V4
...
```

e quindi una storia incrementale.

Questo permette di aggiornare anche database già esistenti senza ricrearli da zero.

---

# 27. Esempio realistico di evoluzione Mivy

Immaginiamo:

### V1

```text
users
groups
group_memberships
```

### V2

introduciamo:

```text
reports
```

### V3

aggiungiamo gli stati:

```text
OPEN
IN_REVIEW
RESOLVED
REJECTED
ESCALATED
```

### V4

introduciamo metadata dei file:

```text
stored_files
```

### V5

aggiungiamo una nuova foreign key.

Un database nuovo esegue:

```text
V1 -> V2 -> V3 -> V4 -> V5
```

Un database già aggiornato fino a V3 esegue solo:

```text
V4 -> V5
```

---

# 28. Cosa succede se una migration fallisce

Supponiamo:

```text
V7__something.sql
```

contenga un errore SQL.

Flyway interrompe la migrazione e segnala l'errore.

A seconda del DBMS e delle operazioni coinvolte, la migration può essere gestita transazionalmente oppure possono esserci effetti parziali da controllare.

Il punto importante è:

> **Una migration fallita va analizzata. Non bisogna semplicemente modificare metadata finché Flyway smette di lamentarsi.**

Bisogna capire:

1. quale statement è fallito;
2. se il DB è stato modificato parzialmente;
3. qual è lo stato effettivo dello schema;
4. come ripristinare una situazione coerente.

---

# 29. Transazioni

Quando il database supporta transazioni per le operazioni coinvolte, Flyway può eseguire migration in maniera transazionale.

Concettualmente:

```text
BEGIN

ALTER ...
CREATE ...
UPDATE ...

COMMIT
```

Se qualcosa fallisce:

```text
ROLLBACK
```

PostgreSQL offre un buon supporto transazionale anche per molte operazioni DDL.

Ma non bisogna assumere che **ogni possibile operazione di ogni DBMS** sia sempre rollbackabile nello stesso modo.

---

# 30. Migration idempotente: serve?

Normalmente una versioned migration non deve essere progettata pensando:

> "deve poter essere eseguita dieci volte."

Flyway impedisce normalmente di rieseguire una versioned migration già applicata proprio grazie alla schema history.

Quindi il controllo principale è:

```text
Flyway history
```

non l'inserimento indiscriminato di:

```sql
IF NOT EXISTS
```

ovunque.

L'idempotenza può essere utile in specifiche situazioni, ma non sostituisce il versionamento delle migration.

---

# 31. Git e Flyway

Le migration devono essere versionate insieme al codice.

Esempio:

```text
commit X
 ├── modifica Entity Report
 ├── modifica repository/service
 └── V8__add_report_reason.sql
```

Così codice e schema evolvono insieme.

---

# 32. Problema con due sviluppatori

Dom crea:

```text
V10__create_a.sql
```

Rita, sul proprio branch, crea:

```text
V10__create_b.sql
```

Al merge abbiamo un conflitto logico:

```text
due migration con la stessa versione
```

Non significa che Flyway sia rotto.

Significa che dobbiamo coordinare la numerazione.

Una delle due migration dovrà diventare, ad esempio:

```text
V11__create_b.sql
```

prima di essere utilizzata negli ambienti condivisi.

---

# 33. Perché una migration è praticamente "immutabile"

Pensala come un commit Git già pubblicato e utilizzato da altre persone.

Modificarla dopo che è stata applicata significa riscrivere la storia.

Flyway usa checksum proprio per rendere visibili queste incoerenze.

Regola pratica:

```text
migration locale non ancora condivisa
    -> può eventualmente essere corretta

migration già applicata/condivisa
    -> nuova migration
```

Nel dubbio, per un progetto di gruppo è più sicuro creare una nuova migration.

---

# 34. `clean`

Flyway possiede anche funzionalità di pulizia dell'ambiente, ma bisogna distinguere nettamente:

```text
ambiente locale/test
```

da:

```text
database con dati importanti
```

Distruggere lo schema per ricostruirlo può essere accettabile in un database di sviluppo usa-e-getta.

Non è una strategia di aggiornamento per un database reale.

Da ricordare:

> Le migration servono proprio a permettere l'evoluzione senza dover cancellare tutto.

---

# 35. Schema history: informazioni concettualmente importanti

La history permette a Flyway di conoscere:

- versione;
- descrizione;
- tipo della migration;
- script;
- checksum;
- ordine di installazione;
- data/tempo di esecuzione;
- esito.

Non serve imparare a memoria ogni colonna della tabella.

Serve capire **a cosa serve**.

---

# 36. Come Flyway decide cosa eseguire

Versione semplificata dell'algoritmo:

```text
1. trova le migration
2. legge la schema history
3. confronta migration disponibili e applicate
4. valida la coerenza
5. identifica quelle pending
6. le ordina
7. le esegue
8. registra il risultato
```

---

# 37. Perché il nome del file conta

Flyway usa una convenzione di naming per riconoscere:

- tipo della migration;
- versione;
- descrizione.

Esempio:

```text
V12__add_group_invitation_token.sql
│ │   │
│ │   └─ descrizione
│ └───── versione
└─────── versioned migration
```

Un file nominato male può non essere interpretato come migration valida in base alla configurazione.

Flyway dispone anche di un'opzione per rendere più severa la validazione dei nomi.

---

# 38. Versioni: non devono per forza essere solo interi

Sono possibili versioni articolate, ad esempio:

```text
V1
V2
V2_1
V3
```

Per Mivy, però, conviene probabilmente mantenere una numerazione semplice se non abbiamo una ragione per complicarla:

```text
V1
V2
V3
V4
...
```

La semplicità riduce errori e rende la storia più leggibile.

---

# 39. Cosa NON fa Flyway

Flyway non è:

- un ORM;
- un DBMS;
- un sostituto di PostgreSQL;
- un repository;
- JPA;
- Hibernate;
- un sistema di backup;
- Git;
- un tool di query per utenti finali.

Flyway gestisce principalmente:

> **la versione e l'evoluzione del database tramite migration controllate.**

---

# 40. Flyway non è un Design Pattern

Nel nostro progetto dobbiamo distinguere:

```text
tecnologia / tool
```

da:

```text
design pattern
```

Flyway è una tecnologia/tool.

NON conta come uno dei design pattern richiesti dal professore.

Analogamente, il fatto che Spring Boot integri Flyway automaticamente non costituisce un pattern progettato da noi.

---

# 41. Flyway non sostituisce la progettazione del database

Usare Flyway non rende automaticamente buono uno schema.

Possiamo avere:

```text
migration perfettamente versionate
```

ma:

```text
schema progettato male
```

Flyway garantisce principalmente **controllo dell'evoluzione**, non qualità del modello dati.

La progettazione di:

- tabelle;
- relazioni;
- cardinalità;
- foreign key;
- vincoli;
- indici;

resta responsabilità nostra.

---

# 42. Perché è coerente con il nostro progetto

Mivy è un'applicazione client/server con backend Spring Boot e PostgreSQL.

Il database contiene informazioni strutturali importanti come:

- utenti;
- gruppi;
- ruoli;
- membership;
- piani di studio;
- sessioni;
- file e metadata;
- segnalazioni;
- inviti;
- altre entità del dominio.

Lo schema evolverà mentre sviluppiamo.

Per questo Flyway risolve un problema reale:

> mantenere controllata e riproducibile l'evoluzione del database tra sviluppatori e ambienti.

Non è una tecnologia aggiunta "per impressionare".

---

# 43. Workflow consigliato

Quando serve cambiare il database:

```text
1. capiamo la modifica richiesta
2. progettiamo la modifica allo schema
3. creiamo una nuova migration
4. eseguiamo/testiamo la migration
5. controlliamo che l'applicazione funzioni
6. committiamo migration + modifiche correlate
```

E NON:

```text
1. apro pgAdmin
2. modifico una tabella a mano
3. dimentico cosa ho fatto
4. sul PC dell'altro non funziona
```

---

# 44. Modifiche manuali al database

Durante debug/esplorazione può capitare di interrogare il DB manualmente.

Ma una modifica strutturale necessaria al funzionamento dell'applicazione deve essere rappresentata da una migration.

Altrimenti nasce **schema drift**:

```text
schema reale
    !=
schema previsto dalle migration
```

---

# 45. Schema drift

Per *drift* si intende una divergenza tra lo stato previsto e quello effettivo del database.

Esempio:

Migration ufficiali prevedono:

```text
users
reports
```

ma qualcuno modifica manualmente il server aggiungendo:

```text
temporary_column
```

Quella modifica non esiste nella storia versionata.

Il database è quindi "derivato" dallo stato previsto.

Questo è uno dei motivi per cui le modifiche manuali agli ambienti condivisi sono pericolose.

---

# 46. Test e Flyway

Flyway è utile anche nei test di integrazione.

Possiamo creare un database pulito e lasciare che le migration lo portino allo schema necessario.

Questo verifica indirettamente che:

- le migration funzionino da zero;
- lo schema sia riproducibile;
- l'applicazione non dipenda da modifiche manuali dimenticate.

È particolarmente utile se utilizziamo database temporanei/containerizzati durante i test.

---

# 47. Cosa testare

Non ha senso scrivere unit test per dimostrare che "Flyway funziona".

Flyway è una libreria esterna.

Ha invece senso verificare:

- che una nuova istanza del DB possa essere migrata;
- che l'applicazione parta correttamente;
- che mapping JPA e schema siano compatibili;
- che vincoli e relazioni importanti siano effettivamente presenti;
- che migration che trasformano dati producano il risultato previsto.

---

# 48. Flyway e `ddl-auto`

Domanda da orale molto probabile:

## "Perché usate Flyway se avete Hibernate?"

Risposta:

> Hibernate/JPA gestisce il mapping tra oggetti Java e database. Flyway gestisce invece l'evoluzione versionata dello schema. Evitiamo di affidare l'evoluzione di un database condiviso a modifiche automatiche non versionate; preferiamo migration esplicite, riproducibili e tracciate in Git.

---

# 49. Perché non Liquibase?

Flyway e Liquibase risolvono problemi simili.

In maniera molto semplificata:

### Flyway

molto naturale quando si preferiscono migration SQL sequenziali e semplici.

### Liquibase

offre un changelog più ricco e può utilizzare formati come XML, YAML, JSON e SQL.

Non dobbiamo sostenere che uno sia "sempre migliore".

Per Mivy la giustificazione può essere:

> "Avevamo bisogno di versionare uno schema PostgreSQL in modo semplice e trasparente. Flyway si integra bene con Spring Boot e il modello delle migration SQL versionate è sufficiente per la complessità del nostro progetto."

Questa è una motivazione migliore di:

> "Abbiamo usato Flyway perché lo usano tutti."

---

# 50. Cosa dobbiamo saper spiegare davvero all'orale

## Livello essenziale

Dobbiamo sapere:

- cos'è una database migration;
- che problema risolve Flyway;
- perché non modifichiamo il DB soltanto a mano;
- convenzione `V1__...sql`;
- cos'è `flyway_schema_history`;
- cos'è un checksum;
- cosa fa `migrate`;
- cosa fa `validate`;
- perché non si modifica una migration già applicata;
- differenza tra Flyway e Hibernate;
- differenza tra Flyway e PostgreSQL.

## Livello buono

In più:

- repeatable migration;
- baseline;
- comportamento in caso di errore;
- transazioni;
- drift;
- uso con Git;
- conflitti di versione tra branch;
- ruolo di `repair`.

## Livello avanzato ma non prioritario

Solo sapere che esistono:

- Java migrations;
- callbacks;
- baseline migrations `B...`;
- configurazioni multi-schema;
- pipeline/CI;
- placeholders;
- funzionalità enterprise avanzate.

Non dobbiamo introdurle nel progetto senza una necessità reale.

---

# 51. Domande da orale

## D1 — Cos'è Flyway?

**Risposta breve**

Flyway è uno strumento di database migration che permette di versionare e applicare in ordine controllato le modifiche allo schema e ai dati del database.

---

## D2 — Quale problema risolve?

Evita che ambienti diversi abbiano schemi diversi e permette di ricostruire o aggiornare il database attraverso una sequenza riproducibile di migration versionate.

---

## D3 — Come sa quali migration sono già state eseguite?

Mantiene una tabella di history, normalmente chiamata `flyway_schema_history`, nella quale registra le migration applicate e i relativi metadata.

---

## D4 — Cos'è il checksum?

È un'impronta calcolata sul contenuto della migration. Permette a Flyway di rilevare se una migration già applicata è stata modificata successivamente.

---

## D5 — Posso modificare `V3` dopo averla applicata?

Tecnicamente il file può essere modificato, ma è una pratica da evitare una volta che la migration è stata applicata/condivisa. Il checksum non corrisponderebbe più alla history. La soluzione corretta è normalmente creare una nuova migration.

---

## D6 — Che differenza c'è tra Flyway e Hibernate?

Hibernate/JPA gestisce principalmente il mapping object-relational tra classi Java e database. Flyway gestisce l'evoluzione versionata dello schema attraverso migration esplicite.

---

## D7 — Perché non usate `ddl-auto=update`?

Perché vogliamo che le modifiche al database siano esplicite, versionate, riproducibili e tracciate. In un progetto condiviso una strategia automatica di update dello schema offre molto meno controllo sulla storia delle modifiche.

---

## D8 — Cosa succede all'avvio?

Spring Boot configura Flyway, Flyway legge le migration e la propria schema history, verifica quali sono ancora pending e applica quelle necessarie prima che l'applicazione utilizzi lo schema aggiornato.

---

## D9 — Cosa fa `validate`?

Controlla che le migration disponibili siano coerenti con quelle registrate nella history, verificando elementi come versioni, nomi, tipi e checksum.

---

## D10 — Cosa fa `repair`?

Ripara i metadata della schema history, ad esempio riallineando checksum o gestendo determinati record di migration fallite/mancanti. Deve essere usato solo dopo aver compreso la causa dell'incoerenza.

---

## D11 — Cos'è una repeatable migration?

Una migration senza versione, tipicamente `R__...sql`, che può essere rieseguita quando cambia il suo contenuto. È utile per elementi come view o procedure, quando necessario.

---

## D12 — Cos'è una baseline?

È il modo di dire a Flyway che un database già esistente rappresenta il punto iniziale da cui cominciare a tracciare le migration.

---

## D13 — Cosa succede se due sviluppatori creano entrambi `V12`?

Nasce un conflitto di versionamento. Prima di integrare le modifiche bisogna rinumerare/coordinare una delle migration in modo che la sequenza sia univoca.

---

## D14 — Flyway è un design pattern?

No. È uno strumento/libreria di database migration. Non può essere considerato uno dei design pattern richiesti nel progetto.

---

# 52. Esempio di risposta completa da orale

> "Nel progetto utilizziamo Flyway per gestire l'evoluzione dello schema PostgreSQL. Ogni modifica strutturale viene rappresentata da una migration SQL versionata e mantenuta in Git. Flyway confronta le migration presenti nel progetto con quelle registrate nella tabella `flyway_schema_history` e applica soltanto quelle ancora pending. Mantiene inoltre un checksum delle migration, quindi può rilevare modifiche retroattive a script già applicati. Per questo, quando lo schema deve evolvere, non modifichiamo normalmente una vecchia migration già condivisa ma ne aggiungiamo una nuova. JPA/Hibernate continua a occuparsi del mapping tra oggetti e tabelle, mentre Flyway ha la responsabilità distinta di versionare l'evoluzione del database."

Se sappiamo spiegare bene questa risposta e i concetti che contiene, abbiamo capito la parte realmente importante.

---

# 53. Errori concettuali da evitare all'orale

❌ "Flyway crea il database."

Meglio:

✅ "Flyway applica migration al database e ne gestisce la storia."

---

❌ "Flyway è una funzione di Spring."

Meglio:

✅ "Flyway è uno strumento indipendente che Spring Boot può integrare e configurare automaticamente."

---

❌ "Hibernate e Flyway fanno la stessa cosa."

Meglio:

✅ "Hibernate gestisce l'ORM; Flyway gestisce la migrazione/versionamento del database."

---

❌ "Se una migration non funziona facciamo repair."

Meglio:

✅ "Prima analizziamo la causa e lo stato reale del database; repair agisce sulla history e va usato consapevolmente."

---

❌ "Se devo cambiare una tabella modifico V1."

Meglio:

✅ "Se V1 è già stata applicata/condivisa aggiungo una nuova migration."

---

❌ "Flyway è uno dei nostri pattern."

✅ No. È una tecnologia.

---

# 54. Mini glossario

### Migration

Script che rappresenta una modifica del database.

### Versioned migration

Migration con versione, ad esempio:

```text
V4__create_reports.sql
```

### Repeatable migration

Migration rieseguibile quando cambia:

```text
R__create_views.sql
```

### Pending

Migration disponibile ma non ancora applicata.

### Applied

Migration già eseguita.

### Schema

Struttura logica del database.

### Schema history

Tabella con la storia delle migration.

### Checksum

Impronta del contenuto di una migration.

### Validate

Verifica la coerenza tra migration disponibili e applicate.

### Migrate

Applica le migration necessarie.

### Repair

Ripara/allinea determinati metadata della history.

### Baseline

Definisce il punto di partenza per un database preesistente.

### Drift

Differenza tra lo schema previsto e quello realmente presente.

### DDL

Comandi che modificano la struttura del database.

### DML

Comandi che modificano i dati.

---

# 55. Checklist: possiamo dire di conoscere Flyway?

Dovremmo riuscire a rispondere senza guardare gli appunti:

- [ ] Cos'è una database migration?
- [ ] Perché Flyway ci serve?
- [ ] Come vengono nominate le migration?
- [ ] Dove stanno normalmente in Spring Boot?
- [ ] Cos'è `flyway_schema_history`?
- [ ] Come fa Flyway a capire quali migration eseguire?
- [ ] Cos'è un checksum?
- [ ] Perché non modificare una migration già applicata?
- [ ] Cosa fa `migrate`?
- [ ] Cosa fa `validate`?
- [ ] Quando avrebbe senso `repair`?
- [ ] Cos'è una repeatable migration?
- [ ] Cos'è una baseline?
- [ ] Flyway e Hibernate che responsabilità diverse hanno?
- [ ] Flyway e PostgreSQL che responsabilità diverse hanno?
- [ ] Cosa succede se Dom e Rita creano la stessa versione su due branch?
- [ ] Perché le migration vanno in Git?
- [ ] Cos'è lo schema drift?
- [ ] Flyway è un design pattern? Perché no?

Se queste risposte sono chiare, per il livello richiesto dal nostro progetto siamo messi bene.

---

# 56. Cose che NON serve studiare adesso

Per Mivy non investirei tempo, salvo necessità concreta, su:

- Flyway Desktop avanzato;
- edition Teams/Enterprise;
- schema model enterprise;
- snapshot avanzati;
- undo migration avanzate;
- callbacks complessi;
- JavaMigration personalizzate;
- multi-schema sofisticato;
- configurazioni enterprise;
- generazione automatica delle migration;
- pipeline Flyway complesse.

Il professore ha ragione sul principio:

> se usiamo una tecnologia, dobbiamo sapere cos'è e perché la stiamo usando.

Ma **conoscere una tecnologia non significa utilizzare ogni feature che offre**.

Per il nostro progetto è più importante saper spiegare molto bene il workflow base e applicarlo correttamente.

---

# 57. Riassunto finale in 10 righe

1. Flyway gestisce le **database migration**.
2. Le migration descrivono cambiamenti incrementali del database.
3. Le migration versionate hanno nomi come `V1__initial_schema.sql`.
4. Flyway registra quelle applicate in `flyway_schema_history`.
5. Usa anche checksum per rilevare modifiche a migration già eseguite.
6. `migrate` applica le migration mancanti.
7. `validate` controlla che storia e file siano coerenti.
8. Una migration già condivisa non si modifica: si crea normalmente una migration successiva.
9. Hibernate/JPA gestisce il mapping OOP-relazionale; Flyway gestisce l'evoluzione dello schema.
10. Nel nostro progetto Flyway serve a rendere PostgreSQL **versionato, riproducibile e coerente tra ambienti**.

---

# 58. Fonti ufficiali

Documentazione consultata il **15 ottobre 2026**:

- Redgate Flyway — Getting Started  
  https://documentation.red-gate.com/fd/getting-started-with-flyway-184127223.html

- Redgate Flyway — Migrations  
  https://documentation.red-gate.com/fd/migrations-271585107.html

- Redgate Flyway — Migrate  
  https://documentation.red-gate.com/flyway/reference/commands/migrate

- Redgate Flyway — Validate  
  https://documentation.red-gate.com/flyway/reference/commands/validate

- Redgate Flyway — Repair  
  https://documentation.red-gate.com/flyway/reference/commands/repair

- Redgate Flyway — Baseline  
  https://documentation.red-gate.com/flyway/reference/commands/baseline

- Redgate Flyway — Baseline Migrations  
  https://documentation.red-gate.com/flyway/flyway-concepts/migrations/baseline-migrations

- Spring Boot — Database Initialization / Flyway integration  
  https://docs.spring.io/spring-boot/4.2/how-to/data-initialization.html

---

## Nota per Mivy

Questa guida spiega Flyway come tecnologia. Prima di fissare definitivamente nel progetto:

- naming delle migration;
- strategia Hibernate `ddl-auto`;
- configurazione di test;
- eventuale uso di Testcontainers;

conviene verificarli insieme alla configurazione reale del backend, così la documentazione finale descriverà **quello che abbiamo veramente implementato**, non una configurazione teorica.
