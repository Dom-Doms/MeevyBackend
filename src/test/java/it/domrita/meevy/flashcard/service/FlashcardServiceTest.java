package it.domrita.meevy.flashcard.service;
import java.util.List;


import it.domrita.meevy.auth.repository.CurrentUserIdProvider;
import it.domrita.meevy.flashcard.dto.FlashcardRequest;
import it.domrita.meevy.flashcard.dto.FlashcardResponse;
import it.domrita.meevy.flashcard.dto.FlashcardUpdateRequest;
import it.domrita.meevy.flashcard.exception.FlashcardNotFoundException;
import it.domrita.meevy.flashcard.model.Flashcard;
import it.domrita.meevy.flashcard.repository.FlashcardRepository;
import it.domrita.meevy.subject.model.Subject;
import it.domrita.meevy.subject.repository.SubjectRepository;
import it.domrita.meevy.user.model.User;
import it.domrita.meevy.user.repository.UserRepository;
import it.domrita.meevy.subject.exception.SubjectNotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;


import static org.junit.jupiter.api.Assertions.*;



@ExtendWith(MockitoExtension.class)
public class FlashcardServiceTest {

    @Mock
    private FlashcardRepository flashcardRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserIdProvider currentUserIdProvider;

    @InjectMocks
    private FlashcardService flashcardService;

