
package it.domrita.meevy.group.controller;

import it.domrita.meevy.common.exception.GlobalExceptionHandler;

import it.domrita.meevy.group.dto.*;
import it.domrita.meevy.group.exception.*;
import it.domrita.meevy.group.role.GroupRole;
import it.domrita.meevy.group.service.StudyGroupService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class StudyGroupControllerTest {

    @Mock
    private StudyGroupService studyGroupService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp(){

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new StudyGroupController(studyGroupService)
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }


    @Test //Creazione corretta del gruppo
    void createGroup_Test() throws Exception {

        StudyGroupRequest request = new StudyGroupRequest(
                "Basi di Dati",
                "Preparazione esame"
        );

        StudyGroupResponse response = new StudyGroupResponse(
                10L,
                "Basi di Dati",
                "Preparazione esame",
                Instant.now(),
                GroupRole.OWNER,
                1L
        );

        when(studyGroupService.createGroup(request))
                .thenReturn(response);

        mockMvc.perform(post("/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Basi di Dati",
                                    "description": "Preparazione esame"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Basi di Dati"))
                .andExpect(jsonPath("$.description").value("Preparazione esame"))
                .andExpect(jsonPath("$.myRole").value("OWNER"))
                .andExpect(jsonPath("$.memberCount").value(1));

        verify(studyGroupService).createGroup(request);
    }


    @Test //Recupero dei gruppi personali
    void getAllGroups_Test() throws Exception {

        Instant now = Instant.now();

        StudyGroupResponse group1 = new StudyGroupResponse(
                10L,
                "Basi di Dati",
                "Preparazione esame",
                now,
                GroupRole.OWNER,
                1L
        );

        StudyGroupResponse group2 = new StudyGroupResponse(
                11L,
                "Ingegneria del Software",
                "Progetto finale",
                now,
                GroupRole.MEMBER,
                4L
        );

        when(studyGroupService.getAllGroups())
                .thenReturn(List.of(group1, group2));

        mockMvc.perform(get("/groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].name").value("Basi di Dati"))
                .andExpect(jsonPath("$[0].myRole").value("OWNER"))
                .andExpect(jsonPath("$[1].id").value(11))
                .andExpect(jsonPath("$[1].myRole").value("MEMBER"))
                .andExpect(jsonPath("$[1].memberCount").value(4));

        verify(studyGroupService).getAllGroups();
    }


    @Test //Recupero del gruppo tramite id
    void getGroupById_Test() throws Exception {

        Long groupId = 10L;

        StudyGroupResponse response = new StudyGroupResponse(
                groupId,
                "Basi di Dati",
                "Preparazione esame",
                Instant.now(),
                GroupRole.MEMBER,
                3L
        );

        when(studyGroupService.getGroupById(groupId))
                .thenReturn(response);

        mockMvc.perform(get("/groups/{id}", groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Basi di Dati"))
                .andExpect(jsonPath("$.myRole").value("MEMBER"))
                .andExpect(jsonPath("$.memberCount").value(3));

        verify(studyGroupService).getGroupById(groupId);
    }


    @Test //Modifica corretta del gruppo
    void updateGroup_Test() throws Exception {

        Long groupId = 10L;

        StudyGroupRequest request = new StudyGroupRequest(
                "Basi di Dati 2",
                "Nuova descrizione"
        );

        StudyGroupResponse response = new StudyGroupResponse(
                groupId,
                "Basi di Dati 2",
                "Nuova descrizione",
                Instant.now(),
                GroupRole.OWNER,
                1L
        );

        when(studyGroupService.updateGroup(groupId, request))
                .thenReturn(response);

        mockMvc.perform(put("/groups/{id}", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Basi di Dati 2",
                                    "description": "Nuova descrizione"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Basi di Dati 2"))
                .andExpect(jsonPath("$.description").value("Nuova descrizione"));

        verify(studyGroupService).updateGroup(groupId, request);
    }


    @Test //Eliminazione corretta del gruppo
    void deleteGroup_Test() throws Exception {

        Long groupId = 10L;

        mockMvc.perform(delete("/groups/{id}", groupId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(studyGroupService).deleteGroup(groupId);
    }


    @Test //Recupero dei membri del gruppo
    void getMembers_Test() throws Exception {

        Long groupId = 10L;

        Instant now = Instant.now();

        GroupMemberResponse owner = new GroupMemberResponse(
                1L,
                "Domenico",
                "Test",
                GroupRole.OWNER,
                now
        );

        GroupMemberResponse member = new GroupMemberResponse(
                2L,
                "Rita",
                "Test",
                GroupRole.MEMBER,
                now
        );

        when(studyGroupService.getMembers(groupId))
                .thenReturn(List.of(owner, member));

        mockMvc.perform(get("/groups/{id}/members", groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[0].firstName").value("Domenico"))
                .andExpect(jsonPath("$[0].role").value("OWNER"))
                .andExpect(jsonPath("$[1].userId").value(2))
                .andExpect(jsonPath("$[1].firstName").value("Rita"))
                .andExpect(jsonPath("$[1].role").value("MEMBER"));

        verify(studyGroupService).getMembers(groupId);
    }


    @Test //Modifica del ruolo di un membro
    void updateMemberRole_Test() throws Exception {

        Long groupId = 10L;
        Long memberId = 2L;

        GroupRoleRequest request =
                new GroupRoleRequest(GroupRole.MODERATOR);

        GroupMemberResponse response = new GroupMemberResponse(
                memberId,
                "Rita",
                "Test",
                GroupRole.MODERATOR,
                Instant.now()
        );

        when(studyGroupService.updateMemberRole(
                groupId, memberId, request
        )).thenReturn(response);

        mockMvc.perform(patch(
                        "/groups/{id}/members/{userId}/role",
                        groupId,
                        memberId
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "role": "MODERATOR"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(2))
                .andExpect(jsonPath("$.role").value("MODERATOR"));

        verify(studyGroupService)
                .updateMemberRole(groupId, memberId, request);
    }


    @Test //Rimozione di un membro
    void removeMember_Test() throws Exception {

        Long groupId = 10L;
        Long memberId = 2L;

        mockMvc.perform(delete(
                        "/groups/{id}/members/{userId}",
                        groupId,
                        memberId
                ))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(studyGroupService).removeMember(groupId, memberId);
    }


    @Test //Abbandono del gruppo
    void leaveGroup_Test() throws Exception {

        Long groupId = 10L;

        mockMvc.perform(post("/groups/{id}/leave", groupId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(studyGroupService).leaveGroup(groupId);
    }


    @Test //Trasferimento della proprietà
    void transferOwnership_Test() throws Exception {

        Long groupId = 10L;
        Long newOwnerId = 2L;

        GroupOwnerTransferRequest request =
                new GroupOwnerTransferRequest(newOwnerId);

        mockMvc.perform(patch("/groups/{id}/owner", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "newOwnerUserId": 2
                                }
                                """))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(studyGroupService)
                .transferOwnership(groupId, request);
    }


    //TEST DELLE ECCEZIONI


    @Test //Gruppo inesistente o non accessibile
    void getGroupById_GroupNotFound() throws Exception {

        Long groupId = 10L;

        when(studyGroupService.getGroupById(groupId))
                .thenThrow(new GroupNotFoundException(groupId));

        mockMvc.perform(get("/groups/{id}", groupId))
                .andExpect(status().isNotFound())
                .andExpect(content().string(
                        containsString("GROUP_NOT_FOUND")
                ));

        verify(studyGroupService).getGroupById(groupId);
    }


    @Test //Membro non appartenente al gruppo
    void removeMember_MemberNotFound() throws Exception {

        Long groupId = 10L;
        Long memberId = 2L;

        doThrow(new GroupMemberNotFoundException(memberId))
                .when(studyGroupService)
                .removeMember(groupId, memberId);

        mockMvc.perform(delete(
                        "/groups/{id}/members/{userId}",
                        groupId,
                        memberId
                ))
                .andExpect(status().isNotFound())
                .andExpect(content().string(
                        containsString("GROUP_MEMBER_NOT_FOUND")
                ));

        verify(studyGroupService).removeMember(groupId, memberId);
    }


    @Test //Operazione senza permessi
    void deleteGroup_AccessDenied() throws Exception {

        Long groupId = 10L;

        doThrow(new GroupAccessDeniedException(
                "Solo il proprietario può eseguire questa operazione."
        )).when(studyGroupService).deleteGroup(groupId);

        mockMvc.perform(delete("/groups/{id}", groupId))
                .andExpect(status().isForbidden())
                .andExpect(content().string(
                        containsString("GROUP_ACCESS_DENIED")
                ));

        verify(studyGroupService).deleteGroup(groupId);
    }


    @Test //Tentativo di assegnare direttamente OWNER
    void updateMemberRole_InvalidRole() throws Exception {

        Long groupId = 10L;
        Long memberId = 2L;

        GroupRoleRequest request =
                new GroupRoleRequest(GroupRole.OWNER);

        when(studyGroupService.updateMemberRole(
                groupId, memberId, request
        )).thenThrow(new InvalidGroupRoleException(
                "È possibile assegnare solo i ruoli MODERATOR e MEMBER."
        ));

        mockMvc.perform(patch(
                        "/groups/{id}/members/{userId}/role",
                        groupId,
                        memberId
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "role": "OWNER"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        containsString("INVALID_GROUP_ROLE")
                ));

        verify(studyGroupService)
                .updateMemberRole(groupId, memberId, request);
    }


    @Test //OWNER non può abbandonare il gruppo
    void leaveGroup_OwnerCannotLeave() throws Exception {

        Long groupId = 10L;

        doThrow(new OwnerCannotLeaveException())
                .when(studyGroupService).leaveGroup(groupId);

        mockMvc.perform(post("/groups/{id}/leave", groupId))
                .andExpect(status().isConflict())
                .andExpect(content().string(
                        containsString("OWNER_CANNOT_LEAVE")
                ));

        verify(studyGroupService).leaveGroup(groupId);
    }


    @Test //Trasferimento della proprietà a se stessi
    void transferOwnership_InvalidTransfer() throws Exception {

        Long groupId = 10L;
        Long ownerId = 1L;

        GroupOwnerTransferRequest request =
                new GroupOwnerTransferRequest(ownerId);

        doThrow(new InvalidOwnershipTransferException(
                "Sei già il proprietario del gruppo."
        )).when(studyGroupService)
                .transferOwnership(groupId, request);

        mockMvc.perform(patch("/groups/{id}/owner", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "newOwnerUserId": 1
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().string(
                        containsString("INVALID_OWNERSHIP_TRANSFER")
                ));

        verify(studyGroupService)
                .transferOwnership(groupId, request);
    }


    //TEST DI VALIDAZIONE


    @Test //Nome del gruppo vuoto
    void createGroup_BlankName() throws Exception {

        mockMvc.perform(post("/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "   ",
                                    "description": "Preparazione esame"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        containsString("VALIDATION_ERROR")
                ))
                .andExpect(content().string(
                        containsString("name")
                ));

        verifyNoInteractions(studyGroupService);
    }


    @Test //Nome con più di 150 caratteri
    void createGroup_NameTooLong() throws Exception {

        String name = "a".repeat(151);

        mockMvc.perform(post("/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "%s",
                                    "description": "Preparazione esame"
                                }
                                """.formatted(name)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        containsString("VALIDATION_ERROR")
                ))
                .andExpect(content().string(
                        containsString("name")
                ));

        verifyNoInteractions(studyGroupService);
    }


    @Test //Descrizione con più di 500 caratteri
    void updateGroup_DescriptionTooLong() throws Exception {

        Long groupId = 10L;

        String description = "a".repeat(501);

        mockMvc.perform(put("/groups/{id}", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Basi di Dati",
                                    "description": "%s"
                                }
                                """.formatted(description)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        containsString("VALIDATION_ERROR")
                ))
                .andExpect(content().string(
                        containsString("description")
                ));

        verifyNoInteractions(studyGroupService);
    }


    @Test //Ruolo mancante
    void updateMemberRole_MissingRole() throws Exception {

        Long groupId = 10L;
        Long memberId = 2L;

        mockMvc.perform(patch(
                        "/groups/{id}/members/{userId}/role",
                        groupId,
                        memberId
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        containsString("VALIDATION_ERROR")
                ))
                .andExpect(content().string(
                        containsString("role")
                ));

        verifyNoInteractions(studyGroupService);
    }


    @Test //Ruolo non presente nell'enum
    void updateMemberRole_UnknownRole() throws Exception {

        Long groupId = 10L;
        Long memberId = 2L;

        mockMvc.perform(patch(
                        "/groups/{id}/members/{userId}/role",
                        groupId,
                        memberId
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        containsString("INVALID_JSON")
                ));

        verifyNoInteractions(studyGroupService);
    }


    @Test //Id del nuovo proprietario mancante
    void transferOwnership_MissingUserId() throws Exception {

        Long groupId = 10L;

        mockMvc.perform(patch("/groups/{id}/owner", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        containsString("VALIDATION_ERROR")
                ))
                .andExpect(content().string(
                        containsString("newOwnerUserId")
                ));

        verifyNoInteractions(studyGroupService);
    }


    @Test //Id del nuovo proprietario non positivo
    void transferOwnership_InvalidUserId() throws Exception {

        Long groupId = 10L;

        mockMvc.perform(patch("/groups/{id}/owner", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "newOwnerUserId": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        containsString("VALIDATION_ERROR")
                ))
                .andExpect(content().string(
                        containsString("newOwnerUserId")
                ));

        verifyNoInteractions(studyGroupService);
    }


    @Test //JSON malformato
    void createGroup_InvalidJson() throws Exception {

        mockMvc.perform(post("/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name":
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        containsString("INVALID_JSON")
                ));

        verifyNoInteractions(studyGroupService);
    }
}
