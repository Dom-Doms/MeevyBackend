
package it.domrita.meevy.flashcard.model;

import it.domrita.meevy.subject.model.Subject;
import it.domrita.meevy.user.model.User;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class FlashcardTest {

    @Test //Creazione corretta dell'oggetto flashcard
    void createFlashcard_Test(){

        User user = mock(User.class);
        Subject subject = mock(Subject.class);

        Flashcard flashcard = new Flashcard(
                user,
                subject,
                "cos'è una classe?",
                "è un modello che definisce le caratteristiche e i comportamenti che un oggetto potrà avere"
        );

        assertSame(user, flashcard.getUser());
        assertSame(subject, flashcard.getSubject());
        assertEquals("cos'è una classe?", flashcard.getQuestion());
        assertEquals("è un modello che definisce le caratteristiche e i comportamenti che un oggetto potrà avere", flashcard.getAnswer());

        //La flashcard non è ancora stata salvata
        assertNull(flashcard.getId());
        assertNull(flashcard.getCreatedAt());
        assertNull(flashcard.getUpdatedAt());
    }

    @Test //Modifica del contenuto della flashcard
    void updateContent_Test(){

        User user = mock(User.class);
        Subject subject = mock(Subject.class);

        Flashcard flashcard = new Flashcard(
                user,
                subject,
                "cos'è MVC?",
                "Model View Controller"
        );

        ReflectionTestUtils.setField(flashcard, "id", 10L);

        flashcard.updateContent(
                "cos'è il polimorfismo?",
                "è la possibilità di assumere comportamenti differenti"
        );

        assertEquals("cos'è il polimorfismo?", flashcard.getQuestion());
        assertEquals("è la possibilità di assumere comportamenti differenti", flashcard.getAnswer());

        //Verifichiamo che gli altri attributi non vengano modificati
        assertEquals(10L, flashcard.getId());
        assertSame(user, flashcard.getUser());
        assertSame(subject, flashcard.getSubject());
    }

    @Test //Impostazione delle date alla creazione
    void onCreate_Test(){

        User user = mock(User.class);
        Subject subject = mock(Subject.class);

        Flashcard flashcard = new Flashcard(
                user,
                subject,
                "cos'è JWT?",
                "è uno standard per trasmettere informazioni in modo sicuro"
        );

        //Simuliamo il callback eseguito da JPA prima del salvataggio
        ReflectionTestUtils.invokeMethod(flashcard, "onCreate");

        assertNotNull(flashcard.getCreatedAt());
        assertNotNull(flashcard.getUpdatedAt());

        //Alla creazione le due date devono coincidere
        assertEquals(flashcard.getCreatedAt(), flashcard.getUpdatedAt());
    }

    @Test //Aggiornamento della data di modifica
    void onUpdate_Test(){

        User user = mock(User.class);
        Subject subject = mock(Subject.class);

        Flashcard flashcard = new Flashcard(
                user,
                subject,
                "cos'è MVC?",
                "Model View Controller"
        );

        Instant createdAt = Instant.parse("2026-11-10T10:00:00Z");

        ReflectionTestUtils.setField(flashcard, "createdAt", createdAt);
        ReflectionTestUtils.setField(flashcard, "updatedAt", createdAt);

        flashcard.updateContent(
                "Nuova domanda",
                "Nuova risposta"
        );

        //Simuliamo il callback eseguito da JPA prima dell'aggiornamento
        ReflectionTestUtils.invokeMethod(flashcard, "onUpdate");

        assertNotNull(flashcard.getUpdatedAt());
        assertTrue(flashcard.getUpdatedAt().isAfter(createdAt));

        //La data di creazione deve rimanere invariata
        assertEquals(createdAt, flashcard.getCreatedAt());

        assertEquals("Nuova domanda", flashcard.getQuestion());
        assertEquals("Nuova risposta", flashcard.getAnswer());
    }

}
