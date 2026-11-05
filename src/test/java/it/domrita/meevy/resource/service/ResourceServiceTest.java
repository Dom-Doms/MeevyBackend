
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long SUBJECT_ID = 5L;
    private static final Long RESOURCE_ID = 10L;

    private static final String STORAGE_KEY =
            "123e4567-e89b-12d3-a456-426614174000";

    private static final Instant CREATED_AT =
            Instant.parse("2026-11-01T12:00:00Z");

    private static final byte[] FILE_CONTENT =
            "Documento di prova".getBytes(StandardCharsets.UTF_8);

    @Mock
    private FileResourceRepository resourceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private CurrentUserIdProvider currentUserIdProvider;

    @Mock
    private LocalFileStorageService storageService;

    @Mock
    private PlatformTransactionManager transactionManager;

    private ResourceService resourceService;

    @BeforeEach
    void setUp() {
        resourceService = new ResourceService(
                resourceRepository,
                userRepository,
                subjectRepository,
                currentUserIdProvider,
                storageService,
                transactionManager
        );
    }

    // Metodi di supporto usati da più test

    private MockMultipartFile validFile() {
        return new MockMultipartFile(
                "file",
                "appunti.pdf",
                "application/pdf",
                FILE_CONTENT
        );
    }

    private User authenticatedUser() {
        User user = new User(
                "test@example.com",
                "passwordHash",
                "Mario",
                "Rossi"
        );

        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(USER_ID);

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        return user;
    }

    private void currentUser() {
        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(USER_ID);
    }

    private Subject subject() {
        Subject subject = new Subject(
                "Analisi 1",
                mock(User.class)
        );

        ReflectionTestUtils.setField(
                subject, "id", SUBJECT_ID
        );

        return subject;
    }

    private FileResource savedResource(Subject subject) {
        User user = mock(User.class);

        FileResource resource = new FileResource(
                user,
                user,
                subject,
                "appunti.pdf",
                STORAGE_KEY,
                "application/pdf",
                FILE_CONTENT.length
        );

        ReflectionTestUtils.setField(
                resource, "id", RESOURCE_ID
        );

        ReflectionTestUtils.setField(
                resource, "createdAt", CREATED_AT
        );

        return resource;
    }

    private void mockTransaction() {
        when(transactionManager.getTransaction(
                any(TransactionDefinition.class)
        )).thenReturn(mock(TransactionStatus.class));
    }

    private void mockSuccessfulSave() {
        when(resourceRepository.saveAndFlush(
                any(FileResource.class)
        )).thenAnswer(invocation -> {

            FileResource resource =
                    invocation.getArgument(0);

            ReflectionTestUtils.setField(
                    resource, "id", RESOURCE_ID
            );

            ReflectionTestUtils.setField(
                    resource, "createdAt", CREATED_AT
            );

            return resource;
        });
    }

    // UPLOAD

    @Test
    void upload_rifiutaFileNullo() {

        assertThrows(
                InvalidResourceException.class,
                () -> resourceService.upload(null, null)
        );

        verifyNoInteractions(storageService, resourceRepository);
    }

    @Test
    void upload_rifiutaFileVuoto() {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "appunti.pdf",
                "application/pdf",
                new byte[0]
        );

        assertThrows(
                InvalidResourceException.class,
                () -> resourceService.upload(file, null)
        );

        verifyNoInteractions(storageService, resourceRepository);
    }

    @Test
    void upload_rifiutaNomeNonValido() {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "..",
                "application/pdf",
                FILE_CONTENT
        );

        assertThrows(
                InvalidResourceException.class,
                () -> resourceService.upload(file, null)
        );

        verifyNoInteractions(storageService, resourceRepository);
    }

    @Test
    void upload_rifiutaMateriaNonAppartenenteAllUtente() {

        authenticatedUser();

        when(subjectRepository.findByIdAndUser_Id(
                SUBJECT_ID, USER_ID
        )).thenReturn(Optional.empty());

        assertThrows(
                SubjectNotFoundException.class,
                () -> resourceService.upload(validFile(), SUBJECT_ID)
        );

        verifyNoInteractions(storageService, resourceRepository);
    }

    @Test
    void upload_rifiutaUtenteNonPresente() {

        currentUser();

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> resourceService.upload(validFile(), null)
        );

        verifyNoInteractions(storageService, resourceRepository);
    }

    @Test
    void upload_salvaFileEMetadatiConMateria() {

        User user = authenticatedUser();
        Subject subject = subject();

        when(subjectRepository.findByIdAndUser_Id(
                SUBJECT_ID, USER_ID
        )).thenReturn(Optional.of(subject));

        when(storageService.save(any(InputStream.class)))
                .thenReturn(STORAGE_KEY);

        mockTransaction();
        mockSuccessfulSave();

        ResourceResponse response = resourceService.upload(
                validFile(), SUBJECT_ID
        );

        ArgumentCaptor<FileResource> captor =
                ArgumentCaptor.forClass(FileResource.class);

        verify(resourceRepository).saveAndFlush(
                captor.capture()
        );

        FileResource saved = captor.getValue();

        assertSame(user, saved.getOwnerUser());
        assertSame(user, saved.getUploadedByUser());
        assertSame(subject, saved.getSubject());

        assertEquals("appunti.pdf", saved.getOriginalFilename());
        assertEquals(STORAGE_KEY, saved.getStorageKey());
        assertEquals("application/pdf", saved.getContentType());
        assertEquals(FILE_CONTENT.length, saved.getSizeBytes());

        assertEquals(RESOURCE_ID, response.id());
        assertEquals(SUBJECT_ID, response.subjectId());
        assertEquals(CREATED_AT, response.createdAt());

        verify(storageService).save(any(InputStream.class));
        verify(transactionManager).commit(
                any(TransactionStatus.class)
        );
    }

    @Test
    void upload_salvaFileSenzaMateria() {

        authenticatedUser();

        when(storageService.save(any(InputStream.class)))
                .thenReturn(STORAGE_KEY);

        mockTransaction();
        mockSuccessfulSave();

        ResourceResponse response = resourceService.upload(
                validFile(), null
        );

        assertEquals(RESOURCE_ID, response.id());
        assertNull(response.subjectId());

        verifyNoInteractions(subjectRepository);
        verify(resourceRepository).saveAndFlush(
                any(FileResource.class)
        );
    }

    @Test
    void upload_usaContentTypePredefinito() {

        authenticatedUser();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "documento.bin",
                null,
                FILE_CONTENT
        );

        when(storageService.save(any(InputStream.class)))
                .thenReturn(STORAGE_KEY);

        mockTransaction();
        mockSuccessfulSave();

        ResourceResponse response =
                resourceService.upload(file, null);

        assertEquals(
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                response.contentType()
        );
    }

    @Test
    void upload_rimuovePercorsoDalNomeOriginale() {

        authenticatedUser();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "C:\\documenti\\appunti.pdf",
                "application/pdf",
                FILE_CONTENT
        );

        when(storageService.save(any(InputStream.class)))
                .thenReturn(STORAGE_KEY);

        mockTransaction();
        mockSuccessfulSave();

        ResourceResponse response =
                resourceService.upload(file, null);

        assertEquals(
                "appunti.pdf",
                response.originalFilename()
        );
    }

    @Test
    void upload_erroreStorageNonSalvaNelDatabase() {

        authenticatedUser();

        when(storageService.save(any(InputStream.class)))
                .thenThrow(
                        new UncheckedIOException(
                                new IOException("Disco non disponibile")
                        )
                );

        assertThrows(
                ResourceStorageException.class,
                () -> resourceService.upload(validFile(), null)
        );

        verifyNoInteractions(resourceRepository);
        verifyNoInteractions(transactionManager);
    }

    @Test
    void upload_erroreDatabaseEliminaFileFisico() {

        authenticatedUser();

        when(storageService.save(any(InputStream.class)))
                .thenReturn(STORAGE_KEY);

        mockTransaction();

        when(resourceRepository.saveAndFlush(
                any(FileResource.class)
        )).thenThrow(
                new IllegalStateException("Database non disponibile")
        );

        assertThrows(
                IllegalStateException.class,
                () -> resourceService.upload(validFile(), null)
        );

        verify(transactionManager).rollback(
                any(TransactionStatus.class)
        );

        verify(storageService).delete(STORAGE_KEY);

        verify(transactionManager, never()).commit(
                any(TransactionStatus.class)
        );
    }

    @Test
    void upload_conservaErrorePuliziaComeSuppressed() {

        authenticatedUser();

        when(storageService.save(any(InputStream.class)))
                .thenReturn(STORAGE_KEY);

        mockTransaction();

        when(resourceRepository.saveAndFlush(
                any(FileResource.class)
        )).thenThrow(
                new IllegalStateException("Errore database")
        );

        doThrow(new IllegalStateException("Errore pulizia"))
                .when(storageService)
                .delete(STORAGE_KEY);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> resourceService.upload(validFile(), null)
        );

        assertEquals(
                "Errore database",
                exception.getMessage()
        );

        assertEquals(1, exception.getSuppressed().length);
        assertEquals(
                "Errore pulizia",
                exception.getSuppressed()[0].getMessage()
        );
    }

    // ELENCO E DETTAGLIO

    @Test
    void getAll_restituisceFilePersonali() {

        currentUser();

        FileResource resource = savedResource(null);

        when(resourceRepository
                .findAllByOwnerUser_IdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of(resource));

        List<ResourceResponse> result =
                resourceService.getAll(null);

        assertEquals(1, result.size());
        assertEquals(RESOURCE_ID, result.get(0).id());
        assertNull(result.get(0).subjectId());
    }

    @Test
    void getAll_filtraPerMateria() {

        currentUser();

        Subject subject = subject();
        FileResource resource = savedResource(subject);

        when(subjectRepository.findByIdAndUser_Id(
                SUBJECT_ID, USER_ID
        )).thenReturn(Optional.of(subject));

        when(resourceRepository
                .findAllByOwnerUser_IdAndSubject_IdOrderByCreatedAtDesc(
                        USER_ID, SUBJECT_ID
                )).thenReturn(List.of(resource));

        List<ResourceResponse> result =
                resourceService.getAll(SUBJECT_ID);

        assertEquals(1, result.size());
        assertEquals(SUBJECT_ID, result.get(0).subjectId());
    }

    @Test
    void getAll_rifiutaMateriaNonAutorizzata() {

        currentUser();

        when(subjectRepository.findByIdAndUser_Id(
                SUBJECT_ID, USER_ID
        )).thenReturn(Optional.empty());

        assertThrows(
                SubjectNotFoundException.class,
                () -> resourceService.getAll(SUBJECT_ID)
        );

        verifyNoInteractions(resourceRepository);
    }

    @Test
    void getById_restituisceMetadati() {

        currentUser();

        FileResource resource = savedResource(null);

        when(resourceRepository.findByIdAndOwnerUser_Id(
                RESOURCE_ID, USER_ID
        )).thenReturn(Optional.of(resource));

        ResourceResponse response =
                resourceService.getById(RESOURCE_ID);

        assertEquals(RESOURCE_ID, response.id());
        assertEquals("appunti.pdf", response.originalFilename());
        assertEquals(FILE_CONTENT.length, response.sizeBytes());
        assertEquals(CREATED_AT, response.createdAt());
    }

    @Test
    void getById_rifiutaFileNonAutorizzato() {

        currentUser();

        when(resourceRepository.findByIdAndOwnerUser_Id(
                RESOURCE_ID, USER_ID
        )).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> resourceService.getById(RESOURCE_ID)
        );

        verifyNoInteractions(storageService);
    }

    // DOWNLOAD

    @Test
    void download_restituisceFileENomeOriginale() {

        currentUser();

        FileResource file = savedResource(null);

        when(resourceRepository.findByIdAndOwnerUser_Id(
                RESOURCE_ID, USER_ID
        )).thenReturn(Optional.of(file));

        Resource content = new ByteArrayResource(FILE_CONTENT);

        when(storageService.load(STORAGE_KEY))
                .thenReturn(content);

        ResourceDownload result =
                resourceService.download(RESOURCE_ID);

        assertSame(content, result.content());
        assertEquals("appunti.pdf", result.filename());
        assertEquals(FILE_CONTENT.length, result.sizeBytes());

        verify(storageService).load(STORAGE_KEY);
    }

    @Test
    void download_fileFisicoMancanteGeneraErrore() {

        currentUser();

        when(resourceRepository.findByIdAndOwnerUser_Id(
                RESOURCE_ID, USER_ID
        )).thenReturn(Optional.of(savedResource(null)));

        when(storageService.load(STORAGE_KEY))
                .thenThrow(
                        new IllegalStateException("File non presente")
                );

        assertThrows(
                ResourceStorageException.class,
                () -> resourceService.download(RESOURCE_ID)
        );
    }

    @Test
    void download_rifiutaFileNonAutorizzato() {

        currentUser();

        when(resourceRepository.findByIdAndOwnerUser_Id(
                RESOURCE_ID, USER_ID
        )).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> resourceService.download(RESOURCE_ID)
        );

        verifyNoInteractions(storageService);
    }

    // ELIMINAZIONE

    @Test
    void delete_eliminaRecordEFileFisico() {

        currentUser();
        mockTransaction();

        FileResource resource = savedResource(null);

        when(resourceRepository.findByIdAndOwnerUser_Id(
                RESOURCE_ID, USER_ID
        )).thenReturn(Optional.of(resource));

        resourceService.delete(RESOURCE_ID);

        verify(resourceRepository).delete(resource);
        verify(resourceRepository).flush();

        InOrder order = inOrder(
                transactionManager,
                storageService
        );

        order.verify(transactionManager).commit(
                any(TransactionStatus.class)
        );

        order.verify(storageService).delete(STORAGE_KEY);
    }

    @Test
    void delete_rifiutaFileNonAutorizzato() {

        currentUser();
        mockTransaction();

        when(resourceRepository.findByIdAndOwnerUser_Id(
                RESOURCE_ID, USER_ID
        )).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> resourceService.delete(RESOURCE_ID)
        );

        verify(resourceRepository, never()).delete(
                any(FileResource.class)
        );

        verifyNoInteractions(storageService);
    }

    @Test
    void delete_erroreDatabaseNonEliminaFileFisico() {

        currentUser();
        mockTransaction();

        FileResource resource = savedResource(null);

        when(resourceRepository.findByIdAndOwnerUser_Id(
                RESOURCE_ID, USER_ID
        )).thenReturn(Optional.of(resource));

        doThrow(new IllegalStateException("Errore database"))
                .when(resourceRepository)
                .flush();

        assertThrows(
                IllegalStateException.class,
                () -> resourceService.delete(RESOURCE_ID)
        );

        verify(transactionManager).rollback(
                any(TransactionStatus.class)
        );

        verifyNoInteractions(storageService);
    }

    @Test
    void delete_erroreFilesystemGeneraEccezione() {

        currentUser();
        mockTransaction();

        FileResource resource = savedResource(null);

        when(resourceRepository.findByIdAndOwnerUser_Id(
                RESOURCE_ID, USER_ID
        )).thenReturn(Optional.of(resource));

        doThrow(
                new UncheckedIOException(
                        new IOException("Impossibile eliminare")
                )
        ).when(storageService).delete(STORAGE_KEY);

        assertThrows(
                ResourceStorageException.class,
                () -> resourceService.delete(RESOURCE_ID)
        );

        verify(transactionManager).commit(
                any(TransactionStatus.class)
        );

        verify(storageService).delete(STORAGE_KEY);
    }
}
