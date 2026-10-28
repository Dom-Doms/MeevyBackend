
package it.domrita.meevy.pomodoro.personal.service;

import it.domrita.meevy.pomodoro.common.PomodoroSettings;
import it.domrita.meevy.pomodoro.personal.exception.*;
import it.domrita.meevy.pomodoro.personal.model.PersonalPomodoroSession;
import it.domrita.meevy.pomodoro.personal.repository.PersonalPomodoroRepository;
import it.domrita.meevy.pomodoro.personal.role.PersonalPomodoroStatus;

import it.domrita.meevy.subject.exception.SubjectNotFoundException;
import it.domrita.meevy.subject.model.Subject;
import it.domrita.meevy.subject.repository.SubjectRepository;

import it.domrita.meevy.auth.repository.CurrentUserIdProvider;
import it.domrita.meevy.user.model.User;
import it.domrita.meevy.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;

/*
 * Gestisce le sessioni Pomodoro personali.
 *
 * Verifica la proprietà delle risorse, coordina
 * le operazioni sul dominio e la persistenza.
 *
 * Le transizioni di stato restano responsabilità
 * di PersonalPomodoroSession.
 */
@Service
@RequiredArgsConstructor
@Transactional(
        noRollbackFor = PersonalPomodoroInvalidStateException.class
)
public class PersonalPomodoroService {

    private static final EnumSet<PersonalPomodoroStatus> ACTIVE_STATUSES =
            EnumSet.of(
                    PersonalPomodoroStatus.READY,
                    PersonalPomodoroStatus.RUNNING,
                    PersonalPomodoroStatus.PAUSED
            );

    private final PersonalPomodoroRepository pomodoroRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final CurrentUserIdProvider currentUserIdProvider;

    /*
     * Crea una nuova sessione personale in stato READY.
     *
     * L'utente viene identificato tramite autenticazione.
     * La materia deve appartenere allo stesso utente.
     */
    public PersonalPomodoroSession create(
            Long subjectId,
            PomodoroSettings settings
    ) {
        Long userId = currentUserIdProvider.getCurrentUserId();
        Instant now = Instant.now();

        // Verifica se esiste già una sessione non terminata.
        Optional<PersonalPomodoroSession> existing =
                pomodoroRepository.findByUser_IdAndStatusIn(
                        userId,
                        ACTIVE_STATUSES
                );

        if (existing.isPresent()) {

            // Riconcilia un'eventuale sessione già scaduta.
            if (existing.get().completeIfExpired(now)) {

                // Salva il completamento prima di inserire
                // una nuova sessione, per rispettare
                // l'indice univoco parziale PostgreSQL.
                pomodoroRepository.flush();

            } else {
                throw new PersonalPomodoroAlreadyActiveException();
            }
        }

        Subject subject = requireOwnedSubject(subjectId, userId);

        // Recupera il riferimento all'utente autenticato.
        User user = userRepository.getReferenceById(userId);

        PersonalPomodoroSession session =
                new PersonalPomodoroSession(
                        user,
                        subject,
                        settings
                );

        return pomodoroRepository.saveAndFlush(session);
    }

    /*
     * Modifica materia e durate di una sessione READY.
     */
    public PersonalPomodoroSession updateReady(
            Long sessionId,
            Long subjectId,
            PomodoroSettings settings
    ) {
        Long userId = currentUserIdProvider.getCurrentUserId();

        PersonalPomodoroSession session =
                requireOwnedSession(sessionId, userId);

        Subject subject = requireOwnedSubject(subjectId, userId);

        applyTransition(() ->
                session.updateConfiguration(subject, settings)
        );

        return session;
    }

    /*
     * Avvia una sessione READY.
     */
    public PersonalPomodoroSession start(Long sessionId) {
        PersonalPomodoroSession session = requireOwnedSession(
                sessionId,
                currentUserIdProvider.getCurrentUserId()
        );

        Instant now = Instant.now();

        applyTransition(() -> session.start(now));

        return session;
    }

    /*
     * Sospende manualmente una sessione RUNNING.
     */
    public PersonalPomodoroSession pause(Long sessionId) {
        PersonalPomodoroSession session = requireOwnedSession(
                sessionId,
                currentUserIdProvider.getCurrentUserId()
        );

        Instant now = Instant.now();

        // Una sessione scaduta non può essere sospesa.
        requireNotExpired(session, now);

        applyTransition(() -> session.pause(now));

        return session;
    }