    @Test //creazione corretta della flashcard
    void createFlashcard_testCreationFlashcardSuccess() {


        //Prepariamo i dati
        Long userId = 1L;
        Long subjectId = 2L;

        User user = mock(User.class);
        Subject subject = mock(Subject.class);


        FlashcardRequest request = new FlashcardRequest(
                subjectId,
                "cos'è JWT?",
                "è uno standard aperto utilizzato per trasmettere in modo sicuro e compatto " +
                        "informazioni tra le due parti sotto forma di JSON"
        );

        when(currentUserIdProvider.getCurrentUserId()).thenReturn(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        when(subjectRepository.findByIdAndUser_Id(subjectId, userId))
                .thenReturn(Optional.of(subject));

        when(subject.getId()).thenReturn(subjectId);

        Instant now = Instant.now();

        //Simuliamo il salvataggio
        when(flashcardRepository.save(any(Flashcard.class)))
                .thenAnswer(invocation-> {

                    Flashcard flashcard = invocation.getArgument(0);

                    ReflectionTestUtils.setField(flashcard, "id", 10L);
                    ReflectionTestUtils.setField(flashcard, "createdAt", now);
                    ReflectionTestUtils.setField(flashcard, "updatedAt", now);

                    return flashcard;
                });

        //eseguiamo il metodo da testare
        FlashcardResponse response = flashcardService.createFlashcard(request);

        //verifica dei dati restituiti.
        assertEquals(10L, response.id());
        assertEquals(subjectId, response.subjectId());
        assertEquals("cos'è JWT?", response.question());
        assertEquals("è uno standard aperto utilizzato per trasmettere in modo sicuro e compatto informazioni tra le due parti sotto forma di JSON", response.answer());
        assertEquals(now, response.createdAt());
        assertEquals(now, response.updatedAt());

        //Verifica dell'oggetto passato alla Repo
        ArgumentCaptor<Flashcard> captor = ArgumentCaptor.forClass(Flashcard.class);

        verify(flashcardRepository).save(captor.capture());

        Flashcard savedFlashcard = captor.getValue();

        assertSame(user, savedFlashcard.getUser());
        assertSame(subject, savedFlashcard.getSubject());
    }

    @Test //utente inesistente
    void createFlashcard_UserNotFound(){

        Long userId = 1L;
        Long subjectId = 2L;

        FlashcardRequest request = new FlashcardRequest(
                subjectId,
                "cos'è una classe?",
                "è un modello che definisce le caratteristiche e i comportamenti che un oggetto potrà avere"
        );

        when(currentUserIdProvider.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> flashcardService.createFlashcard(request)
        );

        //verifichiamo che non venga cercata la materia e la flashcard non venga salvata
        verifyNoInteractions(subjectRepository, flashcardRepository );
    }

    @Test //Materia inesistente
    void createFlashcard_SubjectNotFound(){

        long userId = 1L;
        Long subjectId = 2L;

        User user = mock(User.class);

        FlashcardRequest request = new FlashcardRequest(
                subjectId,
                "cos'è una classe?",
                "è un modello che definisce le caratteristiche e i comportamenti che un oggetto potrà avere"
        );

        when(currentUserIdProvider.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subjectRepository.findByIdAndUser_Id(subjectId, userId)).thenReturn(Optional.empty());

        assertThrows(SubjectNotFoundException.class, () -> flashcardService.createFlashcard(request));

        verifyNoInteractions(flashcardRepository );
    }

    @Test //Recupero flashcard personali
    void getAllFlashcards_Test() {

        Long userId = 1L;
        Long subjectId = 2L;

        User user = mock(User.class);
        Subject subject = mock(Subject.class);

        when(subject.getId()).thenReturn(subjectId);

        Flashcard flashcard1 = new Flashcard(
                user,
                subject,
                "cos'è una classe?",
                "è un modello che definisce le caratteristiche e i comportamenti che un oggetto potrà avere"
        );

        Flashcard flashcard2 = new Flashcard(
                user,
                subject,
                "cos'è JWT?",
                "è uno standard aperto utilizzato per trasmettere in modo sicuro e compatto " +
                        "informazioni tra le due parti sotto forma di JSON"
        );

        ReflectionTestUtils.setField(flashcard1, "id", 10L);
        ReflectionTestUtils.setField(flashcard2, "id", 11L);

        when(currentUserIdProvider.getCurrentUserId()).thenReturn(userId);
        when(flashcardRepository.findAllByUser_IdOrderByIdAsc(userId))
                .thenReturn(List.of(flashcard1, flashcard2));

        List<FlashcardResponse> response = flashcardService.getAllFlashcards();

        assertEquals(10L, response.get(0).id());
        assertEquals(subjectId, response.get(0).subjectId());
        assertEquals("cos'è una classe?", response.get(0).question());
        assertEquals("è un modello che definisce le caratteristiche e i comportamenti che un oggetto potrà avere", response.get(0).answer());

        assertEquals(11L, response.get(1).id());
        assertEquals(subjectId, response.get(1).subjectId());
        assertEquals("cos'è JWT?", response.get(1).question());

        verify(flashcardRepository).findAllByUser_IdOrderByIdAsc(userId);
    }

    @Test //nessuna flashcard personale
    void getAllFlashcards_EmptyListTest() {

        Long userId = 1L;

        when(currentUserIdProvider.getCurrentUserId()).thenReturn(userId);
        when(flashcardRepository.findAllByUser_IdOrderByIdAsc(userId)).thenReturn(List.of());

        List<FlashcardResponse> response = flashcardService.getAllFlashcards();

        assertNotNull(response);
        assertTrue(response.isEmpty());
        verify(flashcardRepository).findAllByUser_IdOrderByIdAsc(userId);
    }

    @Test //Flashcard senza materia
    void getAllFlashcards_FlashcardWithoutSubjectTest() {

        Long userId = 1L;
        User user = mock(User.class);

        Flashcard flashcard = new Flashcard(
                user,
                null,
                "Domanda di prova",
                "Risposta di prova"
        );

        ReflectionTestUtils.setField(flashcard, "id", 10L);

        when(currentUserIdProvider.getCurrentUserId()).thenReturn(userId);
        when(flashcardRepository.findAllByUser_IdOrderByIdAsc(userId)).thenReturn(List.of(flashcard));

        List<FlashcardResponse> response = flashcardService.getAllFlashcards();

        assertEquals(1, response.size());
        assertEquals(10L, response.get(0).id());
        assertNull(response.get(0).subjectId());
        assertEquals("Domanda di prova", response.get(0).question());
        assertEquals("Risposta di prova", response.get(0).answer());

        verify(flashcardRepository).findAllByUser_IdOrderByIdAsc(userId);
    }


    @Test //Recupero flashcard per materia
    void getFlashcardBySubjectId_Test() {

        Long userId = 1L;
        Long subjectId = 2L;

        User user = mock(User.class);
        Subject subject = mock(Subject.class);

        when(subject.getId()).thenReturn(subjectId);

        Flashcard flashcard = new Flashcard(
                user,
                subject,
                "cos'è MVC?",
                "Model View Controller"
        );

        ReflectionTestUtils.setField(flashcard, "id", 10L);

        when(currentUserIdProvider.getCurrentUserId()).thenReturn(userId);
        when(subjectRepository.findByIdAndUser_Id(subjectId, userId)).thenReturn(Optional.of(subject));
        when(flashcardRepository.findAllByUser_IdAndSubject_IdOrderByIdAsc(userId, subjectId)).thenReturn(List.of(flashcard));

        List<FlashcardResponse> response = flashcardService.getFlashcardsBySubjectId(subjectId);

        assertEquals(1, response.size());
        assertEquals(10L, response.get(0).id());
        assertEquals(subjectId, response.get(0).subjectId());
        assertEquals("cos'è MVC?", response.get(0).question());
        assertEquals("Model View Controller", response.get(0).answer());

        verify(subjectRepository).findByIdAndUser_Id(subjectId, userId);
        verify(flashcardRepository).findAllByUser_IdAndSubject_IdOrderByIdAsc(userId, subjectId);
    }

    @Test //Materia senza flashcard
    void getFlashcardBySubjectId_EmptyList(){

        Long userId = 1L;
        Long subjectId = 2L;

        Subject subject = mock(Subject.class);

        when(currentUserIdProvider.getCurrentUserId()).thenReturn(userId);
        when(subjectRepository.findByIdAndUser_Id(subjectId, userId)).thenReturn(Optional.of(subject));
        when(flashcardRepository.findAllByUser_IdAndSubject_IdOrderByIdAsc(userId, subjectId)).thenReturn(List.of());

        List<FlashcardResponse> response = flashcardService.getFlashcardsBySubjectId(subjectId);

        assertNotNull(response);
        assertTrue(response.isEmpty());

        verify(subjectRepository).findByIdAndUser_Id(subjectId, userId);
        verify(flashcardRepository).findAllByUser_IdAndSubject_IdOrderByIdAsc(userId, subjectId);
    }

    @Test //Materia inesistente
    void getFlashcardBySubjectId_SubjectNotFound(){

        Long userId = 1L;
        Long subjectId = 2L;

        when(currentUserIdProvider.getCurrentUserId()).thenReturn(userId);
        when(subjectRepository.findByIdAndUser_Id(subjectId, userId)).thenReturn(Optional.empty());

        assertThrows(SubjectNotFoundException.class, () -> flashcardService.getFlashcardsBySubjectId(subjectId));

        verify(subjectRepository).findByIdAndUser_Id(subjectId, userId);
        verifyNoInteractions(flashcardRepository);
    }

    @Test //Recupero flashcard tramite id
    void getFlashcardById_Test(){

        Long userId = 1L;
        Long subjectId = 2L;
        Long flashcardId = 10L;

        User user = mock(User.class);
        Subject subject = mock(Subject.class);

        when(subject.getId()).thenReturn(subjectId);

        Flashcard flashcard = new Flashcard(
                user,
                subject,
                "cos'è una classe?",
                "è un modello che definisce le caratteristiche e i comportamenti che un oggetto potrà avere"
        );

        Instant now = Instant.now();

        ReflectionTestUtils.setField(flashcard, "id", flashcardId);
        ReflectionTestUtils.setField(flashcard, "createdAt", now);
        ReflectionTestUtils.setField(flashcard, "updatedAt", now);

        when(currentUserIdProvider.getCurrentUserId()).thenReturn(userId);
        when(flashcardRepository.findByIdAndUser_Id(flashcardId, userId)).thenReturn(Optional.of(flashcard));

        FlashcardResponse response = flashcardService.getFlashcardById(flashcardId);

        assertEquals(flashcardId, response.id());
        assertEquals(subjectId, response.subjectId());
        assertEquals("cos'è una classe?", response.question());
        assertEquals("è un modello che definisce le caratteristiche e i comportamenti che un oggetto potrà avere", response.answer());
        assertEquals(now, response.createdAt());
        assertEquals(now, response.updatedAt());

        verify(flashcardRepository).findByIdAndUser_Id(flashcardId, userId);
    }

    @Test //Flashcard inesistente o di un altro utente
    void getFlashcardById_FlashcardNotFound(){

        Long userId = 1L;
        Long flashcardId = 10L;

        when(currentUserIdProvider.getCurrentUserId()).thenReturn(userId);
        when(flashcardRepository.findByIdAndUser_Id(flashcardId, userId)).thenReturn(Optional.empty());

        assertThrows(FlashcardNotFoundException.class, () -> flashcardService.getFlashcardById(flashcardId));

        verify(flashcardRepository).findByIdAndUser_Id(flashcardId, userId);
    }

    @Test //Modifica corretta della flashcard
    void updateFlashcard_Test(){

        Long userId = 1L;
        Long subjectId = 2L;
        Long flashcardId = 10L;

        User user = mock(User.class);
        Subject subject = mock(Subject.class);

        when(subject.getId()).thenReturn(subjectId);

        Flashcard flashcard = new Flashcard(
                user,
                subject,
                "cos'è una classe?",
                "è un modello per creare oggetti"
        );

        Instant createdAt = Instant.parse("2026-11-10T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-11-12T10:00:00Z");

        ReflectionTestUtils.setField(flashcard, "id", flashcardId);
        ReflectionTestUtils.setField(flashcard, "createdAt", createdAt);
        ReflectionTestUtils.setField(flashcard, "updatedAt", createdAt);

        FlashcardUpdateRequest request = new FlashcardUpdateRequest(
                "cos'è il polimorfismo?",
                "è la possibilità di assumere comportamenti differenti"
        );

        when(currentUserIdProvider.getCurrentUserId()).thenReturn(userId);
        when(flashcardRepository.findByIdAndUser_Id(flashcardId, userId)).thenReturn(Optional.of(flashcard));

        //Simuliamo il salvataggio della flashcard modificata
        when(flashcardRepository.saveAndFlush(flashcard))
                .thenAnswer(invocation -> {

                    Flashcard updatedFlashcard = invocation.getArgument(0);

                    ReflectionTestUtils.setField(updatedFlashcard, "updatedAt", updatedAt);

                    return updatedFlashcard;
                });

        FlashcardResponse response = flashcardService.updateFlashcard(flashcardId, request);

        assertEquals(flashcardId, response.id());
        assertEquals(subjectId, response.subjectId());
        assertEquals("cos'è il polimorfismo?", response.question());
        assertEquals("è la possibilità di assumere comportamenti differenti", response.answer());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());

        assertSame(user, flashcard.getUser());
        assertSame(subject, flashcard.getSubject());

        verify(flashcardRepository).findByIdAndUser_Id(flashcardId, userId);
        verify(flashcardRepository).saveAndFlush(flashcard);
    }

