package it.domrita.meevy.chat.controller;

import it.domrita.meevy.common.exception.GlobalExceptionHandler;
import it.domrita.meevy.group.exception.GroupNotFoundException;
import it.domrita.meevy.chat.dto.GroupMessageRequest;
import it.domrita.meevy.chat.dto.GroupMessageResponse;
import it.domrita.meevy.chat.exception.InvalidGroupMessageException;
import it.domrita.meevy.chat.service.GroupMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GroupMessageControllerTest {

    @Mock private GroupMessageService groupMessageService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new GroupMessageController(groupMessageService)
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void sendMessage_Returns201AndMessage() throws Exception {
        GroupMessageResponse response = response();
        when(groupMessageService.sendMessage(eq(10L), eq(new GroupMessageRequest("Ciao"))))
                .thenReturn(response);

        mockMvc.perform(post("/groups/10/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Ciao"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(55))
                .andExpect(jsonPath("$.groupId").value(10))
                .andExpect(jsonPath("$.senderUserId").value(2))
                .andExpect(jsonPath("$.content").value("Ciao"));
    }

    @Test
    void sendMessage_BlankContentReturns400() throws Exception {
        mockMvc.perform(post("/groups/10/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.content").exists());
        verifyNoInteractions(groupMessageService);
    }

    @Test
    void sendMessage_ContentTooLongReturns400() throws Exception {
        mockMvc.perform(post("/groups/10/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + "x".repeat(4001) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(groupMessageService);
    }

    @Test
    void sendMessage_NonMemberReturns404() throws Exception {
        when(groupMessageService.sendMessage(eq(10L), any(GroupMessageRequest.class)))
                .thenThrow(new GroupNotFoundException(10L));

        mockMvc.perform(post("/groups/10/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Ciao"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GROUP_NOT_FOUND"));
    }

    @Test
    void getMessages_DefaultLimitIs30() throws Exception {
        when(groupMessageService.getMessages(10L, null, 30))
                .thenReturn(List.of(response()));

        mockMvc.perform(get("/groups/10/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(55));
        verify(groupMessageService).getMessages(10L, null, 30);
    }

    @Test
    void getMessages_PassesCursorAndLimit() throws Exception {
        when(groupMessageService.getMessages(10L, 50L, 20))
                .thenReturn(List.of());

        mockMvc.perform(get("/groups/10/messages")
                        .param("beforeId", "50")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        verify(groupMessageService).getMessages(10L, 50L, 20);
    }

    @Test
    void getMessages_InvalidLimitReturns400() throws Exception {
        when(groupMessageService.getMessages(10L, null, 101))
                .thenThrow(new InvalidGroupMessageException("Limite non valido"));

        mockMvc.perform(get("/groups/10/messages").param("limit", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_GROUP_MESSAGE"));
    }

    private GroupMessageResponse response() {
        return new GroupMessageResponse(
                55L, 10L, 2L, "Rita", null, "Ciao",
                Instant.parse("2026-11-17T09:00:00Z")
        );
    }
}

