
package it.domrita.meevy.pomodoro.personal.exception;

/*
 * Indica che la sessione Pomodoro richiesta
 * non esiste o non appartiene all'utente autenticato.
 */
public class PersonalPomodoroNotFoundException
        extends RuntimeException {

    public PersonalPomodoroNotFoundException(Long id) {
        super("Sessione Pomodoro non trovata: " + id);
    }
}
