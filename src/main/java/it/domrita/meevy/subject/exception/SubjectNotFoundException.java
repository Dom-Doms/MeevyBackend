package it.domrita.meevy.subject.exception;

public class SubjectNotFoundException extends RuntimeException {

    public SubjectNotFoundException(Long subjectId) {
        super("Materia non trovata: " + subjectId );
    }
}