    @Test //Modifica di una flashcard inesistente
    void updateFlashcard_FlashcardNotFound(){

        Long userId = 1L;
        Long flashcardId = 10L;

        FlashcardUpdateRequest request = new FlashcardUpdateRequest(
                "Nuova domanda",
                "Nuova risposta"
        );

        when(currentUserIdProvider.getCurrentUserId()).thenReturn(userId);
        when(flashcardRepository.findByIdAndUser_Id(flashcardId, userId)).thenReturn(Optional.empty());

        assertThrows(FlashcardNotFoundException.class, () -> flashcardService.updateFlashcard(flashcardId, request));

        verify(flashcardRepository).findByIdAndUser_Id(flashcardId, userId);
        verify(flashcardRepository, never()).saveAndFlush(any(Flashcard.class));
    }

    @Test //Eliminazione corretta della flashcard
    void deleteFlashcard_Test(){

        Long userId = 1L;
        Long flashcardId = 10L;

        User user = mock(User.class);
        Subject subject = mock(Subject.class);

        Flashcard flashcard = new Flashcard(
                user,
                subject,
                "Domanda di prova",
                "Risposta di prova"
        );

        ReflectionTestUtils.setField(flashcard, "id", flashcardId);

        when(currentUserIdProvider.getCurrentUserId()).thenReturn(userId);
        when(flashcardRepository.findByIdAndUser_Id(flashcardId, userId)).thenReturn(Optional.of(flashcard));

        flashcardService.deleteFlashcard(flashcardId);

        verify(flashcardRepository).findByIdAndUser_Id(flashcardId, userId);
        verify(flashcardRepository).delete(flashcard);
    }

