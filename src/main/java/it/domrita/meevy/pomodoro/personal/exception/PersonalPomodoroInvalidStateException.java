
package it.domrita.meevy.pomodoro.personal.exception;

/*
 * Indica che l'operazione richiesta non è consentita
 * nello stato attuale della sessione.
 */
public class PersonalPomodoroInvalidStateException
        extends RuntimeException {

    public PersonalPomodoroInvalidStateException(String message) {
        super(message);
    }
}
