
package it.domrita.meevy.pomodoro.personal.repository;

import it.domrita.meevy.pomodoro.personal.model.PersonalPomodoroSession;
import it.domrita.meevy.pomodoro.personal.role.PersonalPomodoroStatus;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Collection;
import java.util.Optional;

/*
 * Gestisce l'accesso alle sessioni Pomodoro personali.
 *
 * Le query sono limitate all'utente proprietario,
 * per evitare l'accesso alle sessioni di altri utenti.
 */
public interface PersonalPomodoroRepository
        extends JpaRepository<PersonalPomodoroSession, Long> {

    /*
     * Recupera una sessione tramite ID,
     * verificando che appartenga all'utente.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PersonalPomodoroSession> findByIdAndUser_Id(
            Long id,
            Long userId
    );
    /*
     * Recupera la sessione non ancora terminata:
     * READY, RUNNING oppure PAUSED.
     *
     * Cosi ogni utente avrà al massimo una sessione in corso.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PersonalPomodoroSession> findByUser_IdAndStatusIn(
            Long userId,
            Collection<PersonalPomodoroStatus> statuses
    );

    /*
     * Recupera le sessioni dell'utente,
     * ordinate dalla più recente alla più vecchia.
     *
     * Pageable permette di gestire la paginazione.
     */
    Page<PersonalPomodoroSession> findByUser_IdOrderByCreatedAtDesc(
            Long userId,
            Pageable pageable
    );
}
