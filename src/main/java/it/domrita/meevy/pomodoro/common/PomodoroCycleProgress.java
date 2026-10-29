
package it.domrita.meevy.pomodoro.common;

/*
 * Contiene le informazioni temporali
 * calcolate per una sessione Pomodoro.
 *
 * phase: fase corrente, null se terminata.
 * cycleNumber: numero del ciclo.
 * remainingPhaseSeconds: secondi fino al prossimo
 *                       cambio di fase o alla fine.
 * remainingSessionSeconds: secondi prima della fine.
 * plannedStudySeconds: tempo di studio programmato
 *                      accumulato.
 * completed: indica se la durata totale è terminata.
 */
public record PomodoroCycleProgress(
        PomodoroPhase phase,
        long cycleNumber,
        long remainingPhaseSeconds,
        long remainingSessionSeconds,
        long plannedStudySeconds,
        boolean completed
) {}
