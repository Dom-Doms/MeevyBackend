
package it.domrita.meevy.pomodoro.personal.dto;

import it.domrita.meevy.pomodoro.common.PomodoroSettings;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/*
 * Rappresenta i dati inviati dal client
 * per configurare un Pomodoro personale.
 *
 * Le durate sono espresse in secondi.
 */
public record PersonalPomodoroRequest(

        @NotNull
        @Positive
        Long subjectId,

        @NotNull
        @Positive
        Integer studyDurationSeconds,

        @NotNull
        @Positive
        Integer breakDurationSeconds,

        @NotNull
        @Positive
        Long targetDurationSeconds

) {

    /*
     * Converte i dati ricevuti in modo da poterli utilizzare nel codice
     */
    public PomodoroSettings toSettings() {
        return new PomodoroSettings(
                studyDurationSeconds,
                breakDurationSeconds,
                targetDurationSeconds
        );
    }
}
