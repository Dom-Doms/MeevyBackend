
package it.domrita.meevy.resource.model;

import it.domrita.meevy.subject.model.Subject;
import it.domrita.meevy.user.model.User;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

/*
 * Rappresenta un file caricato su Meevy.
 *
 * Il database conserva soltanto i metadati.
 * Il contenuto fisico viene salvato sul server
 * e identificato tramite storageKey.
 */

@Entity
@Table(
        name = "file_resource",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_file_resource_storage_key",
                        columnNames = "storage_key"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Distinguiamo il proprietario da chi carica il documento.
     * Per i file personali saranno lo stesso utente, ma quando
     * avremo i gruppi questa distinzione ci servirà.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User ownerUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_user_id", nullable = false)
    private User uploadedByUser;

    // La materia è facoltativa
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    // Nome che aveva il documento quando è stato caricato
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    /*
     * Nome interno generato dal server per trovare il file sul disco.
     * Non usiamo il nome originale perché due utenti potrebbero
     * caricare documenti con lo stesso nome.
     */
    @Column(name = "storage_key", nullable = false, length = 255)
    private String storageKey;

    // Tipo del documento, ad esempio application/pdf
    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;

    // Dimensione del file in byte
    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public FileResource(
            User ownerUser,
            User uploadedByUser,
            Subject subject,
            String originalFilename,
            String storageKey,
            String contentType,
            long sizeBytes
    ) {
        // I dati principali devono essere presenti
        this.ownerUser = Objects.requireNonNull(ownerUser);
        this.uploadedByUser = Objects.requireNonNull(uploadedByUser);
        this.subject = subject;
        this.originalFilename = Objects.requireNonNull(originalFilename);
        this.storageKey = Objects.requireNonNull(storageKey);
        this.contentType = Objects.requireNonNull(contentType);

        if (sizeBytes <= 0) {
            throw new IllegalArgumentException(
                    "La dimensione del file deve essere positiva."
            );
        }

        this.sizeBytes = sizeBytes;
    }

    // Imposta automaticamente la data al primo salvataggio
    @PrePersist
    private void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
