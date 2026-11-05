package it.domrita.meevy.flashcard.controller;


import it.domrita.meevy.flashcard.dto.FlashcardRequest;
import it.domrita.meevy.flashcard.dto.FlashcardResponse;
import it.domrita.meevy.flashcard.model.Flashcard;
import it.domrita.meevy.flashcard.repository.FlashcardRepository;
import it.domrita.meevy.flashcard.service.FlashcardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
