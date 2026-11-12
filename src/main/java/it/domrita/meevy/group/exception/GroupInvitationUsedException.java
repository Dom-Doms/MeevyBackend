
package it.domrita.meevy.group.exception;

public class GroupInvitationUsedException extends RuntimeException {

    public GroupInvitationUsedException() {
        super("L'invito è già stato utilizzato.");
    }
}
