
package it.domrita.meevy.group.exception;

public class GroupNotFoundException extends RuntimeException {

    public GroupNotFoundException(Long groupId) {
        super("Gruppo non trovato: " + groupId);
    }
}
