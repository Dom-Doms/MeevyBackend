package it.domrita.meevy.chat.service;

import it.domrita.meevy.auth.repository.CurrentUserIdProvider;
import it.domrita.meevy.group.exception.GroupNotFoundException;
import it.domrita.meevy.chat.dto.GroupMessageRequest;
import it.domrita.meevy.chat.dto.GroupMessageResponse;
import it.domrita.meevy.chat.exception.InvalidGroupMessageException;
import it.domrita.meevy.chat.model.GroupMessage;
import it.domrita.meevy.chat.repository.GroupMessageRepository;
import it.domrita.meevy.group.model.StudyGroup;
import it.domrita.meevy.group.repository.GroupMembershipRepository;
import it.domrita.meevy.group.repository.StudyGroupRepository;
import it.domrita.meevy.user.model.User;
import it.domrita.meevy.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupMessageServiceTest {

    @Mock private GroupMessageRepository groupMessageRepository;
    @Mock private GroupMembershipRepository groupMembershipRepository;
    @Mock private StudyGroupRepository studyGroupRepository;
    @Mock private UserRepository userRepository;
    @Mock private CurrentUserIdProvider currentUserIdProvider;
    @InjectMocks private GroupMessageService groupMessageService;

    private StudyGroup group;
    private User sender;
    private static final Instant NOW = Instant.parse("2026-11-17T09:00:00Z");

    @BeforeEach
    void setUp() {
        group = new StudyGroup("Analisi", null);
        ReflectionTestUtils.setField(group, "id", 10L);
        sender = new User("rita@example.test", "hash", "Rita", null);
        ReflectionTestUtils.setField(sender, "id", 2L);
    }

    @Test
    void sendMessage_SavesAuthenticatedUserAndTrimsText() {
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(2L);
        when(studyGroupRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(group));
        when(groupMembershipRepository.existsByGroup_IdAndUser_Id(10L, 2L)).thenReturn(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(sender));
        when(groupMessageRepository.saveAndFlush(any(GroupMessage.class))).thenAnswer(invocation -> {
            GroupMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", 50L);
            ReflectionTestUtils.setField(message, "createdAt", NOW);
            return message;
        });

        GroupMessageResponse response = groupMessageService.sendMessage(
                10L, new GroupMessageRequest("  Ciao gruppo!  ")
        );

        assertEquals(50L, response.id());
        assertEquals(10L, response.groupId());
        assertEquals(2L, response.senderUserId());
        assertEquals("Rita", response.senderFirstName());
        assertEquals("Ciao gruppo!", response.content());
        assertEquals(NOW, response.createdAt());

        // Non basta il controllo: il lock deve arrivare PRIMA della membership.
        InOrder inOrder = inOrder(studyGroupRepository, groupMembershipRepository);
        inOrder.verify(studyGroupRepository).findByIdForUpdate(10L);
        inOrder.verify(groupMembershipRepository).existsByGroup_IdAndUser_Id(10L, 2L);
    }

    @Test
    void sendMessage_RejectsBlankOrOversizedTextEvenWithoutController() {
        assertThrows(InvalidGroupMessageException.class,
                () -> groupMessageService.sendMessage(10L, new GroupMessageRequest(" \n ")));
        assertThrows(InvalidGroupMessageException.class,
                () -> groupMessageService.sendMessage(10L, new GroupMessageRequest("x".repeat(4001))));
        verifyNoInteractions(groupMessageRepository, currentUserIdProvider);
    }

    @Test
    void sendMessage_GroupMissing_DoesNotSave() {
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(2L);
        when(studyGroupRepository.findByIdForUpdate(10L)).thenReturn(Optional.empty());

        assertThrows(GroupNotFoundException.class,
                () -> groupMessageService.sendMessage(10L, new GroupMessageRequest("Ciao")));
        verifyNoInteractions(groupMembershipRepository, userRepository, groupMessageRepository);
    }

    @Test
    void sendMessage_NotMember_DoesNotSave() {
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(2L);
        when(studyGroupRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(group));
        when(groupMembershipRepository.existsByGroup_IdAndUser_Id(10L, 2L)).thenReturn(false);

        assertThrows(GroupNotFoundException.class,
                () -> groupMessageService.sendMessage(10L, new GroupMessageRequest("Ciao")));
        verifyNoInteractions(userRepository, groupMessageRepository);
    }

    @Test
    void sendMessage_MissingAuthenticatedUser_DoesNotSave() {
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(2L);
        when(studyGroupRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(group));
        when(groupMembershipRepository.existsByGroup_IdAndUser_Id(10L, 2L)).thenReturn(true);
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class,
                () -> groupMessageService.sendMessage(10L, new GroupMessageRequest("Ciao")));
        verifyNoInteractions(groupMessageRepository);
    }

    @Test
    void getMessages_ReturnsLatestFirst() {
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(2L);
        when(groupMembershipRepository.existsByGroup_IdAndUser_Id(10L, 2L)).thenReturn(true);
        GroupMessage message = message(51L, "Secondo");
        when(groupMessageRepository.findByGroup_IdOrderByIdDesc(eq(10L), any(Pageable.class)))
                .thenReturn(List.of(message));

        List<GroupMessageResponse> result = groupMessageService.getMessages(10L, null, 30);

        assertEquals(1, result.size());
        assertEquals(51L, result.get(0).id());
        assertEquals("Secondo", result.get(0).content());
        verify(groupMessageRepository).findByGroup_IdOrderByIdDesc(
                eq(10L), argThat(page -> page.getPageSize() == 30)
        );
    }

    @Test
    void getMessages_UsesBeforeIdCursor() {
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(2L);
        when(groupMembershipRepository.existsByGroup_IdAndUser_Id(10L, 2L)).thenReturn(true);
        when(groupMessageRepository.findByGroup_IdAndIdLessThanOrderByIdDesc(
                eq(10L), eq(50L), any(Pageable.class)
        )).thenReturn(List.of(message(49L, "Precedente")));

        List<GroupMessageResponse> result = groupMessageService.getMessages(10L, 50L, 20);

        assertEquals(49L, result.get(0).id());
        verify(groupMessageRepository, never())
                .findByGroup_IdOrderByIdDesc(anyLong(), any(Pageable.class));
    }

    @Test
    void getMessages_ExternalUserCannotRead() {
        when(currentUserIdProvider.getCurrentUserId()).thenReturn(2L);
        when(groupMembershipRepository.existsByGroup_IdAndUser_Id(10L, 2L)).thenReturn(false);

        assertThrows(GroupNotFoundException.class,
                () -> groupMessageService.getMessages(10L, null, 30));
        verifyNoInteractions(groupMessageRepository);
    }

    @Test
    void getMessages_RejectsInvalidPagination() {
        assertThrows(InvalidGroupMessageException.class,
                () -> groupMessageService.getMessages(10L, null, 0));
        assertThrows(InvalidGroupMessageException.class,
                () -> groupMessageService.getMessages(10L, null, 101));
        assertThrows(InvalidGroupMessageException.class,
                () -> groupMessageService.getMessages(10L, -1L, 30));
        verifyNoInteractions(currentUserIdProvider, groupMessageRepository);
    }

    private GroupMessage message(Long id, String content) {
        GroupMessage message = new GroupMessage(group, sender, content);
        ReflectionTestUtils.setField(message, "id", id);
        ReflectionTestUtils.setField(message, "createdAt", NOW);
        return message;
    }
}
