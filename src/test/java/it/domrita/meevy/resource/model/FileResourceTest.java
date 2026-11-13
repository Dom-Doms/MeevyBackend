
package it.domrita.meevy.resource.model;

import it.domrita.meevy.subject.model.Subject;
import it.domrita.meevy.user.model.User;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileResourceTest {

    @Test
    void createResource_salvaCorrettamenteIDati() {

        // Creiamo un utente e una sua materia
        User user = new User(
                "test@example.com",
                "passwordHash",
                "Mario",
                "Rossi"
        );

        Subject subject = new Subject("Analisi 1", user);

        // Creiamo il documento con i suoi metadati
        FileResource resource = new FileResource(
                user,
                user,
                subject,
                "appunti.pdf",
                "123e4567-e89b-12d3-a456-426614174000",
                "application/pdf",
                2048
        );

        // Controlliamo che i dati siano stati salvati correttamente
        //Verifica che siano proprio lo stesso oggetto
        assertSame(user, resource.getOwnerUser());
        assertSame(user, resource.getUploadedByUser());
        assertSame(subject, resource.getSubject());

        assertEquals("appunti.pdf", resource.getOriginalFilename());
        assertEquals("application/pdf", resource.getContentType());
        assertEquals(2048, resource.getSizeBytes());
    }

    @Test
    void createResource_rifiutaDimensioneNonValida() {

        User user = new User(
                "test@example.com",
                "passwordHash",
                "Mario",
                "Rossi"
        );

        // Una dimensione pari a zero non deve essere accettata
        //Verifica che il codice generi l'eccezione prevista
        assertThrows(IllegalArgumentException.class, () ->
                new FileResource(
                        user,
                        user,
                        null,
                        "appunti.pdf",
                        "123e4567-e89b-12d3-a456-426614174000",
                        "application/pdf",
                        0
                )
        );
    }
}
