package it.domrita.meevy.checklist.controller;

import it.domrita.meevy.checklist.dto.ChecklistRequest;
import it.domrita.meevy.checklist.dto.ChecklistResponse;
import it.domrita.meevy.checklist.service.ChecklistService;
import it.domrita.meevy.checklist.dto.ChecklistItemRequest;
import it.domrita.meevy.checklist.dto.ChecklistItemResponse;
import it.domrita.meevy.checklist.dto.ChecklistItemCompletedRequest;

import java.util.List;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/checklists")
@RequiredArgsConstructor
public class ChecklistController {

    private final ChecklistService checklistService;

    @PostMapping
    public ResponseEntity<ChecklistResponse> createChecklist(
            @Valid @RequestBody ChecklistRequest request
    ) {

        ChecklistResponse response =
                checklistService.createChecklist(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/subject/{subjectId}")
    public ResponseEntity<ChecklistResponse> getChecklistBySubjectId(
            @PathVariable Long subjectId
    ) {

        ChecklistResponse response =
                checklistService.getChecklistBySubjectId(subjectId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PostMapping("/{checklistId}/items")
    public ResponseEntity<ChecklistItemResponse> createChecklistItem(
            @PathVariable Long checklistId,
            @Valid @RequestBody ChecklistItemRequest request
    ) {

        ChecklistItemResponse response =
                checklistService.createChecklistItem(checklistId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Recupera tutte le attività della checklist.
    @GetMapping("/{checklistId}/items")
    public ResponseEntity<List<ChecklistItemResponse>> getChecklistItems(
            @PathVariable Long checklistId
    ) {

        List<ChecklistItemResponse> response =
                checklistService.getChecklistItems(checklistId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }


    // Modifica il testo di una singola attività.
    @PutMapping("/items/{itemId}")
    public ResponseEntity<ChecklistItemResponse> updateChecklistItem(
            @PathVariable Long itemId,
            @Valid @RequestBody ChecklistItemRequest request
    ) {

        ChecklistItemResponse response =
                checklistService.updateChecklistItem(itemId, request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }


    // Spunta o deseleziona un'attività.
    @PatchMapping("/items/{itemId}/completed")
    public ResponseEntity<ChecklistItemResponse> updateChecklistItemCompleted(
            @PathVariable Long itemId,
            @Valid @RequestBody ChecklistItemCompletedRequest request
    ) {

        ChecklistItemResponse response =
                checklistService.updateChecklistItemCompleted(
                        itemId, request
                );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }


    // Elimina una singola attività.
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> deleteChecklistItem(
            @PathVariable Long itemId
    ) {

        checklistService.deleteChecklistItem(itemId);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}