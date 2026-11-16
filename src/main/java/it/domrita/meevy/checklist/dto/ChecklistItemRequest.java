package it.domrita.meevy.checklist.dto;

import jakarta.validation.constraints.NotBlank;

public record ChecklistItemRequest(

        @NotBlank
        String text

) {}