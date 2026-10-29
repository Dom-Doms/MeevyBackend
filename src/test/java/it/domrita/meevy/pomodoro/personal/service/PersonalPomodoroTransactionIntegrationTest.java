
package it.domrita.meevy.pomodoro.personal.service;

import it.domrita.meevy.auth.repository.CurrentUserIdProvider;
import it.domrita.meevy.pomodoro.common.PomodoroSettings;
import it.domrita.meevy.pomodoro.personal.exception.PersonalPomodoroInvalidStateException;
import it.domrita.meevy.pomodoro.personal.model.PersonalPomodoroSession;
import it.domrita.meevy.pomodoro.personal.repository.PersonalPomodoroRepository;
import it.domrita.meevy.pomodoro.personal.role.PersonalPomodoroStatus;
import it.domrita.meevy.subject.model.Subject;
import it.domrita.meevy.subject.repository.SubjectRepository;
import it.domrita.meevy.user.model.User;
import it.domrita.meevy.user.repository.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/*
 * Verifica il comportamento transazionale reale
 * del Service utilizzando PostgreSQL.
 *
 * Il test non ha una transazione esterna:
 * in questo modo il Service può eseguire
 * e concludere la propria transazione.
 */
@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@Import(PersonalPomodoroService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PersonalPomodoroTransactionIntegrationTest {

    @Autowired
    private PersonalPomodoroService service;

    @Autowired
    private PersonalPomodoroRepository pomodoroRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private CurrentUserIdProvider currentUserIdProvider;

    private Long userId;
    private Long subjectId;
    private Long sessionId;

    /*
     * Verifica che il completamento automatico
     * venga salvato anche quando il Service
     * rifiuta l'operazione richiesta.
     */
    @Test
    void shouldPersistCompletionDespiteInvalidStateException() {

        // Crea un utente temporaneo.
        User user = userRepository.saveAndFlush(
                new User(
                        "pomodoro-transaction-"
                                + UUID.randomUUID()
                                + "@example.invalid",
                        "test-hash",
                        null,
                        null
                )
        );

        userId = user.getId();

        // Crea una materia temporanea.
        Subject subject = subjectRepository.saveAndFlush(
                new Subject("Materia test transazione", user)
        );

        subjectId = subject.getId();

        // Crea una sessione già scaduta.
        PersonalPomodoroSession session =
                new PersonalPomodoroSession(
                        user,
                        subject,
                        new PomodoroSettings(20, 10, 60)
                );

        session.start(
                Instant.now()
                        .minusSeconds(120)
                        .truncatedTo(ChronoUnit.SECONDS)
        );

        pomodoroRepository.saveAndFlush(session);
        sessionId = session.getId();

        // Simula l'utente autenticato.
        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(userId);

        // Il Service deve rifiutare la pausa.
        assertThrows(
                PersonalPomodoroInvalidStateException.class,
                () -> service.pause(sessionId)
        );

        // Rilegge la sessione dal database.
        PersonalPomodoroSession saved =
                pomodoroRepository.findById(sessionId).orElseThrow();

        // La sessione deve essere stata completata.
        assertEquals(
                PersonalPomodoroStatus.COMPLETED,
                saved.getStatus()
        );

        // Due cicli da 20 secondi di studio.
        assertEquals(40, saved.getActualStudySeconds());

        // La fine coincide con la scadenza programmata.
        assertEquals(
                session.getStartedAt().plusSeconds(60),
                saved.getEndedAt()
        );
    }

    /*
     * Elimina esclusivamente i record temporanei
     * creati da questo test.
     *
     * Non modifica le sessioni degli altri utenti.
     */
    @AfterEach
    void cleanup() {

        if (sessionId != null) {
            jdbcTemplate.update(
                    "DELETE FROM personal_pomodoro_session WHERE id = ?",
                    sessionId
            );
        }

        if (subjectId != null) {
            jdbcTemplate.update(
                    "DELETE FROM subject WHERE id = ?",
                    subjectId
            );
        }

        if (userId != null) {
            jdbcTemplate.update(
                    "DELETE FROM \"user\" WHERE id = ?",
                    userId
            );
        }
    }
}
