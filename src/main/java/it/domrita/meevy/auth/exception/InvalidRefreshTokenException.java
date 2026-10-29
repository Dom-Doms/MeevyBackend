
package it.domrita.meevy.auth.exception;

/*
 * Segnala che un refresh token non può essere
 * utilizzato perché inesistente, scaduto,
 * revocato o associato a un account non attivo.
 */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Refresh token non valido o scaduto.");
    }
}
