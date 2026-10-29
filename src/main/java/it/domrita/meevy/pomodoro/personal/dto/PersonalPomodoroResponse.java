
package it.domrita.meevy.pomodoro.personal.dto;

import it.domrita.meevy.pomodoro.common.*;
import it.domrita.meevy.pomodoro.personal.model.PersonalPomodoroSession;
import it.domrita.meevy.pomodoro.personal.role.PersonalPomodoroStatus;

import java.time.Instant;

/*
 * Rappresenta una sessione Pomodoro restituita
 * al client, senza esporre direttamente la Entity JPA.
 */
public record PersonalPomodoroResponse(

        Long id,
        Long subjectId,
        PersonalPomodoroStatus status,

        int studyDurationSeconds,
        int breakDurationSeconds,
        long targetDurationSeconds,

        Instant createdAt,
        Instant startedAt,
        Instant pausedAt,
        Instant endedAt,

        long elapsedSeconds,
        PomodoroPhase phase,
        long cycleNumber,
        long remainingPhaseSeconds,
        long remainingSessionSeconds,

        long plannedStudySeconds,
        long actualStudySeconds

) {

    /*
     * Converte una Entity nella risposta REST.
     *
     * Il progresso viene ricostruito utilizzando
     * il tempo trascorso, senza un timer lato server.
     */
    public static PersonalPomodoroResponse from(
            PersonalPomodoroSession session,
            Instant now
    ) {
        PersonalPomodoroStatus status = session.getStatus();

        boolean terminated =
                status == PersonalPomodoroStatus.COMPLETED
                        || status == PersonalPomodoroStatus.CANCELLED;
        boolean ready = status == PersonalPomodoroStatus.READY;

        long elapsed = session.elapsedSeconds(now);

        PomodoroSettings settings = new PomodoroSettings(
                session.getStudyDurationSeconds(),
                session.getBreakDurationSeconds(),
                session.getTargetDurationSeconds()
        );

        PomodoroCycleProgress progress =
                PomodoroCycleCalculator.calculate(
                        settings,
                        elapsed
                );


        /*
         * Quando la sessione è READY, il timer non è ancora
         * partito infatti non  esiste una fase attiva.
         *
         * Quando è terminata, non ci sono tempi residui.
         */
        PomodoroPhase phase =
                (ready || terminated) ? null : progress.phase();

        long remainingPhase =
                (ready || terminated)
                        ? 0
                        : progress.remainingPhaseSeconds();

        long remainingSession =
                terminated ? 0 : progress.remainingSessionSeconds();


        /*
         * Le sessioni terminate con cancel non verranno contate ai fini delle statistiche
         * CANCELLED mantiene il valore zero.
         */
        long plannedStudy =
                terminated
                        ? session.getActualStudySeconds()
                        : progress.plannedStudySeconds();

        Long subjectId = session.getSubject() == null
                ? null
                : session.getSubject().getId();

        return new PersonalPomodoroResponse(
                session.getId(),
                subjectId,
                status,

                session.getStudyDurationSeconds(),
                session.getBreakDurationSeconds(),
                session.getTargetDurationSeconds(),

                session.getCreatedAt(),
                session.getStartedAt(),
                session.getPausedAt(),
                session.getEndedAt(),

                elapsed,
                phase,
                progress.cycleNumber(),
                remainingPhase,
                remainingSession,

                plannedStudy,
                session.getActualStudySeconds()
        );
    }
}
