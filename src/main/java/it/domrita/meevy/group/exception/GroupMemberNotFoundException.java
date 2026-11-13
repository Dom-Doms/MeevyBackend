
package it.domrita.meevy.group.exception;

public class GroupMemberNotFoundException extends RuntimeException {

    public GroupMemberNotFoundException(Long userId) {
        super("Membro del gruppo non trovato: " + userId);
    }
}
