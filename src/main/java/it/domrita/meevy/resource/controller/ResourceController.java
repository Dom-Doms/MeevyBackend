
package it.domrita.meevy.resource.controller;

import it.domrita.meevy.resource.dto.ResourceDownload;
import it.domrita.meevy.resource.dto.ResourceResponse;
import it.domrita.meevy.resource.service.ResourceService;

import lombok.RequiredArgsConstructor;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    /*
     * Carica un documento.
     * Usiamo multipart/form-data perché dobbiamo ricevere
     * il file vero e proprio, non soltanto dati JSON.
     *
     * subjectId è facoltativo.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResourceResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) Long subjectId
    ) {

        ResourceResponse response =
                resourceService.upload(file, subjectId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Elenca i propri documenti, con filtro opzionale per materia
    @GetMapping
    public ResponseEntity<List<ResourceResponse>> getAll(
            @RequestParam(required = false) Long subjectId
    ) {

        return ResponseEntity.ok(
                resourceService.getAll(subjectId)
        );
    }

    // Restituisce solo i metadati, non il contenuto del file
    @GetMapping("/{id}")
    public ResponseEntity<ResourceResponse> getById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                resourceService.getById(id)
        );
    }

    // Restituisce il documento fisico per il download
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(
            @PathVariable Long id
    ) {

        ResourceDownload download = resourceService.download(id);

        /*
         * Content-Disposition dice al client di scaricare
         * il documento usando il nome originale.
         * Il charset serve a gestire anche nomi con accenti.
         */
        ContentDisposition disposition =
                ContentDisposition.attachment()
                        .filename(
                                download.filename(),
                                StandardCharsets.UTF_8
                        )
                        .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(download.sizeBytes())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString()
                )
                .header("X-Content-Type-Options", "nosniff")
                .body(download.content());
    }

    // Elimina il file personale
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {

        resourceService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
