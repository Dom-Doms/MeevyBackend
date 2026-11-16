package it.domrita.meevy.checklist.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ChecklistRequest(

        @NotNull
        @Positive
        Long subjectId

) {}