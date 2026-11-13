
package it.domrita.meevy.group.controller;

import it.domrita.meevy.group.dto.*;
import it.domrita.meevy.group.service.StudyGroupService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
public class StudyGroupController {

    private final StudyGroupService studyGroupService;

    // Crea un gruppo e assegna al creatore il ruolo OWNER.
    @PostMapping
    public ResponseEntity<StudyGroupResponse> createGroup(
            @Valid @RequestBody StudyGroupRequest request
    ) {
        StudyGroupResponse response =
                studyGroupService.createGroup(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Recupera tutti i gruppi dell'utente corrente.
    @GetMapping
    public ResponseEntity<List<StudyGroupResponse>> getAllGroups() {

        List<StudyGroupResponse> groups =
                studyGroupService.getAllGroups();

        return ResponseEntity.status(HttpStatus.OK).body(groups);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudyGroupResponse> getGroupById(
            @PathVariable Long id
    ) {
        StudyGroupResponse group =
                studyGroupService.getGroupById(id);

        return ResponseEntity.status(HttpStatus.OK).body(group);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudyGroupResponse> updateGroup(
            @PathVariable Long id,
            @Valid @RequestBody StudyGroupRequest request
    ) {
        StudyGroupResponse response =
                studyGroupService.updateGroup(id, request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // Elimina il gruppo.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(
            @PathVariable Long id
    ) {
        studyGroupService.deleteGroup(id);

        return ResponseEntity.noContent().build();
    }

    // Recupera i membri del gruppo.
    @GetMapping("/{id}/members")
    public ResponseEntity<List<GroupMemberResponse>> getMembers(
            @PathVariable Long id
    ) {
        List<GroupMemberResponse> members =
                studyGroupService.getMembers(id);

        return ResponseEntity.status(HttpStatus.OK).body(members);
    }

    // Modifica il ruolo di un membro.
    @PatchMapping("/{id}/members/{userId}/role")
    public ResponseEntity<GroupMemberResponse> updateMemberRole(
            @PathVariable Long id,
            @PathVariable Long userId,
            @Valid @RequestBody GroupRoleRequest request
    ) {
        GroupMemberResponse response =
                studyGroupService.updateMemberRole(id, userId, request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    // Espelle un membro dal gruppo.
    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long id,
            @PathVariable Long userId
    ) {
        studyGroupService.removeMember(id, userId);

        return ResponseEntity.noContent().build();
    }

    // L'utente corrente abbandona il gruppo.
    @PostMapping("/{id}/leave")
    public ResponseEntity<Void> leaveGroup(
            @PathVariable Long id
    ) {
        studyGroupService.leaveGroup(id);

        return ResponseEntity.noContent().build();
    }

    // Trasferisce la proprietà del gruppo.
    @PatchMapping("/{id}/owner")
    public ResponseEntity<Void> transferOwnership(
            @PathVariable Long id,
            @Valid @RequestBody GroupOwnerTransferRequest request
    ) {
        studyGroupService.transferOwnership(id, request);

        return ResponseEntity.noContent().build();
    }
}
