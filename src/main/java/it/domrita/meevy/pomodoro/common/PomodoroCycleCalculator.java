
package it.domrita.meevy.pomodoro.common;

import java.util.Objects;

/*
 * Calcola l'avanzamento dei cicli Pomodoro.
 *
 * Utilizza la configurazione e il tempo trascorso
 * per determinare la fase corrente, il ciclo
 * e il tempo residuo.
 *
 * Non gestisce database, utenti o thread.
 * Non mantiene uno stato interno.
 */
public final class PomodoroCycleCalculator {

    private PomodoroCycleCalculator() {
        // Impedisce l'istanziazione della classe.
    }

    public static PomodoroCycleProgress calculate(
            PomodoroSettings settings,
            long elapsedSeconds
    ) {
        Objects.requireNonNull(settings);

        if (elapsedSeconds < 0) {
            throw new IllegalArgumentException(
                    "Il tempo trascorso non può essere negativo."
            );
        }

        /*
         * Impedisce di superare la durata totale.
         */
        long elapsed = Math.min(
                elapsedSeconds,
                settings.targetDurationSeconds()
        );

        /*
         * Durata di un ciclo completo.
         */
        long cycleDuration =
                (long) settings.studyDurationSeconds()
                        + settings.breakDurationSeconds();

        /*
         * Cicli completati e posizione
         * all'interno del ciclo.
         */
        long completedCycles = elapsed / cycleDuration;
        long position = elapsed % cycleDuration;

        /*
         * Calcola il tempo appartenente
         * alle fasi di studio.
         */
        long plannedStudySeconds =
                completedCycles * settings.studyDurationSeconds()
                        + Math.min(
                        position,
                        settings.studyDurationSeconds()
                );

        long remainingSessionSeconds =
                settings.targetDurationSeconds() - elapsed;

        /*
         * La durata totale è stata raggiunta.
         */
        if (remainingSessionSeconds == 0) {

            long lastCycle =
                    completedCycles + (position > 0 ? 1 : 0);

            return new PomodoroCycleProgress(
                    null,
                    lastCycle,
                    0,
                    0,
                    plannedStudySeconds,
                    true
            );
        }

        PomodoroPhase phase;
        long remainingPhaseSeconds;

        /*
         * Determina se ci troviamo
         * nella fase STUDY oppure BREAK.
         */
        if (position < settings.studyDurationSeconds()) {

            phase = PomodoroPhase.STUDY;

            remainingPhaseSeconds =
                    settings.studyDurationSeconds() - position;

        } else {

            phase = PomodoroPhase.BREAK;

            remainingPhaseSeconds =
                    cycleDuration - position;
        }

        /*
         * La fase non può durare oltre
         * il termine previsto della sessione.
         */
        remainingPhaseSeconds = Math.min(
                remainingPhaseSeconds,
                remainingSessionSeconds
        );

        return new PomodoroCycleProgress(
                phase,
                completedCycles + 1,
                remainingPhaseSeconds,
                remainingSessionSeconds,
                plannedStudySeconds,
                false
        );
    }
}
