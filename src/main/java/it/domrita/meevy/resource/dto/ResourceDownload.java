
package it.domrita.meevy.resource.dto;

import org.springframework.core.io.Resource;

// Passa al Controller il documento e le informazioni
// necessarie per farlo scaricare con il nome originale
public record ResourceDownload(
        Resource content,
        String filename,
        long sizeBytes
) {}
