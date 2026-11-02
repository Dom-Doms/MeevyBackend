
package it.domrita.meevy.resource.dto;

import java.time.Instant;

// Informazioni del file che restituiamo a Flutter o Postman
public record ResourceResponse(
        Long id,
        String originalFilename,
        String contentType,
        long sizeBytes,
        Long subjectId,
        Instant createdAt
) {}
