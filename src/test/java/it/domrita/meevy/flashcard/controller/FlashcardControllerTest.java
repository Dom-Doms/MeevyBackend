
package it.domrita.meevy.flashcard.controller;

import it.domrita.meevy.common.exception.GlobalExceptionHandler;
import it.domrita.meevy.flashcard.dto.FlashcardRequest;
import it.domrita.meevy.flashcard.dto.FlashcardResponse;
import it.domrita.meevy.flashcard.dto.FlashcardUpdateRequest;
import it.domrita.meevy.flashcard.exception.FlashcardNotFoundException;
import it.domrita.meevy.flashcard.service.FlashcardService;
import it.domrita.meevy.subject.exception.SubjectNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class FlashcardControllerTest {

    @Mock
    private FlashcardService flashcardService;

    private MockMvc mockMvc;

    private static final Long FLASHCARD_ID = 10L;
    private static final Long SUBJECT_ID = 2L;

    private static final String VALID_REQUEST = """
            {
                "subjectId": 2,
                "question": "cos'è MVC?",
                "answer": "Model View Controller"
            }
            """;

    private static final String UPDATE_REQUEST = """
            {
                "question": "cos'è il polimorfismo?",
                "answer": "è la possibilità di assumere comportamenti differenti"
            }
            """;

    @BeforeEach
    void setUp(){

        //Prepariamo il Controller e la gestione delle eccezioni
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FlashcardController(flashcardService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    //Prepariamo una response da utilizzare nei test
    private FlashcardResponse response(Long id){

        Instant now = Instant.parse("2026-11-12T10:00:00Z");

        return new FlashcardResponse(
                id,
                SUBJECT_ID,
                "cos'è MVC?",
                "Model View Controller",
                now,
                now
        );
    }

    @Test //Creazione corretta della flashcard
    void createFlashcard_Test() throws Exception {

        FlashcardRequest request = new FlashcardRequest(
                SUBJECT_ID,
                "cos'è MVC?",
                "Model View Controller"
        );

        when(flashcardService.createFlashcard(request))
                .thenReturn(response(FLASHCARD_ID));

        mockMvc.perform(post("/flashcards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))

                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.subjectId").value(2))
                .andExpect(jsonPath("$.question").value("cos'è MVC?"))
                .andExpect(jsonPath("$.answer").value("Model View Controller"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(flashcardService).createFlashcard(request);
    }

    @Test //Creazione senza materia
    void createFlashcard_SubjectNull() throws Exception {

        String request = """
                {
                    "question": "cos'è MVC?",
                    "answer": "Model View Controller"
                }
                """;

        mockMvc.perform(post("/flashcards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))

                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.subjectId").exists());

        verifyNoInteractions(flashcardService);
    }

    @Test //Creazione con id della materia non valido
    void createFlashcard_InvalidSubjectId() throws Exception {

        String request = """
                {
                    "subjectId": 0,
                    "question": "cos'è MVC?",
                    "answer": "Model View Controller"
                }
                """;

        mockMvc.perform(post("/flashcards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))

                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.subjectId").exists());

        verifyNoInteractions(flashcardService);
    }

    @Test //Creazione con domanda vuota
    void createFlashcard_EmptyQuestion() throws Exception {

        String request = """
                {
                    "subjectId": 2,
                    "question": "",
                    "answer": "Model View Controller"
                }
                """;

        mockMvc.perform(post("/flashcards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))

                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.question").exists());

        verifyNoInteractions(flashcardService);
    }

    @Test //Creazione con risposta vuota
    void createFlashcard_EmptyAnswer() throws Exception {

        String request = """
                {
                    "subjectId": 2,
                    "question": "cos'è MVC?",
                    "answer": ""
                }
                """;

        mockMvc.perform(post("/flashcards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))

                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.answer").exists());

        verifyNoInteractions(flashcardService);
    }

    @Test //Creazione con JSON non valido
    void createFlashcard_InvalidJson() throws Exception {

        mockMvc.perform(post("/flashcards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subjectId\":"))

                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JSON"));

        verifyNoInteractions(flashcardService);
    }

    @Test //Creazione con materia inesistente
    void createFlashcard_SubjectNotFound() throws Exception {

        FlashcardRequest request = new FlashcardRequest(
                SUBJECT_ID,
                "cos'è MVC?",
                "Model View Controller"
        );

        when(flashcardService.createFlashcard(request))
                .thenThrow(new SubjectNotFoundException(SUBJECT_ID));

        mockMvc.perform(post("/flashcards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))

                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SUBJECT_NOT_FOUND"));

        verify(flashcardService).createFlashcard(request);
    }

    @Test //Recupero flashcard personali
    void getAllFlashcards_Test() throws Exception {

        when(flashcardService.getAllFlashcards())
                .thenReturn(List.of(response(10L), response(11L)));

        mockMvc.perform(get("/flashcards"))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].subjectId").value(2))
                .andExpect(jsonPath("$[0].question").value("cos'è MVC?"))
                .andExpect(jsonPath("$[0].answer").value("Model View Controller"))
                .andExpect(jsonPath("$[1].id").value(11));

        verify(flashcardService).getAllFlashcards();
    }

    @Test //Nessuna flashcard personale
    void getAllFlashcards_EmptyList() throws Exception {

        when(flashcardService.getAllFlashcards())
                .thenReturn(List.of());

        mockMvc.perform(get("/flashcards"))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(flashcardService).getAllFlashcards();
    }

    @Test //Recupero flashcard per materia
    void getFlashcardsBySubjectId_Test() throws Exception {

        when(flashcardService.getFlashcardsBySubjectId(SUBJECT_ID))
                .thenReturn(List.of(response(FLASHCARD_ID)));

        mockMvc.perform(get("/flashcards/subject/{subjectId}", SUBJECT_ID))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].subjectId").value(2))
                .andExpect(jsonPath("$[0].question").value("cos'è MVC?"))
                .andExpect(jsonPath("$[0].answer").value("Model View Controller"));

        verify(flashcardService).getFlashcardsBySubjectId(SUBJECT_ID);
    }

    @Test //Materia senza flashcard
    void getFlashcardsBySubjectId_EmptyList() throws Exception {

        when(flashcardService.getFlashcardsBySubjectId(SUBJECT_ID))
                .thenReturn(List.of());

        mockMvc.perform(get("/flashcards/subject/{subjectId}", SUBJECT_ID))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(flashcardService).getFlashcardsBySubjectId(SUBJECT_ID);
    }

    @Test //Recupero flashcard di una materia inesistente
    void getFlashcardsBySubjectId_SubjectNotFound() throws Exception {

        when(flashcardService.getFlashcardsBySubjectId(SUBJECT_ID))
                .thenThrow(new SubjectNotFoundException(SUBJECT_ID));

        mockMvc.perform(get("/flashcards/subject/{subjectId}", SUBJECT_ID))

                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SUBJECT_NOT_FOUND"));

        verify(flashcardService).getFlashcardsBySubjectId(SUBJECT_ID);
    }

    @Test //Recupero flashcard tramite id
    void getFlashcardById_Test() throws Exception {

        when(flashcardService.getFlashcardById(FLASHCARD_ID))
                .thenReturn(response(FLASHCARD_ID));

        mockMvc.perform(get("/flashcards/{id}", FLASHCARD_ID))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.subjectId").value(2))
                .andExpect(jsonPath("$.question").value("cos'è MVC?"))
                .andExpect(jsonPath("$.answer").value("Model View Controller"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(flashcardService).getFlashcardById(FLASHCARD_ID);
    }

    @Test //Recupero flashcard inesistente
    void getFlashcardById_FlashcardNotFound() throws Exception {

        when(flashcardService.getFlashcardById(FLASHCARD_ID))
                .thenThrow(new FlashcardNotFoundException(FLASHCARD_ID));

        mockMvc.perform(get("/flashcards/{id}", FLASHCARD_ID))

                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FLASHCARD_NOT_FOUND"));

        verify(flashcardService).getFlashcardById(FLASHCARD_ID);
    }

    @Test //Modifica corretta della flashcard
    void updateFlashcard_Test() throws Exception {

        FlashcardUpdateRequest request = new FlashcardUpdateRequest(
                "cos'è il polimorfismo?",
                "è la possibilità di assumere comportamenti differenti"
        );

        Instant createdAt = Instant.parse("2026-11-10T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-11-12T10:00:00Z");

        FlashcardResponse updatedFlashcard = new FlashcardResponse(
                FLASHCARD_ID,
                SUBJECT_ID,
                request.question(),
                request.answer(),
                createdAt,
                updatedAt
        );

        when(flashcardService.updateFlashcard(FLASHCARD_ID, request))
                .thenReturn(updatedFlashcard);

        mockMvc.perform(put("/flashcards/{id}", FLASHCARD_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_REQUEST))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.subjectId").value(2))
                .andExpect(jsonPath("$.question").value("cos'è il polimorfismo?"))
                .andExpect(jsonPath("$.answer").value("è la possibilità di assumere comportamenti differenti"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(flashcardService).updateFlashcard(FLASHCARD_ID, request);
    }

    @Test //Modifica con domanda e risposta non valide
    void updateFlashcard_InvalidRequest() throws Exception {

        String request = """
                {
                    "question": "",
                    "answer": "   "
                }
                """;

        mockMvc.perform(put("/flashcards/{id}", FLASHCARD_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))

                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.question").exists())
                .andExpect(jsonPath("$.fields.answer").exists());

        verifyNoInteractions(flashcardService);
    }

    @Test //Modifica di una flashcard inesistente
    void updateFlashcard_FlashcardNotFound() throws Exception {

        FlashcardUpdateRequest request = new FlashcardUpdateRequest(
                "cos'è il polimorfismo?",
                "è la possibilità di assumere comportamenti differenti"
        );

        when(flashcardService.updateFlashcard(FLASHCARD_ID, request))
                .thenThrow(new FlashcardNotFoundException(FLASHCARD_ID));

        mockMvc.perform(put("/flashcards/{id}", FLASHCARD_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_REQUEST))

                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FLASHCARD_NOT_FOUND"));

        verify(flashcardService).updateFlashcard(FLASHCARD_ID, request);
    }

    @Test //Eliminazione corretta della flashcard
    void deleteFlashcard_Test() throws Exception {

        mockMvc.perform(delete("/flashcards/{id}", FLASHCARD_ID))

                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(flashcardService).deleteFlashcard(FLASHCARD_ID);
    }

    @Test //Eliminazione di una flashcard inesistente
    void deleteFlashcard_FlashcardNotFound() throws Exception {

        doThrow(new FlashcardNotFoundException(FLASHCARD_ID))
                .when(flashcardService).deleteFlashcard(FLASHCARD_ID);

        mockMvc.perform(delete("/flashcards/{id}", FLASHCARD_ID))

                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FLASHCARD_NOT_FOUND"));

        verify(flashcardService).deleteFlashcard(FLASHCARD_ID);
    }

    @Test //Duplicazione corretta della flashcard
    void duplicateFlashcard_Test() throws Exception {

        when(flashcardService.duplicateFlashcard(FLASHCARD_ID))
                .thenReturn(response(11L));

        mockMvc.perform(post("/flashcards/{id}/duplicate", FLASHCARD_ID))

                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.subjectId").value(2))
                .andExpect(jsonPath("$.question").value("cos'è MVC?"))
                .andExpect(jsonPath("$.answer").value("Model View Controller"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(flashcardService).duplicateFlashcard(FLASHCARD_ID);
    }

    @Test //Duplicazione di una flashcard inesistente
    void duplicateFlashcard_FlashcardNotFound() throws Exception {

        when(flashcardService.duplicateFlashcard(FLASHCARD_ID))
                .thenThrow(new FlashcardNotFoundException(FLASHCARD_ID));

        mockMvc.perform(post("/flashcards/{id}/duplicate", FLASHCARD_ID))

                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FLASHCARD_NOT_FOUND"));

        verify(flashcardService).duplicateFlashcard(FLASHCARD_ID);
    }
}
