-- MeevyBackend - Flyway migration V2
-- La V1 ha creato la tabella app_user.
-- Questa migrazione la rinomina in "user" senza perdere dati o vincoli.
-- USER e' una parola chiave SQL: usare sempre le virgolette doppie
-- per riferirsi alla tabella "user" nelle query PostgreSQL.

ALTER TABLE app_user RENAME TO "user";

-- La chiave primaria, l'IDENTITY e il vincolo uk_user_email
-- vengono conservati automaticamente dal comando RENAME.
