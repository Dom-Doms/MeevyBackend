
package it.domrita.meevy.pomodoro.personal.exception;

/*
 * Indica che l'utente possiede già una sessione
 * in stato READY, RUNNING oppure PAUSED.
 */
public class PersonalPomodoroAlreadyActiveException
        extends RuntimeException {

    public PersonalPomodoroAlreadyActiveException() {
        super("Esiste già una sessione Pomodoro non terminata.");
    }
}
