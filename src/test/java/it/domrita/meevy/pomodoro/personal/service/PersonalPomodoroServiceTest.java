
package it.domrita.meevy.pomodoro.personal.service;

import it.domrita.meevy.auth.repository.CurrentUserIdProvider;
import it.domrita.meevy.pomodoro.common.PomodoroSettings;
import it.domrita.meevy.pomodoro.personal.exception.*;
import it.domrita.meevy.pomodoro.personal.model.PersonalPomodoroSession;
import it.domrita.meevy.pomodoro.personal.repository.PersonalPomodoroRepository;
import it.domrita.meevy.pomodoro.personal.role.PersonalPomodoroStatus;
import it.domrita.meevy.subject.exception.SubjectNotFoundException;
import it.domrita.meevy.subject.model.Subject;
import it.domrita.meevy.subject.repository.SubjectRepository;
import it.domrita.meevy.user.model.User;
import it.domrita.meevy.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalPomodoroServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long SUBJECT_ID = 2L;
    private static final Long SESSION_ID = 3L;

    private final PomodoroSettings settings =
            new PomodoroSettings(1500, 300, 10800);

    @Mock
    private PersonalPomodoroRepository pomodoroRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserIdProvider currentUserIdProvider;

    @InjectMocks
    private PersonalPomodoroService service;

    @BeforeEach
    void setUp() {
        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(USER_ID);
    }

    private PersonalPomodoroSession newSession() {
        return new PersonalPomodoroSession(
                mock(User.class),
                mock(Subject.class),
                settings
        );
    }

    /*
     * 1. Creazione di una sessione valida.
     */
    @Test
    void shouldCreateReadySession() {
        User user = mock(User.class);
        Subject subject = mock(Subject.class);

        when(pomodoroRepository.findByUser_IdAndStatusIn(
                eq(USER_ID), anyCollection()
        )).thenReturn(Optional.empty());

        when(subjectRepository.findByIdAndUser_Id(
                SUBJECT_ID, USER_ID
        )).thenReturn(Optional.of(subject));

        when(userRepository.getReferenceById(USER_ID))
                .thenReturn(user);

        when(pomodoroRepository.saveAndFlush(
                any(PersonalPomodoroSession.class)
        )).thenAnswer(invocation -> invocation.getArgument(0));

        PersonalPomodoroSession result =
                service.create(SUBJECT_ID, settings);

        assertEquals(PersonalPomodoroStatus.READY,
                result.getStatus());
        assertSame(user, result.getUser());
        assertSame(subject, result.getSubject());
        assertEquals(10800, result.getTargetDurationSeconds());

        verify(pomodoroRepository).saveAndFlush(
                any(PersonalPomodoroSession.class)
        );
    }

    /*
     * 2. Non permette due sessioni non terminate.
     */
    @Test
    void shouldRejectSecondActiveSession() {
        PersonalPomodoroSession existing = newSession();

        when(pomodoroRepository.findByUser_IdAndStatusIn(
                eq(USER_ID), anyCollection()
        )).thenReturn(Optional.of(existing));

        assertThrows(
                PersonalPomodoroAlreadyActiveException.class,
                () -> service.create(SUBJECT_ID, settings)
        );

        verify(pomodoroRepository, never())
                .saveAndFlush(any(PersonalPomodoroSession.class));
    }

    /*
     * 3. Non permette di usare una materia
     * inesistente o appartenente a un altro utente.
     */
    @Test
    void shouldRejectSubjectNotOwnedByUser() {
        when(pomodoroRepository.findByUser_IdAndStatusIn(
                eq(USER_ID), anyCollection()
        )).thenReturn(Optional.empty());

        when(subjectRepository.findByIdAndUser_Id(
                SUBJECT_ID, USER_ID
        )).thenReturn(Optional.empty());

        assertThrows(
                SubjectNotFoundException.class,
                () -> service.create(SUBJECT_ID, settings)
        );

        verify(pomodoroRepository, never())
                .saveAndFlush(any(PersonalPomodoroSession.class));
    }

    /*
     * 4. Avvio corretto della sessione.
     */
    @Test
    void shouldStartReadySession() {
        PersonalPomodoroSession session = newSession();

        when(pomodoroRepository.findByIdAndUser_Id(
                SESSION_ID, USER_ID
        )).thenReturn(Optional.of(session));

        PersonalPomodoroSession result =
                service.start(SESSION_ID);

        assertEquals(PersonalPomodoroStatus.RUNNING,
                result.getStatus());
        assertNotNull(result.getStartedAt());
    }

    /*
     * 5. Non permette di avviare due volte
     * la stessa sessione.
     */
    @Test
    void shouldRejectStartingTwice() {
        PersonalPomodoroSession session = newSession();

        when(pomodoroRepository.findByIdAndUser_Id(
                SESSION_ID, USER_ID
        )).thenReturn(Optional.of(session));

        service.start(SESSION_ID);

        assertThrows(
                PersonalPomodoroInvalidStateException.class,
                () -> service.start(SESSION_ID)
        );
    }

    /*
     * 6. Non permette di recuperare una sessione
     * non appartenente all'utente.
     */
    @Test
    void shouldRejectSessionNotOwnedByUser() {
        when(pomodoroRepository.findByIdAndUser_Id(
                SESSION_ID, USER_ID
        )).thenReturn(Optional.empty());

        assertThrows(
                PersonalPomodoroNotFoundException.class,
                () -> service.getById(SESSION_ID)
        );
    }

    /*
     * 7. Verifica pausa e ripresa.
     */
    @Test
    void shouldPauseAndResumeSession() {
        PersonalPomodoroSession session = newSession();

        session.start(Instant.now().minusSeconds(30));

        when(pomodoroRepository.findByIdAndUser_Id(
                SESSION_ID, USER_ID
        )).thenReturn(Optional.of(session));

        service.pause(SESSION_ID);

        assertEquals(PersonalPomodoroStatus.PAUSED,
                session.getStatus());
        assertNotNull(session.getPausedAt());

        service.resume(SESSION_ID);

        assertEquals(PersonalPomodoroStatus.RUNNING,
                session.getStatus());
        assertNull(session.getPausedAt());
    }

    /*
     * 8. Una sessione scaduta viene completata
     * quando si richiede quella corrente.
     */
    @Test
    void shouldCompleteExpiredCurrentSession() {
        PersonalPomodoroSession session =
                new PersonalPomodoroSession(
                        mock(User.class),
                        mock(Subject.class),
                        new PomodoroSettings(20, 10, 60)
                );

        session.start(Instant.now().minusSeconds(120));

        when(pomodoroRepository.findByUser_IdAndStatusIn(
                eq(USER_ID), anyCollection()
        )).thenReturn(Optional.of(session));

        Optional<PersonalPomodoroSession> result =
                service.getCurrent();

        assertTrue(result.isEmpty());

        assertEquals(
                PersonalPomodoroStatus.COMPLETED,
                session.getStatus()
        );

        assertEquals(40, session.getActualStudySeconds());

        assertEquals(
                session.getStartedAt().plusSeconds(60),
                session.getEndedAt()
        );
    }

    /*
     * 9. Annullamento di una sessione READY.
     */
    @Test
    void shouldCancelReadySession() {
        PersonalPomodoroSession session = newSession();

        when(pomodoroRepository.findByIdAndUser_Id(
                SESSION_ID, USER_ID
        )).thenReturn(Optional.of(session));

        service.cancel(SESSION_ID);

        assertEquals(
                PersonalPomodoroStatus.CANCELLED,
                session.getStatus()
        );

        assertEquals(0, session.getActualStudySeconds());
        assertNotNull(session.getEndedAt());
    }


    /*
     * 10. Modifica la configurazione di una sessione READY.
     */
    @Test
    void shouldUpdateReadySession() {
        PersonalPomodoroSession session = newSession();
        Subject newSubject = mock(Subject.class);

        PomodoroSettings newSettings =
                new PomodoroSettings(1800, 600, 7200);

        when(pomodoroRepository.findByIdAndUser_Id(
                SESSION_ID, USER_ID
        )).thenReturn(Optional.of(session));

        when(subjectRepository.findByIdAndUser_Id(
                SUBJECT_ID, USER_ID
        )).thenReturn(Optional.of(newSubject));

        PersonalPomodoroSession result =
                service.updateReady(
                        SESSION_ID,
                        SUBJECT_ID,
                        newSettings
                );

        assertSame(newSubject, result.getSubject());
        assertEquals(1800, result.getStudyDurationSeconds());
        assertEquals(600, result.getBreakDurationSeconds());
        assertEquals(7200, result.getTargetDurationSeconds());
        assertEquals(PersonalPomodoroStatus.READY, result.getStatus());
    }


    /*
     * 11. Non permette di modificare una sessione avviata.
     */
    @Test
    void shouldRejectUpdatingRunningSession() {
        PersonalPomodoroSession session = newSession();

        session.start(Instant.now().minusSeconds(30));

        when(pomodoroRepository.findByIdAndUser_Id(
                SESSION_ID, USER_ID
        )).thenReturn(Optional.of(session));

        when(subjectRepository.findByIdAndUser_Id(
                SUBJECT_ID, USER_ID
        )).thenReturn(Optional.of(mock(Subject.class)));

        PomodoroSettings newSettings =
                new PomodoroSettings(1800, 600, 7200);

        assertThrows(
                PersonalPomodoroInvalidStateException.class,
                () -> service.updateReady(
                        SESSION_ID,
                        SUBJECT_ID,
                        newSettings
                )
        );

        assertEquals(1500, session.getStudyDurationSeconds());
        assertEquals(PersonalPomodoroStatus.RUNNING, session.getStatus());
    }


    /*
     * 12. Permette il completamento anticipato
     * e contabilizza il tempo di studio.
     */
    @Test
    void shouldCompleteSessionEarly() {
        PersonalPomodoroSession session = newSession();

        session.start(Instant.now().minusSeconds(100));

        when(pomodoroRepository.findByIdAndUser_Id(
                SESSION_ID, USER_ID
        )).thenReturn(Optional.of(session));

        PersonalPomodoroSession result =
                service.complete(SESSION_ID);

        assertEquals(
                PersonalPomodoroStatus.COMPLETED,
                result.getStatus()
        );

        assertNotNull(result.getEndedAt());
        assertTrue(result.getActualStudySeconds() >= 100);
        assertTrue(result.getActualStudySeconds() < 1500);
    }


    /*
     * 13. Una sessione scaduta non può essere sospesa.
     * Deve prima essere completata automaticamente.
     */
    @Test
    void shouldRejectPausingExpiredSession() {
        PersonalPomodoroSession session =
                new PersonalPomodoroSession(
                        mock(User.class),
                        mock(Subject.class),
                        new PomodoroSettings(20, 10, 60)
                );

        session.start(Instant.now().minusSeconds(120));

        when(pomodoroRepository.findByIdAndUser_Id(
                SESSION_ID, USER_ID
        )).thenReturn(Optional.of(session));

        assertThrows(
                PersonalPomodoroInvalidStateException.class,
                () -> service.pause(SESSION_ID)
        );

        assertEquals(
                PersonalPomodoroStatus.COMPLETED,
                session.getStatus()
        );

        assertEquals(40, session.getActualStudySeconds());
    }


    /*
     * 14. Permette una nuova sessione se quella
     * precedente è scaduta.
     */
    @Test
    void shouldCreateAfterPreviousSessionExpired() {
        PersonalPomodoroSession expired =
                new PersonalPomodoroSession(
                        mock(User.class),
                        mock(Subject.class),
                        new PomodoroSettings(20, 10, 60)
                );

        expired.start(Instant.now().minusSeconds(120));

        User user = mock(User.class);
        Subject subject = mock(Subject.class);

        when(pomodoroRepository.findByUser_IdAndStatusIn(
                eq(USER_ID), anyCollection()
        )).thenReturn(Optional.of(expired));

        when(subjectRepository.findByIdAndUser_Id(
                SUBJECT_ID, USER_ID
        )).thenReturn(Optional.of(subject));

        when(userRepository.getReferenceById(USER_ID))
                .thenReturn(user);

        when(pomodoroRepository.saveAndFlush(
                any(PersonalPomodoroSession.class)
        )).thenAnswer(invocation -> invocation.getArgument(0));

        PersonalPomodoroSession created =
                service.create(SUBJECT_ID, settings);

        assertEquals(
                PersonalPomodoroStatus.COMPLETED,
                expired.getStatus()
        );

        assertEquals(
                PersonalPomodoroStatus.READY,
                created.getStatus()
        );

        verify(pomodoroRepository).flush();
        verify(pomodoroRepository).saveAndFlush(
                any(PersonalPomodoroSession.class)
        );
    }


    /*
     * 15. Lo storico restituisce le sessioni
     * e riconcilia quelle scadute.
     */
    @Test
    void shouldReconcileExpiredSessionsInHistory() {
        PersonalPomodoroSession expired =
                new PersonalPomodoroSession(
                        mock(User.class),
                        mock(Subject.class),
                        new PomodoroSettings(20, 10, 60)
                );

        expired.start(Instant.now().minusSeconds(120));

        PersonalPomodoroSession ready = newSession();

        PageRequest pageable = PageRequest.of(0, 20);

        Page<PersonalPomodoroSession> page =
                new PageImpl<>(
                        List.of(expired, ready),
                        pageable,
                        2
                );

        when(pomodoroRepository.findByUser_IdOrderByCreatedAtDesc(
                USER_ID, pageable
        )).thenReturn(page);

        Page<PersonalPomodoroSession> result =
                service.listSessions(pageable);

        assertEquals(2, result.getTotalElements());

        assertEquals(
                PersonalPomodoroStatus.COMPLETED,
                expired.getStatus()
        );

        assertEquals(
                PersonalPomodoroStatus.READY,
                ready.getStatus()
        );
    }

}
