
package it.domrita.meevy.resource.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import java.util.Objects;
import java.util.UUID;

@Service
public class LocalFileStorageService {

    private final Path storageRoot;

    /*
     * Prendiamo da application.properties il percorso della cartella
     * in cui salvare i documenti. Se non esiste, la creiamo.
     */
    public LocalFileStorageService(
            @Value("${meevy.storage.root}") String storagePath
    ) {
        this.storageRoot = Path.of(storagePath)
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(storageRoot);
        } catch (IOException ex) {
            throw new UncheckedIOException(
                    "Impossibile inizializzare la cartella dei file.",
                    ex
            );
        }
    }

    /*
     * Salva un documento sul disco.
     * Riceviamo i dati come InputStream, quindi possiamo copiarli
     * senza dover caricare tutto il file in memoria.
     *
     * Restituiamo il nome generato, che poi salveremo nel database.
     */
    public String save(InputStream inputStream) {

        Objects.requireNonNull(inputStream);

        // UUID per evitare file con nomi uguali
        String storageKey = UUID.randomUUID().toString();
        Path destination = resolve(storageKey);

        boolean created = false;

        try (
                InputStream input = inputStream;
                OutputStream output = Files.newOutputStream(
                        destination,
                        StandardOpenOption.CREATE_NEW,
                        StandardOpenOption.WRITE
                )
        ) {
            created = true;

            // Copia il contenuto del file nella cartella scelta
            input.transferTo(output);

            return storageKey;

        } catch (IOException ex) {

            /*
             * Se qualcosa va storto mentre stiamo scrivendo,
             * proviamo a cancellare il file incompleto.
             * Se anche la cancellazione fallisce, conserviamo
             * entrambe le informazioni nell'eccezione.
             */
            if (created) {
                try {
                    Files.deleteIfExists(destination);
                } catch (IOException cleanupEx) {
                    ex.addSuppressed(cleanupEx);
                }
            }

            throw new UncheckedIOException(
                    "Impossibile salvare il file.", ex
            );
        }
    }

    // Recupera il file dal disco, servirà quando faremo il download
    public Resource load(String storageKey) {

        Path path = resolve(storageKey);

        // Controlliamo che esista e che si possa leggere
        if (!Files.isRegularFile(path)
                || !Files.isReadable(path)) {
            throw new IllegalStateException(
                    "File fisico non disponibile."
            );
        }

        return new FileSystemResource(path);
    }

    // Cancella il documento fisico, non il record nel database
    public void delete(String storageKey) {

        Path path = resolve(storageKey);

        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            throw new UncheckedIOException(
                    "Impossibile eliminare il file.", ex
            );
        }
    }

    /*
     * Partendo dalla storageKey troviamo il percorso del documento.
     * Accettiamo solo nomi nel formato UUID, così non si possono
     * passare percorsi arbitrari per leggere altri file del server.
     */
    private Path resolve(String storageKey) {

        if (storageKey == null || !storageKey.matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
        )) {
            throw new IllegalArgumentException(
                    "Storage key non valida."
            );
        }

        return storageRoot.resolve(storageKey);
    }
}
