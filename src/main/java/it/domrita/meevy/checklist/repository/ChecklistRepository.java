package it.domrita.meevy.checklist.repository;

import it.domrita.meevy.checklist.model.Checklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChecklistRepository
        extends JpaRepository<Checklist, Long> {

    // Cerca la checklist di una materia appartenente all'utente.
    Optional<Checklist> findBySubject_IdAndSubject_User_Id(
            Long subjectId, Long userId
    );

    // Cerca una checklist tramite il suo ID,
    // verificando che appartenga all'utente.
    Optional<Checklist> findByIdAndSubject_User_Id(
            Long id, Long userId
    );
}