
package it.domrita.meevy.pomodoro.personal.controller;

import it.domrita.meevy.pomodoro.personal.dto.*;
import it.domrita.meevy.pomodoro.personal.model.PersonalPomodoroSession;
import it.domrita.meevy.pomodoro.personal.service.PersonalPomodoroService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;


  //Espone le API REST per gestire  le sessioni Pomodoro personali.


@RestController
@RequestMapping("/pomodoro-personal-sessions")
@RequiredArgsConstructor
public class PersonalPomodoroController {

    private final PersonalPomodoroService pomodoroService;

    /*
     * Crea una sessione in stato READY.
     *
     * POST /pomodoro-personal-sessions
     */
    @PostMapping
    public ResponseEntity<PersonalPomodoroResponse> create(
            @Valid @RequestBody PersonalPomodoroRequest request
    ) {
        PersonalPomodoroSession session =
                pomodoroService.create(
                        request.subjectId(),
                        request.toSettings()
                );


        // utile ma non necessario, UTILIZZO: per ritornare nella risposta direttamente l'endpoint per recuperare la sessione
        URI location = URI.create(
                "/pomodoro-personal-sessions/" + session.getId()
        );

        return ResponseEntity
                .created(location)
                .body(PersonalPomodoroResponse.from(
                        session,
                        Instant.now()
                ));
    }

    /*
     * Modifica la configurazione di una sessione READY.
     * La request deve contenere tutti i campi.
     *
     * PATCH /pomodoro-personal-sessions/{id}/update
     */
    @PatchMapping("/{id}/update")
    public ResponseEntity<PersonalPomodoroResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PersonalPomodoroRequest request
    ) {
        PersonalPomodoroSession session =
                pomodoroService.updateReady(
                        id,
                        request.subjectId(),
                        request.toSettings()
                );

        return ResponseEntity.ok(
                PersonalPomodoroResponse.from(session, Instant.now())
        );
    }

    /*
     * Avvia una sessione READY.
     *
     * POST /pomodoro-personal-sessions/{id}/start
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<PersonalPomodoroResponse> start(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                PersonalPomodoroResponse.from(
                        pomodoroService.start(id),
                        Instant.now()
                )
        );
    }

    /*
     * Sospende manualmente la sessione.
     *
     * POST /pomodoro-personal-sessions/{id}/pause
     */
    @PostMapping("/{id}/pause")
    public ResponseEntity<PersonalPomodoroResponse> pause(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                PersonalPomodoroResponse.from(
                        pomodoroService.pause(id),
                        Instant.now()
                )
        );
    }

    /*
     * Riprende la sessione sospesa.
     *
     * POST /pomodoro-personal-sessions/{id}/resume
     */
    @PostMapping("/{id}/resume")
    public ResponseEntity<PersonalPomodoroResponse> resume(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                PersonalPomodoroResponse.from(
                        pomodoroService.resume(id),
                        Instant.now()
                )
        );
    }

    /*
     * Completa la sessione, anche anticipatamente.
     *
     * POST /pomodoro-personal-sessions/{id}/complete
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<PersonalPomodoroResponse> complete(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                PersonalPomodoroResponse.from(
                        pomodoroService.complete(id),
                        Instant.now()
                )
        );
    }

    /*
     * Annulla la sessione senza contabilizzare studio.
     *
     * POST /pomodoro-personal-sessions/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<PersonalPomodoroResponse> cancel(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                PersonalPomodoroResponse.from(
                        pomodoroService.cancel(id),
                        Instant.now()
                )
        );
    }

    /*
     * Recupera la sessione in corso dell'utente autenticato.
     *
     * GET /pomodoro-personal-sessions/current
     *
     * Restituisce 204 se non è presente.
     */
    @GetMapping("/current")
    public ResponseEntity<PersonalPomodoroResponse> getCurrent() {

        Optional<PersonalPomodoroSession> current =
                pomodoroService.getCurrent();

        if (current.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(
                PersonalPomodoroResponse.from(
                        current.get(),
                        Instant.now()
                )
        );
    }

    /*
     * Recupera una sessione tramite ID.
     *
     * GET /pomodoro-personal-sessions/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<PersonalPomodoroResponse> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                PersonalPomodoroResponse.from(
                        pomodoroService.getById(id),
                        Instant.now()
                )
        );
    }

    /*
     * Recupera lo storico delle sessioni dell'utente (TODO per aggiungere le statistiche ).
     *
     * GET /pomodoro-personal-sessions?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<PersonalPomodoroPageResponse> listSessions(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(
                PersonalPomodoroPageResponse.from(
                        pomodoroService.listSessions(pageable),
                        Instant.now()
                )
        );
    }
}
