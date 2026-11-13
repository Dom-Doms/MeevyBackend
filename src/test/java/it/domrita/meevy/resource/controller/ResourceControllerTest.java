
package it.domrita.meevy.resource.controller;

import it.domrita.meevy.common.exception.GlobalExceptionHandler;
import it.domrita.meevy.resource.dto.ResourceDownload;
import it.domrita.meevy.resource.dto.ResourceResponse;
import it.domrita.meevy.resource.exception.InvalidResourceException;
import it.domrita.meevy.resource.exception.ResourceNotFoundException;
import it.domrita.meevy.resource.exception.ResourceStorageException;
import it.domrita.meevy.resource.service.ResourceService;
import it.domrita.meevy.subject.exception.SubjectNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ResourceControllerTest {

    private static final Long RESOURCE_ID = 10L;
    private static final Long SUBJECT_ID = 5L;

    private static final byte[] FILE_CONTENT =
            "Documento di prova".getBytes();

    @Mock
    private ResourceService resourceService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ResourceController(resourceService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private MockMultipartFile validFile() {
        return new MockMultipartFile(
                "file",
                "appunti.pdf",
                "application/pdf",
                FILE_CONTENT
        );
    }

    private ResourceResponse response(Long subjectId) {
        return new ResourceResponse(
                RESOURCE_ID,
                "appunti.pdf",
                "application/pdf",
                FILE_CONTENT.length,
                subjectId,
                Instant.parse("2026-11-04T12:00:00Z")
        );
    }

    // UPLOAD

    @Test
    void upload_restituisceCreated() throws Exception {

        MockMultipartFile file = validFile();

        when(resourceService.upload(file, SUBJECT_ID))
                .thenReturn(response(SUBJECT_ID));

        mockMvc.perform(multipart("/resources")
                        .file(file)
                        .param("subjectId", "5"))

                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.originalFilename")
                        .value("appunti.pdf"))
                .andExpect(jsonPath("$.subjectId").value(5));

        verify(resourceService).upload(file, SUBJECT_ID);
    }

    @Test
    void upload_senzaMateriaRestituisceCreated() throws Exception {

        MockMultipartFile file = validFile();

        when(resourceService.upload(
                any(MultipartFile.class), isNull()
        )).thenReturn(response(null));

        mockMvc.perform(multipart("/resources")
                        .file(file))

                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.subjectId").isEmpty());

        verify(resourceService).upload(
                any(MultipartFile.class), isNull()
        );
    }

    @Test
    void upload_senzaFileRestituisceBadRequest() throws Exception {

        mockMvc.perform(multipart("/resources")
                        .param("subjectId", "5"))

                .andExpect(status().isBadRequest());

        verifyNoInteractions(resourceService);
    }

    // ELENCO DEI DOCUMENTI

    @Test
    void getAll_restituisceListaDocumenti() throws Exception {

        when(resourceService.getAll(null))
                .thenReturn(List.of(response(null)));

        mockMvc.perform(get("/resources"))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].originalFilename")
                        .value("appunti.pdf"));

        verify(resourceService).getAll(null);
    }

    @Test
    void getAll_filtraPerMateria() throws Exception {

        when(resourceService.getAll(SUBJECT_ID))
                .thenReturn(List.of(response(SUBJECT_ID)));

        mockMvc.perform(get("/resources")
                        .param("subjectId", "5"))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subjectId").value(5));

        verify(resourceService).getAll(SUBJECT_ID);
    }

    // DETTAGLIO DOCUMENTO

    @Test
    void getById_restituisceMetadati() throws Exception {

        when(resourceService.getById(RESOURCE_ID))
                .thenReturn(response(SUBJECT_ID));

        mockMvc.perform(get("/resources/{id}", RESOURCE_ID))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.originalFilename")
                        .value("appunti.pdf"))
                .andExpect(jsonPath("$.contentType")
                        .value("application/pdf"))
                .andExpect(jsonPath("$.subjectId").value(5));

        verify(resourceService).getById(RESOURCE_ID);
    }

    // DOWNLOAD

    @Test
    void download_restituisceFileEHeaderCorretti() throws Exception {

        ByteArrayResource content =
                new ByteArrayResource(FILE_CONTENT);

        ResourceDownload download = new ResourceDownload(
                content,
                "appunti.pdf",
                FILE_CONTENT.length
        );

        when(resourceService.download(RESOURCE_ID))
                .thenReturn(download);

        mockMvc.perform(
                        get("/resources/{id}/download", RESOURCE_ID)
                )

                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        MediaType.APPLICATION_OCTET_STREAM
                ))
                .andExpect(content().bytes(FILE_CONTENT))
                .andExpect(header().longValue(
                        HttpHeaders.CONTENT_LENGTH,
                        FILE_CONTENT.length
                ))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        containsString("attachment")
                ))
                .andExpect(header().string(
                        "X-Content-Type-Options",
                        "nosniff"
                ));

        verify(resourceService).download(RESOURCE_ID);
    }

    // ELIMINAZIONE

    @Test
    void delete_restituisceNoContent() throws Exception {

        mockMvc.perform(delete("/resources/{id}", RESOURCE_ID))

                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(resourceService).delete(RESOURCE_ID);
    }

    // GESTIONE DEGLI ERRORI

    @Test
    void getById_fileNonTrovatoRestituisce404() throws Exception {

        when(resourceService.getById(RESOURCE_ID))
                .thenThrow(
                        new ResourceNotFoundException(RESOURCE_ID)
                );

        mockMvc.perform(get("/resources/{id}", RESOURCE_ID))

                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void upload_fileNonValidoRestituisce400() throws Exception {

        when(resourceService.upload(
                any(MultipartFile.class), isNull()
        )).thenThrow(
                new InvalidResourceException(
                        "Il documento non è valido."
                )
        );

        mockMvc.perform(multipart("/resources")
                        .file(validFile()))

                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_RESOURCE"));
    }

    @Test
    void getAll_materiaNonTrovataRestituisce404() throws Exception {

        when(resourceService.getAll(SUBJECT_ID))
                .thenThrow(
                        new SubjectNotFoundException(SUBJECT_ID)
                );

        mockMvc.perform(get("/resources")
                        .param("subjectId", "5"))

                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("SUBJECT_NOT_FOUND"));
    }

    @Test
    void download_erroreStorageRestituisce500() throws Exception {

        when(resourceService.download(RESOURCE_ID))
                .thenThrow(
                        new ResourceStorageException(
                                "File non disponibile.",
                                new IOException("Errore disco")
                        )
                );

        mockMvc.perform(
                        get("/resources/{id}/download", RESOURCE_ID)
                )

                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code")
                        .value("RESOURCE_STORAGE_ERROR"));
    }

    @Test
    void delete_fileNonTrovatoRestituisce404() throws Exception {

        doThrow(new ResourceNotFoundException(RESOURCE_ID))
                .when(resourceService)
                .delete(RESOURCE_ID);

        mockMvc.perform(delete("/resources/{id}", RESOURCE_ID))

                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("RESOURCE_NOT_FOUND"));
    }
}
