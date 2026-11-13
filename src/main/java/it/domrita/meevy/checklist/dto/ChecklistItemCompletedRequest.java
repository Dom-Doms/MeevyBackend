package it.domrita.meevy.checklist.dto;

import jakarta.validation.constraints.NotNull;

public record ChecklistItemCompletedRequest(

        @NotNull
        Boolean completed

) {}