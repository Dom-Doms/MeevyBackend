
package it.domrita.meevy.resource.service;

import it.domrita.meevy.auth.repository.CurrentUserIdProvider;
import it.domrita.meevy.resource.dto.ResourceDownload;
import it.domrita.meevy.resource.dto.ResourceResponse;
import it.domrita.meevy.resource.exception.InvalidResourceException;
import it.domrita.meevy.resource.exception.ResourceNotFoundException;
import it.domrita.meevy.resource.exception.ResourceStorageException;
import it.domrita.meevy.resource.model.FileResource;
import it.domrita.meevy.resource.repository.FileResourceRepository;
import it.domrita.meevy.subject.exception.SubjectNotFoundException;
import it.domrita.meevy.subject.model.Subject;
import it.domrita.meevy.subject.repository.SubjectRepository;
import it.domrita.meevy.user.model.User;
import it.domrita.meevy.user.repository.UserRepository;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

@Service
public class ResourceService {

    private final FileResourceRepository resourceRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final CurrentUserIdProvider currentUserIdProvider;
    private final LocalFileStorageService storageService;
    private final TransactionTemplate transactionTemplate;

    public ResourceService(
            FileResourceRepository resourceRepository,
            UserRepository userRepository,
            SubjectRepository subjectRepository,
            CurrentUserIdProvider currentUserIdProvider,
            LocalFileStorageService storageService,
            PlatformTransactionManager transactionManager
    ) {
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
        this.currentUserIdProvider = currentUserIdProvider;
        this.storageService = storageService;
        this.transactionTemplate =
                new TransactionTemplate(transactionManager);
    }

    // Carica un documento e salva le sue informazioni nel database
    public ResourceResponse upload(
            MultipartFile file,
            Long subjectId
    ) {

        if (file == null || file.isEmpty()) {
            throw new InvalidResourceException(
                    "Il file non può essere vuoto."
            );
        }

        String filename = cleanFilename(file.getOriginalFilename());

        Long userId = currentUserIdProvider.getCurrentUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Utente non presente nel database."
                ));

        // La materia è facoltativa, ma se viene indicata
        // deve appartenere all'utente che sta caricando il file
        Subject subject = findOwnedSubject(subjectId, userId);

        String contentType = file.getContentType();

        if (contentType == null || contentType.isBlank()
                || contentType.length() > 120) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        final String validatedContentType = contentType;

        String storageKey;

        try {
            // Prima salviamo il documento sul disco
            storageKey = storageService.save(file.getInputStream());
        } catch (IOException | UncheckedIOException ex) {
            throw new ResourceStorageException(
                    "Errore durante il salvataggio del documento.",
                    ex
            );
        }

        try {
            // Poi registriamo i metadati nel database.
            // La transazione viene conclusa prima della risposta.
            return transactionTemplate.execute(status -> {

                FileResource resource = new FileResource(
                        user,
                        user,
                        subject,
                        filename,
                        storageKey,
                        validatedContentType,
                        file.getSize()
                );

                FileResource saved =
                        resourceRepository.saveAndFlush(resource);

                return toResponse(saved);
            });

        } catch (RuntimeException ex) {

            // Se il database fallisce, il file era già stato
            // scritto sul disco: proviamo a rimuoverlo.
            try {
                storageService.delete(storageKey);
            } catch (RuntimeException cleanupEx) {
                ex.addSuppressed(cleanupEx);
            }

            throw ex;
        }
    }

    // Recupera i propri file, eventualmente filtrati per materia
    @Transactional(readOnly = true)
    public List<ResourceResponse> getAll(Long subjectId) {

        Long userId = currentUserIdProvider.getCurrentUserId();

        List<FileResource> resources;

        if (subjectId == null) {
            resources = resourceRepository
                    .findAllByOwnerUser_IdOrderByCreatedAtDesc(userId);
        } else {

            findOwnedSubject(subjectId, userId);

            resources = resourceRepository
                    .findAllByOwnerUser_IdAndSubject_IdOrderByCreatedAtDesc(
                            userId, subjectId
                    );
        }

        return resources.stream()
                .map(this::toResponse)
                .toList();
    }

    // Recupera le informazioni di un singolo file
    @Transactional(readOnly = true)
    public ResourceResponse getById(Long id) {

        FileResource resource = findOwnedResource(id);

        return toResponse(resource);
    }

    // Prepara il file che verrà restituito nel download
    @Transactional(readOnly = true)
    public ResourceDownload download(Long id) {

        FileResource file = findOwnedResource(id);

        try {
            return new ResourceDownload(
                    storageService.load(file.getStorageKey()),
                    file.getOriginalFilename(),
                    file.getSizeBytes()
            );
        } catch (IllegalStateException | UncheckedIOException ex) {
            throw new ResourceStorageException(
                    "Il documento non è disponibile sul server.",
                    ex
            );
        }
    }

    // Elimina il record e successivamente il documento fisico
    public void delete(Long id) {

        String storageKey = transactionTemplate.execute(status -> {

            FileResource file = findOwnedResource(id);

            resourceRepository.delete(file);
            resourceRepository.flush();

            return file.getStorageKey();
        });

        /*
         * Eliminiamo il file solo dopo il commit sul database.
         * Se la cancellazione fisica fallisce, può rimanere
         * un file orfano, ma non perdiamo un documento ancora
         * referenziato dal database.
         */
        try {
            storageService.delete(storageKey);
        } catch (UncheckedIOException ex) {
            throw new ResourceStorageException(
                    "Il record è stato eliminato, ma il file fisico "
                            + "non è stato rimosso.",
                    ex
            );
        }
    }

    // Cerca un file controllando che appartenga all'utente corrente
    private FileResource findOwnedResource(Long id) {

        Long userId = currentUserIdProvider.getCurrentUserId();

        return resourceRepository
                .findByIdAndOwnerUser_Id(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    private Subject findOwnedSubject(Long subjectId, Long userId) {

        if (subjectId == null) {
            return null;
        }

        return subjectRepository
                .findByIdAndUser_Id(subjectId, userId)
                .orElseThrow(() -> new SubjectNotFoundException(subjectId));
    }

    // Teniamo solo il nome del documento, senza eventuali percorsi
    private String cleanFilename(String originalFilename) {

        if (originalFilename == null) {
            throw new InvalidResourceException(
                    "Il documento deve avere un nome."
            );
        }

        String filename = originalFilename
                .replace('\\', '/');

        filename = filename.substring(
                filename.lastIndexOf('/') + 1
        ).replaceAll("\\p{Cntrl}", "").trim();

        if (filename.isBlank()
                || filename.length() > 255
                || filename.equals(".")
                || filename.equals("..")) {

            throw new InvalidResourceException(
                    "Il nome del documento non è valido."
            );
        }

        return filename;
    }

    // Trasforma l'Entity nelle informazioni da restituire al client
    private ResourceResponse toResponse(FileResource resource) {

        Long subjectId = resource.getSubject() == null
                ? null
                : resource.getSubject().getId();

        return new ResourceResponse(
                resource.getId(),
                resource.getOriginalFilename(),
                resource.getContentType(),
                resource.getSizeBytes(),
                subjectId,
                resource.getCreatedAt()
        );
    }
}
