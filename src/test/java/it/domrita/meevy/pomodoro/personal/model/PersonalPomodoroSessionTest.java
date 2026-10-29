
package it.domrita.meevy.pomodoro.personal.model;

import it.domrita.meevy.pomodoro.common.PomodoroSettings;
import it.domrita.meevy.pomodoro.personal.role.PersonalPomodoroStatus;
import it.domrita.meevy.subject.model.Subject;
import it.domrita.meevy.user.model.User;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class PersonalPomodoroSessionTest {

    private final Instant t0 =
            Instant.parse("2026-10-28T10:00:00Z");

    private PersonalPomodoroSession createSession() {
        return new PersonalPomodoroSession(
                mock(User.class),
                mock(Subject.class),
                new PomodoroSettings(1500, 300, 10800)
        );
    }

    @Test
    void shouldCreateSessionInReadyState() {
        PersonalPomodoroSession session = createSession();

        assertEquals(PersonalPomodoroStatus.READY, session.getStatus());
        assertNull(session.getStartedAt());
    }

    @Test
    void shouldStartSession() {
        PersonalPomodoroSession session = createSession();

        session.start(t0);

        assertEquals(PersonalPomodoroStatus.RUNNING, session.getStatus());
        assertEquals(t0, session.getStartedAt());
    }

    @Test
    void shouldRejectStartingTwice() {
        PersonalPomodoroSession session = createSession();

        session.start(t0);

        assertThrows(
                IllegalStateException.class,
                () -> session.start(t0.plusSeconds(10))
        );
    }

    @Test
    void shouldExcludeManualPauseFromElapsedTime() {
        PersonalPomodoroSession session = createSession();

        session.start(t0);
        session.pause(t0.plusSeconds(600));

        assertEquals(
                600,
                session.elapsedSeconds(t0.plusSeconds(900))
        );

        session.resume(t0.plusSeconds(900));

        assertEquals(300, session.getAccumulatedPauseSeconds());
        assertEquals(PersonalPomodoroStatus.RUNNING, session.getStatus());
    }

    @Test
    void shouldCompleteAutomaticallyAfterTargetDuration() {
        PersonalPomodoroSession session = createSession();

        session.start(t0);

        boolean completed =
                session.completeIfExpired(t0.plusSeconds(10801));

        assertTrue(completed);
        assertEquals(PersonalPomodoroStatus.COMPLETED, session.getStatus());
        assertEquals(9000, session.getActualStudySeconds());
        assertEquals(t0.plusSeconds(10800), session.getEndedAt());
    }

    @Test
    void shouldCancelWithoutCreditingStudyTime() {
        PersonalPomodoroSession session = createSession();

        session.start(t0);
        session.cancel(t0.plusSeconds(1200));

        assertEquals(PersonalPomodoroStatus.CANCELLED, session.getStatus());
        assertEquals(0, session.getActualStudySeconds());
    }
}
