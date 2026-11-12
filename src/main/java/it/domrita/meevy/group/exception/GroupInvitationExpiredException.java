
package it.domrita.meevy.group.exception;

public class GroupInvitationExpiredException extends RuntimeException {

    public GroupInvitationExpiredException() {
        super("L'invito è scaduto.");
    }
}
