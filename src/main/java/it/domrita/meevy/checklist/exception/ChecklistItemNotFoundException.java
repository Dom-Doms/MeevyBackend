package it.domrita.meevy.checklist.exception;

public class ChecklistItemNotFoundException extends RuntimeException {

    public ChecklistItemNotFoundException(Long itemId) {
        super("Attività della checklist non trovata: " + itemId);
    }
}