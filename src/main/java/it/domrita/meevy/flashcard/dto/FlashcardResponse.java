package it.domrita.meevy.flashcard.dto;

import java.time.Instant;

public record FlashcardResponse(
        Long id,
        Long subjectId,
        String question,
        String answer,
        Instant createdAt,
        Instant updatedAt) {
}
