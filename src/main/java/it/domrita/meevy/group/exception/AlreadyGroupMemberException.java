
package it.domrita.meevy.group.exception;

public class AlreadyGroupMemberException extends RuntimeException {

    public AlreadyGroupMemberException() {
        super("Fai già parte di questo gruppo.");
    }
}
