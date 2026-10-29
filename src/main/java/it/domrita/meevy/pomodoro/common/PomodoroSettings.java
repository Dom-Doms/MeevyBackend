
package it.domrita.meevy.pomodoro.common;

/*
 * Configurazione di una sessione Pomodoro.
 *
 * studyDurationSeconds: durata della fase di studio.
 * breakDurationSeconds: durata della pausa programmata.
 * targetDurationSeconds: durata totale della sessione.
 *
 * Tutte le durate sono espresse in secondi.
 * La durata totale è sempre obbligatoria.
 */
public record PomodoroSettings(
        int studyDurationSeconds,
        int breakDurationSeconds,
        long targetDurationSeconds
) {

    /*
     * Verifica la validità della configurazione
     * al momento della creazione.
     */
    public PomodoroSettings {

        if (studyDurationSeconds <= 0) {
            throw new IllegalArgumentException(
                    "La durata dello studio deve essere positiva."
            );
        }

        if (breakDurationSeconds <= 0) {
            throw new IllegalArgumentException(
                    "La durata della pausa deve essere positiva."
            );
        }

        if (targetDurationSeconds <= 0) {
            throw new IllegalArgumentException(
                    "La durata totale deve essere positiva."
            );
        }
    }
}
