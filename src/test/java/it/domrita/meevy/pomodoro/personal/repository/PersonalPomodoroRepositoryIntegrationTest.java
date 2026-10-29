
package it.domrita.meevy.pomodoro.personal.repository;

import it.domrita.meevy.pomodoro.common.PomodoroSettings;
import it.domrita.meevy.pomodoro.personal.model.PersonalPomodoroSession;
import it.domrita.meevy.subject.model.Subject;
import it.domrita.meevy.user.model.User;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Test di integrazione con PostgreSQL reale.
 *
 * Utilizza il database meevy esistente.
 * Ogni test viene eseguito in una transazione
 * annullata automaticamente al termine.
 */
@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class PersonalPomodoroRepositoryIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PersonalPomodoroRepository repository;

    private final PomodoroSettings settings =
            new PomodoroSettings(1500, 300, 10800);

    /*
     * Crea un utente temporaneo con email univoca.
     */
    private User createUser() {
        User user = new User(
                "pomodoro-test-" + UUID.randomUUID() + "@example.invalid",
                "test-password-hash",
                null,
                null
        );

        entityManager.persist(user);
        return user;
    }

    /*
     * Crea una materia temporanea dell'utente.
     */
    private Subject createSubject(User user) {
        Subject subject = new Subject(
                "Materia test Pomodoro",
                user
        );

        entityManager.persist(subject);
        return subject;
    }

    /*
     * 1. Una sessione deve essere recuperabile
     * soltanto dal suo proprietario.
     */
    @Test
    void shouldFindSessionOnlyForOwner() {
        User owner = createUser();
        User otherUser = createUser();
        Subject subject = createSubject(owner);

        PersonalPomodoroSession session =
                new PersonalPomodoroSession(
                        owner,
                        subject,
                        settings
                );

        repository.saveAndFlush(session);

        Long sessionId = session.getId();
        Long ownerId = owner.getId();
        Long otherId = otherUser.getId();

        entityManager.clear();

        assertTrue(
                repository.findByIdAndUser_Id(
                        sessionId, ownerId
                ).isPresent()
        );

        assertTrue(
                repository.findByIdAndUser_Id(
                        sessionId, otherId
                ).isEmpty()
        );
    }

    /*
     * 2. PostgreSQL deve impedire due sessioni
     * READY contemporanee per lo stesso utente.
     */
    @Test
    void shouldRejectTwoActiveSessionsForSameUser() {
        User user = createUser();
        Subject subject = createSubject(user);

        PersonalPomodoroSession first =
                new PersonalPomodoroSession(
                        user, subject, settings
                );

        repository.saveAndFlush(first);

        PersonalPomodoroSession second =
                new PersonalPomodoroSession(
                        user, subject, settings
                );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(second)
        );
    }

    /*
     * 3. Eliminare una materia non deve eliminare
     * lo storico delle sessioni Pomodoro.
     */
    @Test
    void shouldPreserveSessionAfterSubjectDeletion() {
        User user = createUser();
        Subject subject = createSubject(user);

        PersonalPomodoroSession session =
                new PersonalPomodoroSession(
                        user, subject, settings
                );

        repository.saveAndFlush(session);

        Long sessionId = session.getId();
        Long userId = user.getId();


        /*
         * Svuota il contesto di persistenza.
         * La sessione Pomodoro rimane salvata nel database,
         * ma non è più gestita in memoria da Hibernate.
         */
        entityManager.clear();

        /*
         * Ricarica la Subject e la elimina.
         * PostgreSQL applicherà ON DELETE SET NULL
         * alla sessione Pomodoro associata.
         */
        Subject subjectToDelete =
                entityManager.find(Subject.class, subject.getId());

        entityManager.remove(subjectToDelete);
        entityManager.flush();

        /*
         * Svuota nuovamente il contesto per verificare
         * i dati effettivamente presenti nel database.
         */
        entityManager.clear();


        PersonalPomodoroSession reloaded =
                repository.findByIdAndUser_Id(
                        sessionId, userId
                ).orElseThrow();

        assertNull(reloaded.getSubject());
        assertEquals(sessionId, reloaded.getId());
    }
}
