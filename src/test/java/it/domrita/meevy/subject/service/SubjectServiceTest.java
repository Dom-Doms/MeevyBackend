package it.domrita.meevy.subject.service;

import it.domrita.meevy.auth.repository.CurrentUserIdProvider;
import it.domrita.meevy.subject.exception.SubjectNotFoundException;
import it.domrita.meevy.subject.repository.SubjectRepository;
import it.domrita.meevy.user.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import it.domrita.meevy.subject.dto.SubjectRequest;
import it.domrita.meevy.subject.dto.SubjectResponse;
import it.domrita.meevy.subject.model.Subject;
import it.domrita.meevy.user.model.User;

import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class SubjectServiceTest {

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserIdProvider currentUserIdProvider;

    @InjectMocks
    private SubjectService subjectService;


    @Test
    void createSubject_salvaMateriaAssociataAllUtente() {

        // 1. PREPARAZIONE
        // Simuliamo un utente con ID 1.
        Long userId = 1L;
        User user = mock(User.class);

        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(userId);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        // Simuliamo il salvataggio della materia.
        when(subjectRepository.save(any(Subject.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 2. ESECUZIONE
        SubjectRequest request = new SubjectRequest("Analisi 1");

        SubjectResponse response = subjectService.createSubject(request);

        // 3. VERIFICA
        assertEquals("Analisi 1", response.name());

        ArgumentCaptor<Subject> captor =
                ArgumentCaptor.forClass(Subject.class);

        verify(subjectRepository).save(captor.capture());

        Subject savedSubject = captor.getValue();

        assertEquals("Analisi 1", savedSubject.getName());
        assertSame(user, savedSubject.getUser());
    }


    @Test
    void createSubject_utenteInesistente_nonSalvaMateria() {

        // PREPARAZIONE: simuliamo un utente inesistente.
        Long userId = 999L;

        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(userId);

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        SubjectRequest request = new SubjectRequest("Analisi 1");

        // ESECUZIONE E VERIFICA: deve generare un'eccezione.
        assertThrows(
                IllegalStateException.class,
                () -> subjectService.createSubject(request)
        );

        // Verifichiamo che nessuna materia venga salvata.
        verify(subjectRepository, never()).save(any(Subject.class));
    }


    @Test
    void getAllSubjects_restituisceMaterieUtenteCorrente() {

        // PREPARAZIONE
        Long userId = 1L;
        User user = mock(User.class);

        Subject subject1 = new Subject("Analisi 1", user);
        Subject subject2 = new Subject("Basi di dati", user);

        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(userId);

        when(subjectRepository.findAllByUser_IdOrderByIdAsc(userId))
                .thenReturn(List.of(subject1, subject2));

        // ESECUZIONE
        List<SubjectResponse> response = subjectService.getAllSubjects();

        // VERIFICA
        assertEquals(2, response.size());
        assertEquals("Analisi 1", response.get(0).name());
        assertEquals("Basi di dati", response.get(1).name());

        verify(subjectRepository)
                .findAllByUser_IdOrderByIdAsc(userId);
    }


    @Test
    void getAllSubjects_nessunaMateria_restituisceListaVuota() {

        // PREPARAZIONE
        Long userId = 1L;

        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(userId);

        // Simuliamo un utente senza materie.
        when(subjectRepository.findAllByUser_IdOrderByIdAsc(userId))
                .thenReturn(List.of());

        // ESECUZIONE
        List<SubjectResponse> response = subjectService.getAllSubjects();

        // VERIFICA
        assertNotNull(response);
        assertTrue(response.isEmpty());

        verify(subjectRepository)
                .findAllByUser_IdOrderByIdAsc(userId);
    }


    @Test
    void getSubjectById_materiaEsistente_restituisceMateria() {

        // PREPARAZIONE
        Long userId = 1L;
        Long subjectId = 5L;

        User user = mock(User.class);
        Subject subject = new Subject("Analisi 1", user);

        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(userId);

        when(subjectRepository.findByIdAndUser_Id(subjectId, userId))
                .thenReturn(Optional.of(subject));

        // ESECUZIONE
        SubjectResponse response = subjectService.getSubjectById(subjectId);

        // VERIFICA
        assertNotNull(response);
        assertEquals("Analisi 1", response.name());

        verify(subjectRepository)
                .findByIdAndUser_Id(subjectId, userId);
    }


    @Test
    void getSubjectById_materiaInesistente_lanciaEccezione() {

        // PREPARAZIONE
        Long userId = 1L;
        Long subjectId = 999L;

        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(userId);

        // Simuliamo una materia non trovata.
        when(subjectRepository.findByIdAndUser_Id(subjectId, userId))
                .thenReturn(Optional.empty());

        // ESECUZIONE E VERIFICA
        assertThrows(
                SubjectNotFoundException.class,
                () -> subjectService.getSubjectById(subjectId)
        );

        // Verifica che la ricerca rispetti il proprietario.
        verify(subjectRepository)
                .findByIdAndUser_Id(subjectId, userId);
    }


    @Test
    void updateSubject_materiaEsistente_modificaNome() {

        // 1. PREPARAZIONE
        Long userId = 1L;
        Long subjectId = 5L;

        User user = mock(User.class);
        Subject subject = new Subject("Analisi 1", user);

        SubjectRequest request =
                new SubjectRequest("Analisi Matematica 1");

        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(userId);

        when(subjectRepository.findByIdAndUser_Id(subjectId, userId))
                .thenReturn(Optional.of(subject));

        when(subjectRepository.save(any(Subject.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // 2. ESECUZIONE
        SubjectResponse response =
                subjectService.updateSubject(subjectId, request);

        // 3. VERIFICA
        assertEquals("Analisi Matematica 1", response.name());

        assertEquals("Analisi Matematica 1", subject.getName());

        verify(subjectRepository)
                .findByIdAndUser_Id(subjectId, userId);

        verify(subjectRepository).save(subject);
    }


    @Test
    void updateSubject_materiaInesistente_lanciaEccezione() {

        // 1. PREPARAZIONE
        Long userId = 1L;
        Long subjectId = 999L;

        SubjectRequest request =
                new SubjectRequest("Analisi Matematica 1");

        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(userId);

        // Simuliamo una materia non trovata.
        when(subjectRepository.findByIdAndUser_Id(subjectId, userId))
                .thenReturn(Optional.empty());

        // 2. ESECUZIONE E VERIFICA
        assertThrows(
                SubjectNotFoundException.class,
                () -> subjectService.updateSubject(subjectId, request)
        );

        // Verifica che la ricerca controlli anche il proprietario.
        verify(subjectRepository)
                .findByIdAndUser_Id(subjectId, userId);

        // Verifica che non venga salvata nessuna materia.
        verify(subjectRepository, never())
                .save(any(Subject.class));
    }


    @Test
    void deleteSubject_materiaEsistente_eliminaMateria() {

        // 1. PREPARAZIONE
        Long userId = 1L;
        Long subjectId = 5L;

        User user = mock(User.class);
        Subject subject = new Subject("Analisi 1", user);

        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(userId);

        when(subjectRepository.findByIdAndUser_Id(subjectId, userId))
                .thenReturn(Optional.of(subject));

        // 2. ESECUZIONE
        subjectService.deleteSubject(subjectId);

        // 3. VERIFICA
        verify(subjectRepository)
                .findByIdAndUser_Id(subjectId, userId);

        verify(subjectRepository)
                .delete(subject);
    }


    @Test
    void deleteSubject_materiaInesistente_lanciaEccezione() {

        // 1. PREPARAZIONE
        Long userId = 1L;
        Long subjectId = 999L;

        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(userId);

        // Simuliamo una materia non trovata.
        when(subjectRepository.findByIdAndUser_Id(subjectId, userId))
                .thenReturn(Optional.empty());

        // 2. ESECUZIONE E VERIFICA
        assertThrows(
                SubjectNotFoundException.class,
                () -> subjectService.deleteSubject(subjectId)
        );

        // Verifica che la ricerca consideri anche il proprietario.
        verify(subjectRepository)
                .findByIdAndUser_Id(subjectId, userId);

        // Verifica che nessuna materia venga eliminata.
        verify(subjectRepository, never())
                .delete(any(Subject.class));
    }





}
