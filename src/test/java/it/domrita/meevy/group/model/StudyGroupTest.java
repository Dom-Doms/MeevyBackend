
package it.domrita.meevy.group.model;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class StudyGroupTest {

    @Test //Creazione corretta del gruppo
    void createStudyGroup_Test(){

        StudyGroup group = new StudyGroup(
                "Basi di Dati",
                "Preparazione esame"
        );

        assertNull(group.getId());
        assertEquals("Basi di Dati", group.getName());
        assertEquals("Preparazione esame", group.getDescription());
        assertNull(group.getCreatedAt());
    }


    @Test //Creazione di un gruppo senza descrizione
    void createStudyGroup_WithoutDescription(){

        StudyGroup group = new StudyGroup(
                "Basi di Dati",
                null
        );

        assertEquals("Basi di Dati", group.getName());
        assertNull(group.getDescription());
    }


    @Test //Modifica dei dati del gruppo
    void updateDetails_Test(){

        StudyGroup group = new StudyGroup(
                "Basi di Dati",
                "Preparazione esame"
        );

        group.updateDetails(
                "Ingegneria del Software",
                "Progetto finale"
        );

        assertEquals("Ingegneria del Software", group.getName());
        assertEquals("Progetto finale", group.getDescription());
    }


    @Test //Rimozione della descrizione
    void updateDetails_RemoveDescription(){

        StudyGroup group = new StudyGroup(
                "Basi di Dati",
                "Preparazione esame"
        );

        group.updateDetails("Basi di Dati", null);

        assertEquals("Basi di Dati", group.getName());
        assertNull(group.getDescription());
    }


    @Test //Impostazione della data di creazione
    void onCreate_Test(){

        StudyGroup group = new StudyGroup(
                "Basi di Dati",
                "Preparazione esame"
        );

        Instant before = Instant.now();

        //Simuliamo il callback eseguito da JPA
        ReflectionTestUtils.invokeMethod(group, "onCreate");

        Instant after = Instant.now();

        assertNotNull(group.getCreatedAt());

        assertFalse(group.getCreatedAt().isBefore(before));
        assertFalse(group.getCreatedAt().isAfter(after));
    }


    @Test //La modifica non cambia la data di creazione
    void updateDetails_CreatedAtUnchanged(){

        StudyGroup group = new StudyGroup(
                "Basi di Dati",
                "Preparazione esame"
        );

        Instant createdAt = Instant.parse("2026-11-12T10:00:00Z");

        ReflectionTestUtils.setField(
                group, "createdAt", createdAt
        );

        group.updateDetails(
                "Nuovo nome",
                "Nuova descrizione"
        );

        assertEquals("Nuovo nome", group.getName());
        assertEquals("Nuova descrizione", group.getDescription());
        assertEquals(createdAt, group.getCreatedAt());
    }
}
