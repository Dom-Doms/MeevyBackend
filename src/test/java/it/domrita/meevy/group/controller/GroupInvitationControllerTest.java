
package it.domrita.meevy.group.controller;

import it.domrita.meevy.common.exception.GlobalExceptionHandler;

import it.domrita.meevy.group.dto.*;
import it.domrita.meevy.group.exception.*;
import it.domrita.meevy.group.role.GroupRole;
import it.domrita.meevy.group.service.GroupInvitationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class GroupInvitationControllerTest {

    @Mock
    private GroupInvitationService groupInvitationService;

    private MockMvc mockMvc;

    private final String token = "a".repeat(43);

    @BeforeEach
    void setUp(){

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new GroupInvitationController(
                                groupInvitationService
                        )
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }


    @Test //Creazione corretta dell'invito
    void createInvitation_Test() throws Exception {

        Long groupId = 10L;

        GroupInvitationResponse response =
                new GroupInvitationResponse(
                        groupId,
                        token,
                        Instant.now().plusSeconds(86400)
                );

        when(groupInvitationService.createInvitation(groupId))
                .thenReturn(response);

        mockMvc.perform(post(
                        "/groups/{id}/invitations",
                        groupId
                ))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.groupId").value(10))
                .andExpect(jsonPath("$.token").value(token))
                .andExpect(jsonPath("$.expiresAt").exists());

        verify(groupInvitationService).createInvitation(groupId);
    }


    @Test //Accettazione corretta dell'invito
    void acceptInvitation_Test() throws Exception {

        GroupInvitationAcceptRequest request =
                new GroupInvitationAcceptRequest(token);

        StudyGroupResponse response = new StudyGroupResponse(
                10L,
                "Basi di Dati",
                "Preparazione esame",
                Instant.now(),
                GroupRole.MEMBER,
                2L
        );

        when(groupInvitationService.acceptInvitation(request))
                .thenReturn(response);

        mockMvc.perform(post("/group-invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "token": "%s"
                                }
                                """.formatted(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Basi di Dati"))
                .andExpect(jsonPath("$.myRole").value("MEMBER"))
                .andExpect(jsonPath("$.memberCount").value(2));

        verify(groupInvitationService).acceptInvitation(request);
    }


    @Test //Creazione di un invito senza permessi
    void createInvitation_AccessDenied() throws Exception {

        Long groupId = 10L;

        when(groupInvitationService.createInvitation(groupId))
                .thenThrow(new GroupAccessDeniedException(
                        "Non hai i permessi per creare inviti."
                ));

        mockMvc.perform(post(
                        "/groups/{id}/invitations",
                        groupId
                ))
                .andExpect(status().isForbidden())
                .andExpect(content().string(
                        containsString("GROUP_ACCESS_DENIED")
                ));

        verify(groupInvitationService).createInvitation(groupId);
    }


    @Test //Gruppo inesistente o non accessibile
    void createInvitation_GroupNotFound() throws Exception {

        Long groupId = 10L;

        when(groupInvitationService.createInvitation(groupId))
                .thenThrow(new GroupNotFoundException(groupId));

        mockMvc.perform(post(
                        "/groups/{id}/invitations",
                        groupId
                ))
                .andExpect(status().isNotFound())
                .andExpect(content().string(
                        containsString("GROUP_NOT_FOUND")
                ));

        verify(groupInvitationService).createInvitation(groupId);
    }


    @Test //Token inesistente
    void acceptInvitation_InvalidToken() throws Exception {

        GroupInvitationAcceptRequest request =
                new GroupInvitationAcceptRequest(token);

        when(groupInvitationService.acceptInvitation(request))
                .thenThrow(new InvalidGroupInvitationException());

        mockMvc.perform(post("/group-invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "token": "%s"
                                }
                                """.formatted(token)))
                .andExpect(status().isNotFound())
                .andExpect(content().string(
                        containsString("INVALID_GROUP_INVITATION")
                ));

        verify(groupInvitationService).acceptInvitation(request);
    }


    @Test //Invito scaduto
    void acceptInvitation_ExpiredInvitation() throws Exception {

        GroupInvitationAcceptRequest request =
                new GroupInvitationAcceptRequest(token);

        when(groupInvitationService.acceptInvitation(request))
                .thenThrow(new GroupInvitationExpiredException());

        mockMvc.perform(post("/group-invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "token": "%s"
                                }
                                """.formatted(token)))
                .andExpect(status().isGone())
                .andExpect(content().string(
                        containsString("GROUP_INVITATION_EXPIRED")
                ));

        verify(groupInvitationService).acceptInvitation(request);
    }


    @Test //Invito già utilizzato
    void acceptInvitation_AlreadyUsed() throws Exception {

        GroupInvitationAcceptRequest request =
                new GroupInvitationAcceptRequest(token);

        when(groupInvitationService.acceptInvitation(request))
                .thenThrow(new GroupInvitationUsedException());

        mockMvc.perform(post("/group-invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "token": "%s"
                                }
                                """.formatted(token)))
                .andExpect(status().isConflict())
                .andExpect(content().string(
                        containsString("GROUP_INVITATION_USED")
                ));

        verify(groupInvitationService).acceptInvitation(request);
    }


    @Test //Utente già appartenente al gruppo
    void acceptInvitation_AlreadyMember() throws Exception {

        GroupInvitationAcceptRequest request =
                new GroupInvitationAcceptRequest(token);

        when(groupInvitationService.acceptInvitation(request))
                .thenThrow(new AlreadyGroupMemberException());

        mockMvc.perform(post("/group-invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "token": "%s"
                                }
                                """.formatted(token)))
                .andExpect(status().isConflict())
                .andExpect(content().string(
                        containsString("ALREADY_GROUP_MEMBER")
                ));

        verify(groupInvitationService).acceptInvitation(request);
    }


    @ParameterizedTest //Token mancante, vuoto o troppo corto serve per  eseguire lo stesso test quattro volte, passando quattro body differenti
    @ValueSource(strings = {
            "{}",
            "{\"token\": null}",
            "{\"token\": \"\"}",
            "{\"token\": \"abc\"}"
    })
    void acceptInvitation_InvalidBody(String body) throws Exception {

        mockMvc.perform(post("/group-invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        containsString("VALIDATION_ERROR")
                ))
                .andExpect(content().string(
                        containsString("token")
                ));

        verifyNoInteractions(groupInvitationService);
    }


    @Test //Token con più di 43 caratteri
    void acceptInvitation_TokenTooLong() throws Exception {

        String longToken = "a".repeat(44);

        mockMvc.perform(post("/group-invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "token": "%s"
                                }
                                """.formatted(longToken)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        containsString("VALIDATION_ERROR")
                ));

        verifyNoInteractions(groupInvitationService);
    }


    @Test //JSON malformato
    void acceptInvitation_InvalidJson() throws Exception {

        mockMvc.perform(post("/group-invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "token":
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        containsString("INVALID_JSON")
                ));

        verifyNoInteractions(groupInvitationService);
    }
}
