
package it.domrita.meevy.pomodoro.personal.controller;

import it.domrita.meevy.common.exception.GlobalExceptionHandler;
import it.domrita.meevy.pomodoro.common.PomodoroSettings;
import it.domrita.meevy.pomodoro.personal.exception.*;
import it.domrita.meevy.pomodoro.personal.model.PersonalPomodoroSession;
import it.domrita.meevy.pomodoro.personal.service.PersonalPomodoroService;
import it.domrita.meevy.subject.exception.SubjectNotFoundException;
import it.domrita.meevy.subject.model.Subject;
import it.domrita.meevy.user.model.User;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/*
 * Test delle API REST del Pomodoro personale.
 *
 * Verifica mapping, validazione, risposte JSON
 * e gestione degli errori HTTP.
 *
 * Il Service viene simulato con Mockito.
 */
@WebMvcTest(PersonalPomodoroController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PersonalPomodoroControllerTest {

    private static final String BASE =
            "/pomodoro-personal-sessions";

    private static final Long SESSION_ID = 3L;
    private static final Long SUBJECT_ID = 2L;

    private static final String VALID_REQUEST = """
            {
                "subjectId": 2,
                "studyDurationSeconds": 1500,
                "breakDurationSeconds": 300,
                "targetDurationSeconds": 10800
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PersonalPomodoroService service;

    /*
     * Crea una vera Entity READY da utilizzare
     * nelle risposte simulate del Service.
     */
    private PersonalPomodoroSession newSession() {
        Subject subject = mock(Subject.class);
        when(subject.getId()).thenReturn(SUBJECT_ID);

        PersonalPomodoroSession session =
                new PersonalPomodoroSession(
                        mock(User.class),
                        subject,
                        new PomodoroSettings(1500, 300, 10800)
                );

        // Simula i valori che normalmente genera JPA.
        ReflectionTestUtils.setField(
                session, "id", SESSION_ID
        );

        ReflectionTestUtils.setField(
                session,
                "createdAt",
                Instant.parse("2026-10-28T10:00:00Z")
        );

        return session;
    }

    /*
     * 1. Creazione corretta: HTTP 201.
     */
    @Test
    void shouldCreateSession() throws Exception {
        PersonalPomodoroSession session = newSession();

        when(service.create(
                eq(SUBJECT_ID),
                any(PomodoroSettings.class)
        )).thenReturn(session);

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))

                .andExpect(status().isCreated())

                .andExpect(header().string(
                        "Location", BASE + "/" + SESSION_ID
                ))

                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.subjectId").value(2))
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.phase").isEmpty())
                .andExpect(jsonPath("$.elapsedSeconds").value(0))
                .andExpect(jsonPath("$.remainingSessionSeconds")
                        .value(10800));

        verify(service).create(
                eq(SUBJECT_ID),
                any(PomodoroSettings.class)
        );
    }

    /*
     * 2. Dati non validi: HTTP 400.
     */
    @Test
    void shouldRejectInvalidRequest() throws Exception {
        String invalidRequest = """
                {
                    "subjectId": 0,
                    "studyDurationSeconds": 0,
                    "breakDurationSeconds": 300,
                    "targetDurationSeconds": 10800
                }
                """;

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))

                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.subjectId").exists())
                .andExpect(jsonPath("$.fields.studyDurationSeconds")
                        .exists());

        verifyNoInteractions(service);
    }

    /*
     * 3. JSON malformato: HTTP 400.
     */
    @Test
    void shouldRejectMalformedJson() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subjectId\":"))

                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_JSON"));

        verifyNoInteractions(service);
    }

    /*
     * 4. Modifica configurazione: HTTP 200.
     */
    @Test
    void shouldUpdateReadySession() throws Exception {
        PersonalPomodoroSession session = newSession();

        when(service.updateReady(
                eq(SESSION_ID),
                eq(SUBJECT_ID),
                any(PomodoroSettings.class)
        )).thenReturn(session);

        mockMvc.perform(patch(BASE + "/{id}", SESSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.targetDurationSeconds")
                        .value(10800));
    }

    /*
     * 5. Endpoint di avvio, pausa, ripresa,
     * completamento e annullamento.
     */
    @Test
    void shouldExposeSessionActions() throws Exception {

        Instant now = Instant.now();

        PersonalPomodoroSession running = newSession();
        running.start(now.minusSeconds(30));

        PersonalPomodoroSession paused = newSession();
        paused.start(now.minusSeconds(30));
        paused.pause(now.minusSeconds(10));

        PersonalPomodoroSession resumed = newSession();
        resumed.start(now.minusSeconds(30));
        resumed.pause(now.minusSeconds(10));
        resumed.resume(now.minusSeconds(5));

        PersonalPomodoroSession completed = newSession();
        completed.start(now.minusSeconds(30));
        completed.complete(now.minusSeconds(1));

        PersonalPomodoroSession cancelled = newSession();
        cancelled.cancel(now);

        when(service.start(SESSION_ID)).thenReturn(running);
        when(service.pause(SESSION_ID)).thenReturn(paused);
        when(service.resume(SESSION_ID)).thenReturn(resumed);
        when(service.complete(SESSION_ID)).thenReturn(completed);
        when(service.cancel(SESSION_ID)).thenReturn(cancelled);

        mockMvc.perform(post(BASE + "/{id}/start", SESSION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.phase").value("STUDY"));

        mockMvc.perform(post(BASE + "/{id}/pause", SESSION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"));

        mockMvc.perform(post(BASE + "/{id}/resume", SESSION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"));

        mockMvc.perform(post(BASE + "/{id}/complete", SESSION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.remainingSessionSeconds")
                        .value(0));

        mockMvc.perform(post(BASE + "/{id}/cancel", SESSION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.actualStudySeconds")
                        .value(0));
    }


    @Test
    void shouldReturnCurrentSession() throws Exception {

        // Prima crea la sessione, completando i mock interni.
        PersonalPomodoroSession session = newSession();

        // Solo dopo configura il mock del Service.
        when(service.getCurrent())
                .thenReturn(Optional.of(session));

        mockMvc.perform(get(BASE + "/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.status").value("READY"));
    }


    /*
     * 7. Nessuna sessione corrente: HTTP 204.
     */
    @Test
    void shouldReturnNoContentWhenNoCurrentSession()
            throws Exception {

        when(service.getCurrent()).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE + "/current"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    /*
     * 8. Recupero tramite ID: HTTP 200.
     */
    @Test
    void shouldGetSessionById() throws Exception {
        PersonalPomodoroSession session = newSession();

        when(service.getById(SESSION_ID))
                .thenReturn(session);

        mockMvc.perform(get(BASE + "/{id}", SESSION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.subjectId").value(2))
                .andExpect(jsonPath("$.status").value("READY"));
    }

    /*
     * 9. Recupero dello storico paginato.
     */
    @Test
    void shouldReturnPaginatedHistory() throws Exception {

        var pageable = PageRequest.of(0, 20);

        var page = new PageImpl<>(
                List.of(newSession()),
                pageable,
                1
        );

        when(service.listSessions(any()))
                .thenReturn(page);

        mockMvc.perform(get(BASE)
                        .param("page", "0")
                        .param("size", "20"))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content[0].id").value(3));
    }

    /*
     * 10. Sessione inesistente o di un altro utente:
     * HTTP 404.
     */
    @Test
    void shouldReturnNotFoundForUnknownSession()
            throws Exception {

        when(service.getById(SESSION_ID))
                .thenThrow(
                        new PersonalPomodoroNotFoundException(SESSION_ID)
                );

        mockMvc.perform(get(BASE + "/{id}", SESSION_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("POMODORO_NOT_FOUND"));
    }

    /*
     * 11. Subject inesistente o non appartenente
     * all'utente: HTTP 404.
     */
    @Test
    void shouldReturnNotFoundForUnknownSubject()
            throws Exception {

        when(service.create(
                eq(SUBJECT_ID),
                any(PomodoroSettings.class)
        )).thenThrow(
                new SubjectNotFoundException(SUBJECT_ID)
        );

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))

                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("SUBJECT_NOT_FOUND"));
    }

    /*
     * 12. Sessione non terminata già esistente:
     * HTTP 409.
     */
    @Test
    void shouldReturnConflictForAlreadyActiveSession()
            throws Exception {

        when(service.create(
                eq(SUBJECT_ID),
                any(PomodoroSettings.class)
        )).thenThrow(
                new PersonalPomodoroAlreadyActiveException()
        );

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))

                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("POMODORO_ALREADY_ACTIVE"));
    }

    /*
     * 13. Transizione di stato non consentita:
     * HTTP 409.
     */
    @Test
    void shouldReturnConflictForInvalidState()
            throws Exception {

        when(service.pause(SESSION_ID))
                .thenThrow(
                        new PersonalPomodoroInvalidStateException(
                                "La sessione non può essere sospesa."
                        )
                );

        mockMvc.perform(post(BASE + "/{id}/pause", SESSION_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("POMODORO_INVALID_STATE"));
    }
}
