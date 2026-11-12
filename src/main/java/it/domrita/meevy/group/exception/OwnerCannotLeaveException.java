
package it.domrita.meevy.group.exception;

public class OwnerCannotLeaveException extends RuntimeException {

    public OwnerCannotLeaveException() {
        super("Il proprietario deve trasferire la proprietà prima di lasciare il gruppo.");
    }
}
