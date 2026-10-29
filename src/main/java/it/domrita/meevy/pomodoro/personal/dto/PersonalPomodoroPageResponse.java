
package it.domrita.meevy.pomodoro.personal.dto;

import it.domrita.meevy.pomodoro.personal.model.PersonalPomodoroSession;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;


// Rappresenta una pagina dello storico Pomodoro.

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
                sessions.getContent()    //Restituisce la lista delle sessioni presenti nella pagina corrente.
                        .stream()  //Converte la lista in uno Stream  (operazioni come filtrare, trasformare e aggregar)
                        .map(session ->     //map() è un'operazione dello Stream che trasforma ogni elemento in un altro valore e con la lambda Per ogni sessione che ricevi, crea il corrispondente PersonalPomodoroResponse.
                                PersonalPomodoroResponse.from(session, now)
                        )
                        .toList(); //Conclude l'elaborazione dello Stream e raccoglie i risultati in una lista

        return new PersonalPomodoroPageResponse(
                content,
                sessions.getNumber(),
                sessions.getSize(),
                sessions.getTotalElements(),
                sessions.getTotalPages()
        );
    }
}

/*

Pageable: descrive quale pagina vogliamo, quanti elementi deve contenere e l'eventuale ordinamento.
Page<T>: rappresenta il risultato della ricerca, contenente gli elementi della pagina e informazioni come il numero totale di risultati.
 */
