package it.domrita.meevy.subject.service;

import it.domrita.meevy.auth.repository.CurrentUserIdProvider;
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

}
