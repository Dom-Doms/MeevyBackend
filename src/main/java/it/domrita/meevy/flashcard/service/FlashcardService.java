package it.domrita.meevy.flashcard.service;

import it.domrita.meevy.flashcard.dto.FlashcardUpdateRequest;
import it.domrita.meevy.auth.repository.CurrentUserIdProvider;
import it.domrita.meevy.flashcard.controller.FlashcardController;
import it.domrita.meevy.flashcard.dto.FlashcardRequest;
import it.domrita.meevy.flashcard.dto.FlashcardResponse;
import it.domrita.meevy.flashcard.model.Flashcard;
import it.domrita.meevy.flashcard.repository.FlashcardRepository;
import it.domrita.meevy.subject.exception.SubjectNotFoundException;
import it.domrita.meevy.subject.model.Subject;
import it.domrita.meevy.subject.repository.SubjectRepository;
import it.domrita.meevy.user.model.User;
import it.domrita.meevy.user.repository.UserRepository;
import it.domrita.meevy.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import it.domrita.meevy.flashcard.exception.FlashcardNotFoundException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FlashcardService {

    private final FlashcardRepository flashcardRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final CurrentUserIdProvider currentUserIdProvider;


    public FlashcardResponse createFlashcard(FlashcardRequest request) {

        //recupera l'id dell'utente
        Long userId = currentUserIdProvider.getCurrentUserId();

        User user =userRepository.findById(userId).
                orElseThrow(() -> new IllegalStateException("Utente non trovato"));

        Subject subject = subjectRepository
                .findByIdAndUser_Id(request.subjectId(), userId)
                .orElseThrow(() -> new SubjectNotFoundException(request.subjectId()));

        //crea la flashcard
        Flashcard flashcard = new Flashcard(user, subject, request.question().trim(), request.answer().trim());

        //salva la flashcard nel database
        Flashcard savedFlashcard = flashcardRepository.save(flashcard);

        //restituisce i dati della flashcard creata
        return toResponse(savedFlashcard);


    }

    @Transactional(readOnly = true)
    public List<FlashcardResponse> getAllFlashcards() {

        Long userId = currentUserIdProvider.getCurrentUserId();

        return flashcardRepository.findAllByUser_IdOrderByIdAsc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FlashcardResponse> getFlashcardsBySubjectId(Long subjectId) {

        Long userId = currentUserIdProvider.getCurrentUserId();

        subjectRepository.findByIdAndUser_Id(subjectId, userId)
                .orElseThrow(() -> new SubjectNotFoundException(subjectId));

        return flashcardRepository
                .findAllByUser_IdAndSubject_IdOrderByIdAsc(userId, subjectId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FlashcardResponse getFlashcardById(Long id) {

        Long userId = currentUserIdProvider.getCurrentUserId();

        Flashcard flashcard = flashcardRepository.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new FlashcardNotFoundException(id));

        return toResponse(flashcard);

    }

    public FlashcardResponse updateFlashcard(Long id, FlashcardUpdateRequest request) {

        Long userId = currentUserIdProvider.getCurrentUserId();

        Flashcard flashcard = flashcardRepository
                .findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new FlashcardNotFoundException(id));

        flashcard.updateContent(
                request.question().trim(),
                request.answer().trim()
        );

        Flashcard updatedFlashcard = flashcardRepository.saveAndFlush(flashcard);

        return toResponse(updatedFlashcard);
    }

    public void deleteFlashcard(Long id) {

        Long userId = currentUserIdProvider.getCurrentUserId();

        Flashcard flashcard = flashcardRepository
                .findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new FlashcardNotFoundException(id));

        flashcardRepository.delete(flashcard);
    }

    public FlashcardResponse duplicateFlashcard(Long id) {

        Long userId = currentUserIdProvider.getCurrentUserId();

        Flashcard original = flashcardRepository
                .findByIdAndUser_Id(id, userId)
                .orElseThrow(()-> new FlashcardNotFoundException(id));

        Flashcard duplicate = new Flashcard(
                original.getUser(),
                original.getSubject(),
                original.getQuestion(),
                original.getAnswer()
        );

        Flashcard savedDuplicate = flashcardRepository.save(duplicate);

        return toResponse(savedDuplicate);
    }

    private FlashcardResponse toResponse(Flashcard flashcard) {

        return new FlashcardResponse(
                flashcard.getId(),
                flashcard.getSubject() != null
                        ? flashcard.getSubject().getId() : null,
                flashcard.getQuestion(),
                flashcard.getAnswer(),
                flashcard.getCreatedAt(),
                flashcard.getUpdatedAt()
        );
    }
}
