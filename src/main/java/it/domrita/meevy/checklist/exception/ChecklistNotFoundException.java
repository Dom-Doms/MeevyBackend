package it.domrita.meevy.checklist.exception;

public class ChecklistNotFoundException extends RuntimeException {

    public ChecklistNotFoundException(Long subjectId) {
        super("Checklist non trovata per la materia: " + subjectId);
    }

    public ChecklistNotFoundException(String message) {
        super(message);
    }
}