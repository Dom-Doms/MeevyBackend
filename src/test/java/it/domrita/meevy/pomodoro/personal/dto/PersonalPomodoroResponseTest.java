
package it.domrita.meevy.pomodoro.personal.dto;

import it.domrita.meevy.pomodoro.common.PomodoroPhase;
import it.domrita.meevy.pomodoro.common.PomodoroSettings;
import it.domrita.meevy.pomodoro.personal.model.PersonalPomodoroSession;
import it.domrita.meevy.pomodoro.personal.role.PersonalPomodoroStatus;
import it.domrita.meevy.subject.model.Subject;
import it.domrita.meevy.user.model.User;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PersonalPomodoroResponseTest {

    private static final Instant T0 =
            Instant.parse("2026-10-28T10:00:00Z");

    private PersonalPomodoroSession newSession() {
        Subject subject = mock(Subject.class);
        when(subject.getId()).thenReturn(2L);

        return new PersonalPomodoroSession(
                mock(User.class),
                subject,
                new PomodoroSettings(1500, 300, 10800)
        );
    }

    /*
     * 1. Sessione READY: timer non ancora partito.
     */
    @Test
    void shouldMapReadySession() {
        PersonalPomodoroSession session = newSession();

        PersonalPomodoroResponse response =
                PersonalPomodoroResponse.from(session, T0);

        assertEquals(PersonalPomodoroStatus.READY, response.status());
        assertEquals(2L, response.subjectId());
        assertNull(response.phase());
        assertEquals(0, response.elapsedSeconds());
        assertEquals(0, response.remainingPhaseSeconds());
        assertEquals(10800, response.remainingSessionSeconds());
        assertEquals(0, response.plannedStudySeconds());
    }

    /*
     * 2. Sessione RUNNING: calcola fase e tempi.
     */
    @Test
    void shouldMapRunningSession() {
        PersonalPomodoroSession session = newSession();
        session.start(T0);

        PersonalPomodoroResponse response =
                PersonalPomodoroResponse.from(
                        session,
                        T0.plusSeconds(600)
                );

        assertEquals(PersonalPomodoroStatus.RUNNING, response.status());
        assertEquals(PomodoroPhase.STUDY, response.phase());
        assertEquals(600, response.elapsedSeconds());
        assertEquals(900, response.remainingPhaseSeconds());
        assertEquals(10200, response.remainingSessionSeconds());
        assertEquals(600, response.plannedStudySeconds());
    }

    /*
     * 3. Sessione PAUSED: il tempo deve restare fermo.
     */
    @Test
    void shouldFreezeTimeWhenPaused() {
        PersonalPomodoroSession session = newSession();
        session.start(T0);
        session.pause(T0.plusSeconds(600));

        PersonalPomodoroResponse response =
                PersonalPomodoroResponse.from(
                        session,
                        T0.plusSeconds(1800)
                );

        assertEquals(PersonalPomodoroStatus.PAUSED, response.status());
        assertEquals(600, response.elapsedSeconds());
        assertEquals(900, response.remainingPhaseSeconds());
        assertEquals(10200, response.remainingSessionSeconds());
    }

    /*
     * 4. Sessione COMPLETED: nessun tempo residuo.
     */
    @Test
    void shouldMapCompletedSession() {
        PersonalPomodoroSession session = newSession();
        session.start(T0);
        session.complete(T0.plusSeconds(1800));

        PersonalPomodoroResponse response =
                PersonalPomodoroResponse.from(
                        session,
                        T0.plusSeconds(3600)
                );

        assertEquals(PersonalPomodoroStatus.COMPLETED, response.status());
        assertNull(response.phase());
        assertEquals(0, response.remainingPhaseSeconds());
        assertEquals(0, response.remainingSessionSeconds());
        assertEquals(1500, response.actualStudySeconds());
        assertEquals(1500, response.plannedStudySeconds());
    }

    /*
     * 5. Sessione CANCELLED: nessuno studio contabilizzato.
     */
    @Test
    void shouldMapCancelledSession() {
        PersonalPomodoroSession session = newSession();
        session.start(T0);
        session.cancel(T0.plusSeconds(900));

        PersonalPomodoroResponse response =
                PersonalPomodoroResponse.from(
                        session,
                        T0.plusSeconds(1800)
                );

        assertEquals(PersonalPomodoroStatus.CANCELLED, response.status());
        assertNull(response.phase());
        assertEquals(0, response.remainingSessionSeconds());
        assertEquals(0, response.actualStudySeconds());
        assertEquals(0, response.plannedStudySeconds());
    }

    /*
     * 6. Se la Subject viene eliminata,
     * la sessione deve rimanere rappresentabile.
     */
    @Test
    void shouldHandleDeletedSubject() {
        PersonalPomodoroSession session = newSession();

        ReflectionTestUtils.setField(
                session,
                "subject",
                null
        );

        PersonalPomodoroResponse response =
                PersonalPomodoroResponse.from(session, T0);

        assertNull(response.subjectId());
        assertEquals(PersonalPomodoroStatus.READY, response.status());
    }

    /*
     * 7. Verifica la conversione dello storico paginato.
     */
    @Test
    void shouldMapPaginatedHistory() {
        var pageable = PageRequest.of(1, 2);

        var page = new PageImpl<>(
                List.of(newSession()),
                pageable,
                5
        );

        PersonalPomodoroPageResponse response =
                PersonalPomodoroPageResponse.from(page, T0);

        assertEquals(1, response.page());
        assertEquals(2, response.size());
        assertEquals(5, response.totalElements());
        assertEquals(3, response.totalPages());
        assertEquals(1, response.content().size());
        assertEquals(
                PersonalPomodoroStatus.READY,
                response.content().getFirst().status()
        );
    }
}
