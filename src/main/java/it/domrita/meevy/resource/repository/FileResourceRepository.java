
package it.domrita.meevy.resource.repository;

import it.domrita.meevy.resource.model.FileResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileResourceRepository
        extends JpaRepository<FileResource, Long> {

    // Tutti i file di un utente, dal più recente
    List<FileResource> findAllByOwnerUser_IdOrderByCreatedAtDesc(
            Long userId
    );

    // File di una specifica materia appartenenti all'utente
    List<FileResource> findAllByOwnerUser_IdAndSubject_IdOrderByCreatedAtDesc(
            Long userId,
            Long subjectId
    );

    // Cerca il file controllando anche chi ne è il proprietario
    Optional<FileResource> findByIdAndOwnerUser_Id(
            Long id,
            Long userId
    );
}
