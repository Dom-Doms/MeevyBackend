package it.domrita.meevy.flashcard.exception;

public class FlashcardNotFoundException extends RuntimeException {
    public FlashcardNotFoundException(Long id) {

        super("Flashcard non trovata: "+id);
    }
}
