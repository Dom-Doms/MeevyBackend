
package it.domrita.meevy.pomodoro.personal.model;

import it.domrita.meevy.pomodoro.common.PomodoroSettings;
import it.domrita.meevy.pomodoro.personal.role.PersonalPomodoroStatus;
import it.domrita.meevy.subject.model.Subject;
import it.domrita.meevy.user.model.User;

import it.domrita.meevy.pomodoro.common.PomodoroCycleCalculator;
import it.domrita.meevy.pomodoro.common.PomodoroCycleProgress;

import java.time.Duration;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

/*
 * Rappresenta una sessione Pomodoro personale.
 *
 * Ogni sessione appartiene a un utente e viene
 * associata a una materia durante la creazione.
 *
 * Memorizza la configurazione, lo stato e i dati
 * temporali necessari per recuperare la sessione
 * e calcolare le statistiche di studio.
 */
@Entity
@Table(name = "personal_pomodoro_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersonalPomodoroSession {

    /*
     * Identificatore univoco della sessione.
     * Generato automaticamente dal database.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Proprietario della sessione.
     *
     * Il riferimento è obbligatorio e permette
     * di mantenere lo storico dell'utente.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /*
     * Materia associata alla sessione.
     *
     * Obbligatoria alla creazione, ma il riferimento
     * può diventare null se la materia viene eliminata.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    /*
     * Configurazione del Pomodoro.
     *
     * Le durate sono espresse in secondi.
     */
    @Column(name = "study_duration_seconds", nullable = false)
    private int studyDurationSeconds;

    @Column(name = "break_duration_seconds", nullable = false)
    private int breakDurationSeconds;

    @Column(name = "target_duration_seconds", nullable = false)
    private long targetDurationSeconds;

    /*
     * Stato complessivo della sessione.
     * Una nuova sessione parte sempre da READY.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PersonalPomodoroStatus status =
            PersonalPomodoroStatus.READY;

    /*
     * Data di creazione del record.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /*
     * Data di avvio.
     * Rimane null finché il Pomodoro non viene avviato.
     */
    @Column(name = "started_at")
    private Instant startedAt;

    /*
     * Momento in cui è iniziata una sospensione manuale.
     * Null quando la sessione non è sospesa.
     */
    @Column(name = "paused_at")
    private Instant pausedAt;

    /*
     * Somma delle sospensioni manuali già concluse.
     * Non comprende le pause programmate del ciclo.
     */
    @Column(name = "accumulated_pause_seconds", nullable = false)
    private long accumulatedPauseSeconds = 0;

    /*
     * Data di conclusione della sessione.
     */
    @Column(name = "ended_at")
    private Instant endedAt;

    /*
     * Secondi di studio contabilizzati
     * alla conclusione della sessione.
     *
     * Non rappresentano una verifica dell'effettivo
     * studio svolto dall'utente.
     */
    @Column(name = "actual_study_seconds", nullable = false)
    private long actualStudySeconds = 0;

    /*
     * Costruttore utilizzato per creare una sessione.
     *
     * Riceve il proprietario, la materia e la
     * configurazione già validata.
     *
     * Il Pomodoro viene creato in READY:
     * non viene ancora avviato.
     */
    public PersonalPomodoroSession(
            User user,
            Subject subject,
            PomodoroSettings settings
    ) {
        this.user = Objects.requireNonNull(user);
        this.subject = Objects.requireNonNull(subject);
        Objects.requireNonNull(settings);

        this.studyDurationSeconds =
                settings.studyDurationSeconds();

        this.breakDurationSeconds =
                settings.breakDurationSeconds();

        this.targetDurationSeconds =
                settings.targetDurationSeconds();

        this.status = PersonalPomodoroStatus.READY;
    }


    /*
     * Avvia una sessione configurata ma non ancora iniziata.
     */
    public void start(Instant now) {
        requireStatus(PersonalPomodoroStatus.READY);

        this.startedAt = Objects.requireNonNull(now);
        this.status = PersonalPomodoroStatus.RUNNING;
    }

    /*
     * Sospende manualmente una sessione in esecuzione.
     *
     * La sospensione manuale è distinta dalla fase BREAK.
     */
    public void pause(Instant now) {
        requireStatus(PersonalPomodoroStatus.RUNNING);

        if (elapsedSeconds(now) >= targetDurationSeconds) {
            throw new IllegalStateException(
                    "La durata della sessione è già terminata."
            );
        }

        this.pausedAt = now;
        this.status = PersonalPomodoroStatus.PAUSED;
    }

    /*
     * Riprende una sessione sospesa.
     *
     * Accumula il tempo della sospensione appena conclusa,
     * che non dovrà essere conteggiato nei cicli Pomodoro.
     */
    public void resume(Instant now) {
        requireStatus(PersonalPomodoroStatus.PAUSED);

        closeManualPause(now);
        this.status = PersonalPomodoroStatus.RUNNING;
    }

    /*
     * Verifica se una sessione in esecuzione
     * ha raggiunto la durata totale prevista.
     *
     * Restituisce true se la sessione è stata completata.
     */
    public boolean completeIfExpired(Instant now) {

        if (status != PersonalPomodoroStatus.RUNNING) {
            return false;
        }

        if (elapsedSeconds(now) < targetDurationSeconds) {
            return false;
        }

        Instant scheduledEnd = startedAt.plusSeconds(
                targetDurationSeconds + accumulatedPauseSeconds
        );

        complete(scheduledEnd);

        return true;
    }


    /*
     * Verifica che la sessione si trovi nello stato richiesto.
     */
    private void requireStatus(PersonalPomodoroStatus expected) {
        if (status != expected) {
            throw new IllegalStateException(
                    "Operazione non consentita nello stato " + status
            );
        }
    }

    /*
     * Restituisce la configurazione della sessione.
     */
    private PomodoroSettings settings() {
        return new PomodoroSettings(
                studyDurationSeconds,
                breakDurationSeconds,
                targetDurationSeconds
        );
    }

    /*
     * Calcola il tempo trascorso escludendo
     * le sospensioni manuali.
     */
    public long elapsedSeconds(Instant now) {
        Objects.requireNonNull(now);

        if (startedAt == null) {
            return 0;
        }

        Instant reference;

        if (status == PersonalPomodoroStatus.PAUSED) {
            reference = pausedAt;
        } else if (endedAt != null) {
            reference = endedAt;
        } else {
            reference = now;
        }

        if (reference.isBefore(startedAt)) {
            throw new IllegalArgumentException(
                    "L'orario non può precedere l'avvio."
            );
        }

        long elapsed =
                Duration.between(startedAt, reference).getSeconds()
                        - accumulatedPauseSeconds;

        return Math.max(0, elapsed);
    }

    /*
     * Termina una sospensione manuale
     * e aggiorna il tempo totale delle pause.
     */
    private void closeManualPause(Instant now) {
        Objects.requireNonNull(now);

        if (now.isBefore(pausedAt)) {
            throw new IllegalArgumentException(
                    "L'orario precede l'inizio della pausa."
            );
        }

        accumulatedPauseSeconds +=
                Duration.between(pausedAt, now).getSeconds();

        pausedAt = null;
    }


    /*
     * Completa una sessione Pomodoro.
     *
     * Può essere chiamato soltanto quando la sessione
     * è RUNNING oppure PAUSED.
     *
     * Calcola il tempo di studio programmato accumulato,
     * registra la data di fine e aggiorna lo stato.
     */
    public void complete(Instant now) {

        if (status != PersonalPomodoroStatus.RUNNING
                && status != PersonalPomodoroStatus.PAUSED) {
            throw new IllegalStateException(
                    "La sessione non può essere completata."
            );
        }

        Objects.requireNonNull(now);

        // Verifica che la data non preceda l'eventuale pausa.
        if (status == PersonalPomodoroStatus.PAUSED
                && now.isBefore(pausedAt)) {
            throw new IllegalArgumentException(
                    "L'orario precede l'inizio della pausa."
            );
        }

        long elapsed = elapsedSeconds(now);

        PomodoroCycleProgress progress =
                PomodoroCycleCalculator.calculate(
                        settings(),
                        elapsed
                );

        // Conclude un'eventuale sospensione manuale.
        if (status == PersonalPomodoroStatus.PAUSED) {
            closeManualPause(now);
        }

        this.actualStudySeconds = progress.plannedStudySeconds();
        this.endedAt = now;
        this.status = PersonalPomodoroStatus.COMPLETED;
    }

    /*
     * Annulla una sessione Pomodoro.
     *
     * Può essere chiamato da READY, RUNNING o PAUSED.
     * Una sessione già terminata non può essere annullata.
     *
     * Le sessioni annullate non contribuiscono
     * alle statistiche di studio.
     */
    public void cancel(Instant now) {

        if (status == PersonalPomodoroStatus.COMPLETED
                || status == PersonalPomodoroStatus.CANCELLED) {
            throw new IllegalStateException(
                    "La sessione è già terminata."
            );
        }

        Objects.requireNonNull(now);

        if (status == PersonalPomodoroStatus.PAUSED) {
            closeManualPause(now);
        } else if (status == PersonalPomodoroStatus.RUNNING) {
            elapsedSeconds(now);
        }

        this.actualStudySeconds = 0;
        this.endedAt = now;
        this.status = PersonalPomodoroStatus.CANCELLED;
    }

    /*
     * Imposta automaticamente la data di creazione
     * prima del primo inserimento nel database.
     */
    @PrePersist
    private void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
