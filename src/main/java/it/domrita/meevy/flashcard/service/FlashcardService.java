package it.domrita.meevy.flashcard.service;

import it.domrita.meevy.auth.repository.CurrentUserIdProvider;
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

        //salva la flashcard nel database
        Flashcard savedFlashcard = new Flashcard(user, subject, request.question().trim(), request.answer().trim());

        //restituisce i dati della flashcard creata
        return new FlashcardResponse(
                savedFlashcard.getId(),
                subject.getId(),
                savedFlashcard.getQuestion(),
                savedFlashcard.getAnswer(),
                savedFlashcard.getCreatedAt(),
                savedFlashcard.getUpdatedAt()

        );


    }

    @Transactional(readOnly = true)
    public List<FlashcardResponse> getAllFlashcards() {

        Long userId = currentUserIdProvider.getCurrentUserId();

        return flashcardRepository.findAllByUser_IdOrderByIdAsc(userId)
                .stream().map(flashcard -> new FlashcardResponse(
                        flashcard.getId(),
                        flashcard.getSubject() != null ? //se la flashcard ha la materia associata, restituisci l'id della materia, altrimenti null
                                flashcard.getSubject().getId() : null,
                        flashcard.getQuestion(),
                        flashcard.getAnswer(),
                        flashcard.getCreatedAt(),
                        flashcard.getUpdatedAt()
                ))
                .toList();
    }
}