    @Test //Eliminazione di una flashcard inesistente
    void deleteFlashcard_FlashcardNotFound(){

        Long userId = 1L;
        Long flashcardId = 10L;

        when(currentUserIdProvider.getCurrentUserId()).thenReturn(userId);
        when(flashcardRepository.findByIdAndUser_Id(flashcardId, userId)).thenReturn(Optional.empty());

        assertThrows(FlashcardNotFoundException.class, () -> flashcardService.deleteFlashcard(flashcardId));

        verify(flashcardRepository).findByIdAndUser_Id(flashcardId, userId);
        verify(flashcardRepository, never()).delete(any(Flashcard.class));
    }

    @Test //Duplicazione corretta della flashcard
    void duplicateFlashcard_Test(){

        Long userId = 1L;
        Long subjectId = 2L;
        Long flashcardId = 10L;

        User user = mock(User.class);
        Subject subject = mock(Subject.class);

        when(subject.getId()).thenReturn(subjectId);

        Flashcard original = new Flashcard(
                user,
                subject,
                "cos'è MVC?",
                "Model View Controller"
        );

        Instant createdAt = Instant.parse("2026-11-10T10:00:00Z");
        Instant now = Instant.parse("2026-11-12T10:00:00Z");

        ReflectionTestUtils.setField(original, "id", flashcardId);
        ReflectionTestUtils.setField(original, "createdAt", createdAt);
        ReflectionTestUtils.setField(original, "updatedAt", createdAt);

        when(currentUserIdProvider.getCurrentUserId()).thenReturn(userId);
        when(flashcardRepository.findByIdAndUser_Id(flashcardId, userId)).thenReturn(Optional.of(original));

        //Simuliamo il salvataggio della nuova flashcard
        when(flashcardRepository.save(any(Flashcard.class)))
                .thenAnswer(invocation -> {

                    Flashcard duplicate = invocation.getArgument(0);

                    ReflectionTestUtils.setField(duplicate, "id", 11L);
                    ReflectionTestUtils.setField(duplicate, "createdAt", now);
                    ReflectionTestUtils.setField(duplicate, "updatedAt", now);

                    return duplicate;
                });

        FlashcardResponse response = flashcardService.duplicateFlashcard(flashcardId);

        assertEquals(11L, response.id());
        assertEquals(subjectId, response.subjectId());
        assertEquals(original.getQuestion(), response.question());
        assertEquals(original.getAnswer(), response.answer());
        assertEquals(now, response.createdAt());
        assertEquals(now, response.updatedAt());

        //Verifica dell'oggetto passato alla Repo
        ArgumentCaptor<Flashcard> captor = ArgumentCaptor.forClass(Flashcard.class);

        verify(flashcardRepository).save(captor.capture());

        Flashcard duplicate = captor.getValue();

        assertNotSame(original, duplicate);
        assertSame(user, duplicate.getUser());
        assertSame(subject, duplicate.getSubject());

        //Verifichiamo che la flashcard originale non venga modificata
        assertEquals(flashcardId, original.getId());
        assertEquals(createdAt, original.getCreatedAt());
        assertEquals(createdAt, original.getUpdatedAt());
        assertEquals("cos'è MVC?", original.getQuestion());
        assertEquals("Model View Controller", original.getAnswer());

        verify(flashcardRepository).findByIdAndUser_Id(flashcardId, userId);
    }

    @Test //Duplicazione di una flashcard inesistente
    void duplicateFlashcard_FlashcardNotFound(){

        Long userId = 1L;
        Long flashcardId = 10L;

        when(currentUserIdProvider.getCurrentUserId()).thenReturn(userId);
        when(flashcardRepository.findByIdAndUser_Id(flashcardId, userId)).thenReturn(Optional.empty());

        assertThrows(FlashcardNotFoundException.class, () -> flashcardService.duplicateFlashcard(flashcardId));

        verify(flashcardRepository).findByIdAndUser_Id(flashcardId, userId);
        verify(flashcardRepository, never()).save(any(Flashcard.class));
    }

}


