
package it.domrita.meevy.resource.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LocalFileStorageServiceTest {
    //	JUnit crea una cartella temporanea, che pulisce al termine del test.
    @TempDir
    Path tempDir;

    private LocalFileStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new LocalFileStorageService(
                tempDir.toString()
        );
    }

    @Test
    void save_creaFileConUUID() throws Exception {

        byte[] contenuto = "Appunti di Analisi 1"
                .getBytes(StandardCharsets.UTF_8);

        String key = storageService.save(
                new ByteArrayInputStream(contenuto)
        );

        Path fileSalvato = tempDir.resolve(key);

        assertDoesNotThrow(() -> UUID.fromString(key));
        assertTrue(Files.exists(fileSalvato));
        assertArrayEquals( //Confronta i byte originali con quelli letti dal disco.
                contenuto,
                Files.readAllBytes(fileSalvato)
        );
    }

    @Test
    void load_restituisceIlContenutoSalvato() throws Exception {

        byte[] contenuto = "Documento di prova"
                .getBytes(StandardCharsets.UTF_8);

        String key = storageService.save(
                new ByteArrayInputStream(contenuto) //	Simula il contenuto di un file partendo da dati in memoria.
        );

        var resource = storageService.load(key);

        assertTrue(resource.exists());

        try (var input = resource.getInputStream()) {
            assertArrayEquals(
                    contenuto,
                    input.readAllBytes()
            );
        }
    }

    @Test
    void delete_eliminaIlFileFisico() {

        String key = storageService.save(
                new ByteArrayInputStream("Test".getBytes())
        );

        Path fileSalvato = tempDir.resolve(key);

        //Controlla che il file esista fisicamente.
        assertTrue(Files.exists(fileSalvato));

        storageService.delete(key);

        assertFalse(Files.exists(fileSalvato));
    }

    @Test
    void load_rifiutaChiaveNonValida() {

        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.load("../documento.txt")
        );
    }

    @Test
    void load_rifiutaFileInesistente() {

        String key = UUID.randomUUID().toString();

        assertThrows(
                IllegalStateException.class,
                () -> storageService.load(key)
        );
    }
}
