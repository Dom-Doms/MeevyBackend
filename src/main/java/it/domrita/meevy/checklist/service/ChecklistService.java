package it.domrita.meevy.checklist.service;

import it.domrita.meevy.auth.repository.CurrentUserIdProvider;
import it.domrita.meevy.checklist.dto.ChecklistRequest;
import it.domrita.meevy.checklist.dto.ChecklistResponse;
import it.domrita.meevy.checklist.model.Checklist;
import it.domrita.meevy.checklist.repository.ChecklistRepository;
import it.domrita.meevy.subject.exception.SubjectNotFoundException;
import it.domrita.meevy.subject.model.Subject;
import it.domrita.meevy.subject.repository.SubjectRepository;
import it.domrita.meevy.checklist.exception.ChecklistNotFoundException;
import it.domrita.meevy.checklist.dto.ChecklistItemRequest;
import it.domrita.meevy.checklist.dto.ChecklistItemResponse;
import it.domrita.meevy.checklist.model.ChecklistItem;
import it.domrita.meevy.checklist.repository.ChecklistItemRepository;
import it.domrita.meevy.checklist.dto.ChecklistItemCompletedRequest;
import it.domrita.meevy.checklist.exception.ChecklistItemNotFoundException;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChecklistService {

    private final ChecklistRepository checklistRepository;
    private final SubjectRepository subjectRepository;
    private final CurrentUserIdProvider currentUserIdProvider;
    private final ChecklistItemRepository checklistItemRepository;

    public ChecklistResponse createChecklist(ChecklistRequest request) {

        Long userId = currentUserIdProvider.getCurrentUserId();

        // Verifica che la materia appartenga all'utente autenticato.
        Subject subject = subjectRepository
                .findByIdAndUser_Id(request.subjectId(), userId)
                .orElseThrow(() ->
                        new SubjectNotFoundException(request.subjectId())
                );

        Checklist checklist = new Checklist(subject);

        Checklist savedChecklist =
                checklistRepository.saveAndFlush(checklist);

        return toResponse(savedChecklist);
    }

    @Transactional(readOnly = true)
    public ChecklistResponse getChecklistBySubjectId(Long subjectId) {

        Long userId = currentUserIdProvider.getCurrentUserId();

        // Verifica che la materia appartenga all'utente.
        subjectRepository.findByIdAndUser_Id(subjectId, userId)
                .orElseThrow(() ->
                        new SubjectNotFoundException(subjectId)
                );

        // Recupera la checklist associata alla materia.
        Checklist checklist = checklistRepository
                .findBySubject_IdAndSubject_User_Id(subjectId, userId)
                .orElseThrow(() ->
                        new ChecklistNotFoundException(subjectId)
                );

        return toResponse(checklist);
    }



    public ChecklistItemResponse createChecklistItem(
            Long checklistId,
            ChecklistItemRequest request
    ) {

        Long userId = currentUserIdProvider.getCurrentUserId();

        // Verifica che la checklist appartenga all'utente.
        Checklist checklist = checklistRepository
                .findByIdAndSubject_User_Id(checklistId, userId)
                .orElseThrow(() ->
                        new ChecklistNotFoundException(
                                "Checklist non trovata: " + checklistId
                        )
                );

        // Crea una nuova attività inizialmente non completata.
        ChecklistItem item = new ChecklistItem(
                checklist,
                request.text().trim()
        );

        ChecklistItem savedItem =
                checklistItemRepository.save(item);

        return toItemResponse(savedItem);
    }

    // Recupera tutte le attività di una checklist.
    @Transactional(readOnly = true)
    public List<ChecklistItemResponse> getChecklistItems(
            Long checklistId
    ) {

        Long userId = currentUserIdProvider.getCurrentUserId();

        // Verifica che la checklist esista e appartenga all'utente.
        checklistRepository
                .findByIdAndSubject_User_Id(checklistId, userId)
                .orElseThrow(() ->
                        new ChecklistNotFoundException(
                                "Checklist non trovata: " + checklistId
                        )
                );

        // Recupera le attività ordinate per ID.
        return checklistItemRepository
                .findAllByChecklist_IdAndChecklist_Subject_User_IdOrderByIdAsc(
                        checklistId, userId
                )
                .stream()
                .map(this::toItemResponse)
                .toList();
    }


    // Modifica il testo di un'attività.
    public ChecklistItemResponse updateChecklistItem(
            Long itemId,
            ChecklistItemRequest request
    ) {

        ChecklistItem item = requireOwnedItem(itemId);

        item.updateText(request.text().trim());

        ChecklistItem updatedItem =
                checklistItemRepository.save(item);

        return toItemResponse(updatedItem);
    }


    // Spunta o deseleziona un'attività.
    public ChecklistItemResponse updateChecklistItemCompleted(
            Long itemId,
            ChecklistItemCompletedRequest request
    ) {

        ChecklistItem item = requireOwnedItem(itemId);

        item.setCompleted(request.completed());

        ChecklistItem updatedItem =
                checklistItemRepository.save(item);

        return toItemResponse(updatedItem);
    }


    // Elimina un'attività dalla checklist.
    public void deleteChecklistItem(Long itemId) {

        ChecklistItem item = requireOwnedItem(itemId);

        checklistItemRepository.delete(item);
    }


    // Recupera un'attività soltanto se appartiene
    // all'utente autenticato.
    private ChecklistItem requireOwnedItem(Long itemId) {

        Long userId = currentUserIdProvider.getCurrentUserId();

        return checklistItemRepository
                .findByIdAndChecklist_Subject_User_Id(itemId, userId)
                .orElseThrow(() ->
                        new ChecklistItemNotFoundException(itemId)
                );
    }

    private ChecklistResponse toResponse(Checklist checklist) {

        return new ChecklistResponse(
                checklist.getId(),
                checklist.getSubject().getId()
        );
    }

    private ChecklistItemResponse toItemResponse(ChecklistItem item) {

        return new ChecklistItemResponse(
                item.getId(),
                item.getChecklist().getId(),
                item.getText(),
                item.isCompleted()
        );
    }

}