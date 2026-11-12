
package it.domrita.meevy.group.controller;

import it.domrita.meevy.group.dto.GroupInvitationAcceptRequest;
import it.domrita.meevy.group.dto.GroupInvitationResponse;
import it.domrita.meevy.group.dto.StudyGroupResponse;
import it.domrita.meevy.group.service.GroupInvitationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class GroupInvitationController {

    private final GroupInvitationService groupInvitationService;


    //Genera un invito per il gruppo.
    @PostMapping("/groups/{id}/invitations")
    public ResponseEntity<GroupInvitationResponse> createInvitation(
            @PathVariable Long id
    ){

        GroupInvitationResponse response =
                groupInvitationService.createInvitation(id);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    //Accetta l'invito e accede al gruppo.
    @PostMapping("/group-invitations/accept")
    public ResponseEntity<StudyGroupResponse> acceptInvitation(
            @Valid @RequestBody GroupInvitationAcceptRequest request
    ){

        StudyGroupResponse response =
                groupInvitationService.acceptInvitation(request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
