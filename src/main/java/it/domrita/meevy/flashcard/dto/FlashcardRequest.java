package it.domrita.meevy.flashcard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FlashcardRequest (

    @NotNull
    @Positive
    Long subjectId,

    @NotBlank
    String question,

    @NotBlank
    String answer){ }
