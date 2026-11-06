package it.domrita.meevy.flashcard.dto;
import jakarta.validation.constraints.NotBlank;

public record FlashcardUpdateRequest(
        @NotBlank String question,
        @NotBlank String answer
) {}
