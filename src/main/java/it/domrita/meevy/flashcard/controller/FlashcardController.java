package it.domrita.meevy.flashcard.controller;


import it.domrita.meevy.flashcard.dto.FlashcardRequest;
import it.domrita.meevy.flashcard.dto.FlashcardResponse;
import it.domrita.meevy.flashcard.exception.FlashcardNotFoundException;
import it.domrita.meevy.flashcard.model.Flashcard;
import it.domrita.meevy.flashcard.repository.FlashcardRepository;
import it.domrita.meevy.flashcard.service.FlashcardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/flashcards")
@RequiredArgsConstructor
public class FlashcardController {

    private final FlashcardService flashcardService;

    @PostMapping
    public ResponseEntity<FlashcardResponse> createFlashcard(@Valid @RequestBody FlashcardRequest request) {

        FlashcardResponse response = flashcardService.createFlashcard(request);

        return ResponseEntity
                .status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<FlashcardResponse>> getAllFlashcards() {

        List<FlashcardResponse> flashcards = flashcardService.getAllFlashcards();

        return ResponseEntity.status(HttpStatus.OK).body(flashcards);
    }

    @GetMapping("/subject/{subjectId}")
    public ResponseEntity<List<FlashcardResponse>> getAllFlashcardsBySubject(@PathVariable Long subjectId) {

        List<FlashcardResponse> flashcard = flashcardService.getFlashcardsBySubjectId(subjectId);

        return ResponseEntity.status(HttpStatus.OK).body(flashcard);
    }

    //
    @GetMapping("/{id}")
    public ResponseEntity<FlashcardResponse> getFlashcardById(@PathVariable Long id) {

        FlashcardResponse flashcard = flashcardService.getFlashcardById(id);

        return ResponseEntity.status(HttpStatus.OK).body(flashcard);
    }


    @ExceptionHandler(FlashcardNotFoundException.class)
    public ProblemDetail handleFlashcardNotFound(FlashcardNotFoundException ex) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());

        problem.setProperty("code", "FLASHCARD_NOT_FOUND");

        return problem;

    }

    @PutMapping("/{id}")
    public ResponseEntity<FlashcardResponse> updateFlashcard(
            @PathVariable Long id, @Valid @RequestBody FlashcardRequest request) {

        FlashcardResponse response = flashcardService.updateFlashcard(id, request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<FlashcardResponse> deleteFlashcard(@PathVariable Long id) {

        flashcardService.deleteFlashcard(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<FlashcardResponse> duplicateFlashcard(@PathVariable Long id) {

        FlashcardResponse response = flashcardService.duplicateFlashcard(id);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


}
