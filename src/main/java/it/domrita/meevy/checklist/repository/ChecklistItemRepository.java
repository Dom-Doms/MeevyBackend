package it.domrita.meevy.checklist.repository;

import it.domrita.meevy.checklist.model.ChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChecklistItemRepository
        extends JpaRepository<ChecklistItem, Long> {

    // Recupera tutte le attività della checklist di un utente.
    List<ChecklistItem>
    findAllByChecklist_IdAndChecklist_Subject_User_IdOrderByIdAsc(
            Long checklistId, Long userId
    );

    // Cerca una singola attività verificandone il proprietario.
    Optional<ChecklistItem>
    findByIdAndChecklist_Subject_User_Id(
            Long id, Long userId
    );
}