    /*
     * Riprende una sessione PAUSED.
     */
    public PersonalPomodoroSession resume(Long sessionId) {
        PersonalPomodoroSession session = requireOwnedSession(
                sessionId,
                currentUserIdProvider.getCurrentUserId()
        );

        Instant now = Instant.now();

        applyTransition(() -> session.resume(now));

        return session;
    }

    /*
     * Completa una sessione.
     *
     * Se la durata totale è già stata raggiunta,
     * usa la scadenza effettiva.
     *
     * Altrimenti permette il completamento anticipato.
     */
    public PersonalPomodoroSession complete(Long sessionId) {
        PersonalPomodoroSession session = requireOwnedSession(
                sessionId,
                currentUserIdProvider.getCurrentUserId()
        );

        Instant now = Instant.now();

        if (!session.completeIfExpired(now)) {
            applyTransition(() -> session.complete(now));
        }

        return session;
    }

    /*
     * Annulla una sessione senza contabilizzare studio.
     */
    public PersonalPomodoroSession cancel(Long sessionId) {
        PersonalPomodoroSession session = requireOwnedSession(
                sessionId,
                currentUserIdProvider.getCurrentUserId()
        );

        Instant now = Instant.now();

        // Evita di annullare una sessione già scaduta.
        requireNotExpired(session, now);

        applyTransition(() -> session.cancel(now));

        return session;
    }

    /*
     * Recupera una specifica sessione dell'utente.
     *
     * Se è scaduta, aggiorna lo stato a COMPLETED.
     */
    public PersonalPomodoroSession getById(Long sessionId) {
        PersonalPomodoroSession session = requireOwnedSession(
                sessionId,
                currentUserIdProvider.getCurrentUserId()
        );

        session.completeIfExpired(Instant.now());

        return session;
    }

    /*
     * Recupera l'eventuale sessione non terminata.
     *
     * Se quella trovata è già scaduta, la completa
     * e restituisce Optional.empty().
     */
    public Optional<PersonalPomodoroSession> getCurrent() {
        Long userId = currentUserIdProvider.getCurrentUserId();

        Optional<PersonalPomodoroSession> current =
                pomodoroRepository.findByUser_IdAndStatusIn(
                        userId,
                        ACTIVE_STATUSES
                );

        if (current.isEmpty()) {
            return Optional.empty();
        }

        PersonalPomodoroSession session = current.get();

        if (session.completeIfExpired(Instant.now())) {
            return Optional.empty();
        }

        return Optional.of(session);
    }


    /*
     * Recupera lo storico paginato.
     *
     * Riconcilia le eventuali sessioni scadute
     * prima di restituire i risultati.
     */
    @Transactional
    public Page<PersonalPomodoroSession> listSessions(
            Pageable pageable
    ) {
        Page<PersonalPomodoroSession> sessions =
                pomodoroRepository.findByUser_IdOrderByCreatedAtDesc(
                        currentUserIdProvider.getCurrentUserId(),
                        pageable
                );

        Instant now = Instant.now();

        sessions.forEach(session ->
                session.completeIfExpired(now)
        );

        return sessions;
    }


    /*
     * Recupera una sessione solo se appartiene
     * all'utente autenticato.
     */
    private PersonalPomodoroSession requireOwnedSession(
            Long sessionId,
            Long userId
    ) {
        return pomodoroRepository.findByIdAndUser_Id(
                sessionId,
                userId
        ).orElseThrow(() ->
                new PersonalPomodoroNotFoundException(sessionId)
        );
    }

    /*
     * Recupera una Subject soltanto se appartiene
     * all'utente autenticato.
     */
    private Subject requireOwnedSubject(
            Long subjectId,
            Long userId
    ) {
        return subjectRepository.findByIdAndUser_Id(
                subjectId,
                userId
        ).orElseThrow(() ->
                new SubjectNotFoundException(subjectId)
        );
    }

    /*
     * Completa una sessione scaduta e impedisce
     * che venga modificata come se fosse attiva.
     */
    private void requireNotExpired(
            PersonalPomodoroSession session,
            Instant now
    ) {
        if (session.completeIfExpired(now)) {
            throw new PersonalPomodoroInvalidStateException(
                    "La sessione è già terminata."
            );
        }
    }

    /*
     * Traduce gli errori delle transizioni del dominio
     * in eccezioni utilizzabili dalle API REST.
     */
    private void applyTransition(Runnable transition) {
        try {
            transition.run();
        } catch (IllegalStateException ex) {
            throw new PersonalPomodoroInvalidStateException(
                    ex.getMessage()
            );
        }
    }
}
