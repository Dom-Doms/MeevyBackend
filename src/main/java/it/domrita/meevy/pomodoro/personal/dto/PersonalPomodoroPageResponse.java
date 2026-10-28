
package it.domrita.meevy.pomodoro.personal.dto;

import it.domrita.meevy.pomodoro.personal.model.PersonalPomodoroSession;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

/*
 * Rappresenta una pagina dello storico Pomodoro.
 */
public record PersonalPomodoroPageResponse(

        List<PersonalPomodoroResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages

) {

    public static PersonalPomodoroPageResponse from(
            Page<PersonalPomodoroSession> sessions,
            Instant now
    ) {
        List<PersonalPomodoroResponse> content =
                sessions.getContent()
                        .stream()
                        .map(session ->
                                PersonalPomodoroResponse.from(session, now)
                        )
                        .toList();

        return new PersonalPomodoroPageResponse(
                content,
                sessions.getNumber(),
                sessions.getSize(),
                sessions.getTotalElements(),
                sessions.getTotalPages()
        );
    }
}
