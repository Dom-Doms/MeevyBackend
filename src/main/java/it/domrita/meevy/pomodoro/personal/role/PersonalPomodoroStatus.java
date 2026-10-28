
package it.domrita.meevy.pomodoro.personal.role;

/*
 * Rappresenta lo stato complessivo
 * di una sessione Pomodoro personale.
 *
 * È distinto dalla fase STUDY/BREAK.
 */
public enum PersonalPomodoroStatus {

    READY,      // Sessione configurata, non avviata.
    RUNNING,    // Sessione in esecuzione.
    PAUSED,     // Sessione sospesa manualmente.
    COMPLETED,  // Durata totale raggiunta o fine anticipata.
    CANCELLED   // Sessione annullata.
}
