
package it.domrita.meevy.pomodoro.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PomodoroCycleCalculatorTest {

    @Test
    void shouldCalculateThirdStudyCycleAfterSeventyMinutes() {

        PomodoroSettings settings =
                new PomodoroSettings(1500, 300, 10800);

        PomodoroCycleProgress result =
                PomodoroCycleCalculator.calculate(settings, 4200);

        assertEquals(PomodoroPhase.STUDY, result.phase());
        assertEquals(3, result.cycleNumber());
        assertEquals(900, result.remainingPhaseSeconds());
        assertEquals(3600, result.plannedStudySeconds());
        assertFalse(result.completed());
    }

    @Test
    void shouldSwitchToBreakAfterStudyDuration() {

        PomodoroSettings settings =
                new PomodoroSettings(1500, 300, 10800);

        PomodoroCycleProgress result =
                PomodoroCycleCalculator.calculate(settings, 1500);

        assertEquals(PomodoroPhase.BREAK, result.phase());
        assertEquals(300, result.remainingPhaseSeconds());
    }

    @Test
    void shouldCompleteAfterTargetDuration() {

        PomodoroSettings settings =
                new PomodoroSettings(1500, 300, 10800);

        PomodoroCycleProgress result =
                PomodoroCycleCalculator.calculate(settings, 10800);

        assertTrue(result.completed());
        assertNull(result.phase());
        assertEquals(0, result.remainingPhaseSeconds());
        assertEquals(0, result.remainingSessionSeconds());
        assertEquals(9000, result.plannedStudySeconds());
    }

    @Test
    void shouldRejectInvalidTargetDuration() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new PomodoroSettings(1500, 300, 0)
        );
    }

    @Test
    void shouldRejectNegativeElapsedTime() {

        PomodoroSettings settings =
                new PomodoroSettings(1500, 300, 10800);

        assertThrows(
                IllegalArgumentException.class,
                () -> PomodoroCycleCalculator.calculate(settings, -1)
        );
    }
}
