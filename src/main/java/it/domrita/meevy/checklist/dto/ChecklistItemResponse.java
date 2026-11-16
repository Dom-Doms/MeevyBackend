package it.domrita.meevy.checklist.dto;

public record ChecklistItemResponse(

        Long id,
        Long checklistId,
        String text,
        boolean completed

) {}