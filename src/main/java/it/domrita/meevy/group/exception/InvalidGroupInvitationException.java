
package it.domrita.meevy.group.exception;

public class InvalidGroupInvitationException extends RuntimeException {

    public InvalidGroupInvitationException() {
        super("Invito non valido.");
    }
}
