
package it.domrita.meevy.group.service;

import it.domrita.meevy.auth.repository.CurrentUserIdProvider;

import it.domrita.meevy.group.dto.*;
import it.domrita.meevy.group.exception.*;
import it.domrita.meevy.group.model.GroupMembership;
import it.domrita.meevy.group.model.StudyGroup;
import it.domrita.meevy.group.repository.GroupMembershipRepository;
import it.domrita.meevy.group.repository.StudyGroupRepository;
import it.domrita.meevy.group.role.GroupRole;

import it.domrita.meevy.user.model.User;
import it.domrita.meevy.user.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StudyGroupServiceTest {

    @Mock
    private StudyGroupRepository studyGroupRepository;

    @Mock
    private GroupMembershipRepository groupMembershipRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserIdProvider currentUserIdProvider;

    @InjectMocks
    private StudyGroupService studyGroupService;


    @Test //Creazione corretta del gruppo
    void createGroup_TestCreationGroupSuccess(){

        Long userId = 1L;
        Long groupId = 10L;

        User user = mock(User.class);

        StudyGroupRequest request = new StudyGroupRequest(
                "  Basi di Dati  ",
                "Preparazione esame"
        );

        Instant now = Instant.now();

        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(userId);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        //Simuliamo il salvataggio del gruppo
        when(studyGroupRepository.save(any(StudyGroup.class)))
                .thenAnswer(invocation -> {

                    StudyGroup group = invocation.getArgument(0);

                    ReflectionTestUtils.setField(group, "id", groupId);
                    ReflectionTestUtils.setField(group, "createdAt", now);

                    return group;
                });

        //Simuliamo il salvataggio dell'appartenenza
        when(groupMembershipRepository.save(any(GroupMembership.class)))
                .thenAnswer(invocation -> {

                    GroupMembership membership = invocation.getArgument(0);

                    ReflectionTestUtils.setField(membership, "id", 20L);
                    ReflectionTestUtils.setField(membership, "joinedAt", now);

                    return membership;
                });

        when(groupMembershipRepository.countByGroup_Id(groupId))
                .thenReturn(1L);

        StudyGroupResponse response = studyGroupService.createGroup(request);

        assertEquals(groupId, response.id());
        assertEquals("Basi di Dati", response.name());
        assertEquals("Preparazione esame", response.description());
        assertEquals(now, response.createdAt());
        assertEquals(GroupRole.OWNER, response.myRole());
        assertEquals(1L, response.memberCount());

        ArgumentCaptor<StudyGroup> groupCaptor =
                ArgumentCaptor.forClass(StudyGroup.class);

        ArgumentCaptor<GroupMembership> membershipCaptor =
                ArgumentCaptor.forClass(GroupMembership.class);

        verify(studyGroupRepository).save(groupCaptor.capture());
        verify(groupMembershipRepository).save(membershipCaptor.capture());

        StudyGroup savedGroup = groupCaptor.getValue();
        GroupMembership savedMembership = membershipCaptor.getValue();

        assertSame(savedGroup, savedMembership.getGroup());
        assertSame(user, savedMembership.getUser());
        assertEquals(GroupRole.OWNER, savedMembership.getRole());
    }


    @Test //Utente inesistente
    void createGroup_UserNotFound(){

        Long userId = 1L;

        StudyGroupRequest request = new StudyGroupRequest(
                "Basi di Dati",
                "Preparazione esame"
        );

        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(userId);

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThrows(
                BadCredentialsException.class,
                () -> studyGroupService.createGroup(request)
        );

        verifyNoInteractions(
                studyGroupRepository,
                groupMembershipRepository
        );
    }


    @Test //Recupero dei gruppi personali
    void getAllGroups_Test(){

        Long userId = 1L;

        StudyGroup group1 = createGroup(10L);
        StudyGroup group2 = createGroup(11L);

        group2.updateDetails(
                "Ingegneria del Software",
                "Progetto finale"
        );

        GroupMembership membership1 = createMembership(
                group1, userId, GroupRole.OWNER
        );

        GroupMembership membership2 = createMembership(
                group2, userId, GroupRole.MEMBER
        );

        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(userId);

        when(groupMembershipRepository.findAllByUser_IdOrderByIdAsc(userId))
                .thenReturn(List.of(membership1, membership2));

        when(groupMembershipRepository.countByGroup_Id(10L))
                .thenReturn(3L);

        when(groupMembershipRepository.countByGroup_Id(11L))
                .thenReturn(5L);

        List<StudyGroupResponse> response =
                studyGroupService.getAllGroups();

        assertEquals(2, response.size());

        assertEquals(10L, response.get(0).id());
        assertEquals("Basi di Dati", response.get(0).name());
        assertEquals(GroupRole.OWNER, response.get(0).myRole());
        assertEquals(3L, response.get(0).memberCount());

        assertEquals(11L, response.get(1).id());
        assertEquals("Ingegneria del Software", response.get(1).name());
        assertEquals(GroupRole.MEMBER, response.get(1).myRole());
        assertEquals(5L, response.get(1).memberCount());

        verify(groupMembershipRepository)
                .findAllByUser_IdOrderByIdAsc(userId);
    }


    @Test //Utente senza gruppi
    void getAllGroups_EmptyList(){

        Long userId = 1L;

        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(userId);

        when(groupMembershipRepository.findAllByUser_IdOrderByIdAsc(userId))
                .thenReturn(List.of());

        List<StudyGroupResponse> response =
                studyGroupService.getAllGroups();

        assertNotNull(response);
        assertTrue(response.isEmpty());

        verify(groupMembershipRepository, never())
                .countByGroup_Id(anyLong());
    }


    @Test //Recupero del gruppo tramite id
    void getGroupById_Test(){

        Long userId = 1L;
        Long groupId = 10L;

        StudyGroup group = createGroup(groupId);

        GroupMembership membership = createMembership(
                group, userId, GroupRole.MODERATOR
        );

        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(userId);

        when(groupMembershipRepository.findByGroup_IdAndUser_Id(groupId, userId))
                .thenReturn(Optional.of(membership));

        when(groupMembershipRepository.countByGroup_Id(groupId))
                .thenReturn(4L);

        StudyGroupResponse response =
                studyGroupService.getGroupById(groupId);

        assertEquals(groupId, response.id());
        assertEquals(group.getName(), response.name());
        assertEquals(group.getDescription(), response.description());
        assertEquals(group.getCreatedAt(), response.createdAt());
        assertEquals(GroupRole.MODERATOR, response.myRole());
        assertEquals(4L, response.memberCount());

        verify(groupMembershipRepository)
                .findByGroup_IdAndUser_Id(groupId, userId);
    }


    @Test //Gruppo inesistente o utente esterno
    void getGroupById_GroupNotFound(){

        Long userId = 1L;
        Long groupId = 10L;

        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(userId);

        when(groupMembershipRepository.findByGroup_IdAndUser_Id(groupId, userId))
                .thenReturn(Optional.empty());

        assertThrows(
                GroupNotFoundException.class,
                () -> studyGroupService.getGroupById(groupId)
        );

        verify(groupMembershipRepository, never())
                .countByGroup_Id(anyLong());
    }


    @Test //Modifica corretta del gruppo
    void updateGroup_Test(){

        Long userId = 1L;
        Long groupId = 10L;

        StudyGroup group = createGroup(groupId);

        GroupMembership membership = createMembership(
                group, userId, GroupRole.OWNER
        );

        StudyGroupRequest request = new StudyGroupRequest(
                "  Basi di Dati 2  ",
                "Nuova descrizione"
        );

        prepareLockedGroup(group, membership);

        when(groupMembershipRepository.countByGroup_Id(groupId))
                .thenReturn(1L);

        StudyGroupResponse response =
                studyGroupService.updateGroup(groupId, request);

        assertEquals(groupId, response.id());
        assertEquals("Basi di Dati 2", response.name());
        assertEquals("Nuova descrizione", response.description());
        assertEquals(GroupRole.OWNER, response.myRole());
        assertEquals(1L, response.memberCount());

        assertEquals("Basi di Dati 2", group.getName());
        assertEquals("Nuova descrizione", group.getDescription());

        //Verifichiamo che il lock venga acquisito prima del controllo dei permessi
        InOrder inOrder = inOrder(
                studyGroupRepository,
                groupMembershipRepository
        );

        inOrder.verify(studyGroupRepository)
                .findByIdForUpdate(groupId);

        inOrder.verify(groupMembershipRepository)
                .findByGroup_IdAndUser_Id(groupId, userId);
    }


    @Test //Modifica da parte di un MEMBER
    void updateGroup_MemberAccessDenied(){

        Long userId = 1L;
        Long groupId = 10L;

        StudyGroup group = createGroup(groupId);

        GroupMembership membership = createMembership(
                group, userId, GroupRole.MEMBER
        );

        StudyGroupRequest request = new StudyGroupRequest(
                "Nuovo nome",
                "Nuova descrizione"
        );

        prepareLockedGroup(group, membership);

        assertThrows(
                GroupAccessDeniedException.class,
                () -> studyGroupService.updateGroup(groupId, request)
        );

        //Il gruppo non deve essere modificato
        assertEquals("Basi di Dati", group.getName());
        assertEquals("Preparazione esame", group.getDescription());

        verify(groupMembershipRepository, never())
                .countByGroup_Id(anyLong());
    }


    @Test //Modifica da parte di un MODERATOR
    void updateGroup_ModeratorAccessDenied(){

        StudyGroup group = createGroup(10L);

        GroupMembership membership = createMembership(
                group, 1L, GroupRole.MODERATOR
        );

        StudyGroupRequest request = new StudyGroupRequest(
                "Nuovo nome",
                null
        );

        prepareLockedGroup(group, membership);

        assertThrows(
                GroupAccessDeniedException.class,
                () -> studyGroupService.updateGroup(10L, request)
        );

        assertEquals("Basi di Dati", group.getName());
        assertEquals("Preparazione esame", group.getDescription());
    }


    @Test //Modifica di un gruppo inesistente
    void updateGroup_GroupNotFound(){

        Long userId = 1L;
        Long groupId = 10L;

        StudyGroupRequest request = new StudyGroupRequest(
                "Nuovo nome",
                "Nuova descrizione"
        );

        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(userId);

        when(studyGroupRepository.findByIdForUpdate(groupId))
                .thenReturn(Optional.empty());

        assertThrows(
                GroupNotFoundException.class,
                () -> studyGroupService.updateGroup(groupId, request)
        );

        verifyNoInteractions(groupMembershipRepository);
    }


    @Test //Modifica da parte di un utente esterno
    void updateGroup_ExternalUser(){

        Long userId = 1L;
        Long groupId = 10L;

        StudyGroup group = createGroup(groupId);

        StudyGroupRequest request = new StudyGroupRequest(
                "Nuovo nome",
                null
        );

        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(userId);

        when(studyGroupRepository.findByIdForUpdate(groupId))
                .thenReturn(Optional.of(group));

        when(groupMembershipRepository.findByGroup_IdAndUser_Id(groupId, userId))
                .thenReturn(Optional.empty());

        assertThrows(
                GroupNotFoundException.class,
                () -> studyGroupService.updateGroup(groupId, request)
        );

        assertEquals("Basi di Dati", group.getName());
    }


    @Test //Eliminazione corretta del gruppo
    void deleteGroup_Test(){

        Long userId = 1L;
        Long groupId = 10L;

        StudyGroup group = createGroup(groupId);

        GroupMembership membership = createMembership(
                group, userId, GroupRole.OWNER
        );

        prepareLockedGroup(group, membership);

        studyGroupService.deleteGroup(groupId);

        verify(studyGroupRepository).findByIdForUpdate(groupId);
        verify(studyGroupRepository).delete(group);
    }


    @Test //Eliminazione senza i permessi necessari
    void deleteGroup_AccessDenied(){

        StudyGroup group = createGroup(10L);

        GroupMembership membership = createMembership(
                group, 1L, GroupRole.MODERATOR
        );

        prepareLockedGroup(group, membership);

        assertThrows(
                GroupAccessDeniedException.class,
                () -> studyGroupService.deleteGroup(10L)
        );

        verify(studyGroupRepository, never())
                .delete(any(StudyGroup.class));
    }


    @Test //Recupero dei membri del gruppo
    void getMembers_Test(){

        Long groupId = 10L;
        Long ownerId = 1L;
        Long memberId = 2L;

        StudyGroup group = createGroup(groupId);

        GroupMembership owner = createMembership(
                group, ownerId, GroupRole.OWNER
        );

        GroupMembership member = createMembership(
                group, memberId, GroupRole.MEMBER
        );

        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(ownerId);

        when(groupMembershipRepository.findByGroup_IdAndUser_Id(groupId, ownerId))
                .thenReturn(Optional.of(owner));

        when(groupMembershipRepository.findAllByGroup_IdOrderByJoinedAtAscIdAsc(groupId))
                .thenReturn(List.of(owner, member));

        List<GroupMemberResponse> response =
                studyGroupService.getMembers(groupId);

        assertEquals(2, response.size());

        assertEquals(ownerId, response.get(0).userId());
        assertEquals("Utente", response.get(0).firstName());
        assertEquals("Test", response.get(0).lastName());
        assertEquals(GroupRole.OWNER, response.get(0).role());
        assertEquals(owner.getJoinedAt(), response.get(0).joinedAt());

        assertEquals(memberId, response.get(1).userId());
        assertEquals(GroupRole.MEMBER, response.get(1).role());
        assertEquals(member.getJoinedAt(), response.get(1).joinedAt());

        verify(groupMembershipRepository)
                .findAllByGroup_IdOrderByJoinedAtAscIdAsc(groupId);
    }


    @Test //Utente esterno che prova a visualizzare i membri
    void getMembers_ExternalUser(){

        Long userId = 1L;
        Long groupId = 10L;

        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(userId);

        when(groupMembershipRepository.findByGroup_IdAndUser_Id(groupId, userId))
                .thenReturn(Optional.empty());

        assertThrows(
                GroupNotFoundException.class,
                () -> studyGroupService.getMembers(groupId)
        );

        verify(groupMembershipRepository, never())
                .findAllByGroup_IdOrderByJoinedAtAscIdAsc(anyLong());
    }


    @Test //Promozione di un membro a moderatore
    void updateMemberRole_PromoteMember(){

        Long groupId = 10L;
        Long ownerId = 1L;
        Long memberId = 2L;

        StudyGroup group = createGroup(groupId);

        GroupMembership owner = createMembership(
                group, ownerId, GroupRole.OWNER
        );

        GroupMembership member = createMembership(
                group, memberId, GroupRole.MEMBER
        );

        GroupRoleRequest request =
                new GroupRoleRequest(GroupRole.MODERATOR);

        prepareLockedGroup(group, owner);

        when(groupMembershipRepository.findByGroup_IdAndUser_Id(groupId, memberId))
                .thenReturn(Optional.of(member));

        GroupMemberResponse response =
                studyGroupService.updateMemberRole(groupId, memberId, request);

        assertEquals(memberId, response.userId());
        assertEquals(GroupRole.MODERATOR, response.role());
        assertEquals(GroupRole.MODERATOR, member.getRole());
        assertEquals(GroupRole.OWNER, owner.getRole());
    }


    @Test //Declassamento di un moderatore
    void updateMemberRole_DemoteModerator(){

        Long groupId = 10L;
        Long ownerId = 1L;
        Long moderatorId = 2L;

        StudyGroup group = createGroup(groupId);

        GroupMembership owner = createMembership(
                group, ownerId, GroupRole.OWNER
        );

        GroupMembership moderator = createMembership(
                group, moderatorId, GroupRole.MODERATOR
        );

        GroupRoleRequest request =
                new GroupRoleRequest(GroupRole.MEMBER);

        prepareLockedGroup(group, owner);

        when(groupMembershipRepository.findByGroup_IdAndUser_Id(groupId, moderatorId))
                .thenReturn(Optional.of(moderator));

        GroupMemberResponse response =
                studyGroupService.updateMemberRole(groupId, moderatorId, request);

        assertEquals(moderatorId, response.userId());
        assertEquals(GroupRole.MEMBER, response.role());
        assertEquals(GroupRole.MEMBER, moderator.getRole());
    }


    @Test //Tentativo di assegnare direttamente OWNER
    void updateMemberRole_InvalidRole(){

        Long groupId = 10L;
        Long ownerId = 1L;
        Long memberId = 2L;

        StudyGroup group = createGroup(groupId);

        GroupMembership owner = createMembership(
                group, ownerId, GroupRole.OWNER
        );

        GroupRoleRequest request =
                new GroupRoleRequest(GroupRole.OWNER);

        prepareLockedGroup(group, owner);

        assertThrows(
                InvalidGroupRoleException.class,
                () -> studyGroupService.updateMemberRole(groupId, memberId, request)
        );

        verify(groupMembershipRepository, never())
                .findByGroup_IdAndUser_Id(groupId, memberId);

        assertEquals(GroupRole.OWNER, owner.getRole());
    }


    @Test //Tentativo di modificare il ruolo dell'OWNER
    void updateMemberRole_CannotChangeOwner(){

        Long groupId = 10L;
        Long ownerId = 1L;

        StudyGroup group = createGroup(groupId);

        GroupMembership owner = createMembership(
                group, ownerId, GroupRole.OWNER
        );

        GroupRoleRequest request =
                new GroupRoleRequest(GroupRole.MEMBER);

        prepareLockedGroup(group, owner);

        assertThrows(
                InvalidGroupRoleException.class,
                () -> studyGroupService.updateMemberRole(groupId, ownerId, request)
        );

        assertEquals(GroupRole.OWNER, owner.getRole());
    }


    @Test //Modifica del ruolo senza permessi
    void updateMemberRole_AccessDenied(){

        Long groupId = 10L;
        Long moderatorId = 1L;
        Long memberId = 2L;

        StudyGroup group = createGroup(groupId);

        GroupMembership moderator = createMembership(
                group, moderatorId, GroupRole.MODERATOR
        );

        GroupRoleRequest request =
                new GroupRoleRequest(GroupRole.MODERATOR);

        prepareLockedGroup(group, moderator);

        assertThrows(
                GroupAccessDeniedException.class,
                () -> studyGroupService.updateMemberRole(groupId, memberId, request)
        );

        verify(groupMembershipRepository, never())
                .findByGroup_IdAndUser_Id(groupId, memberId);
    }


    @Test //Modifica del ruolo di un utente non iscritto
    void updateMemberRole_MemberNotFound(){

        Long groupId = 10L;
        Long ownerId = 1L;
        Long memberId = 2L;

        StudyGroup group = createGroup(groupId);

        GroupMembership owner = createMembership(
                group, ownerId, GroupRole.OWNER
        );

        GroupRoleRequest request =
                new GroupRoleRequest(GroupRole.MODERATOR);

        prepareLockedGroup(group, owner);

        when(groupMembershipRepository.findByGroup_IdAndUser_Id(groupId, memberId))
                .thenReturn(Optional.empty());

        assertThrows(
                GroupMemberNotFoundException.class,
                () -> studyGroupService.updateMemberRole(groupId, memberId, request)
        );

        assertEquals(GroupRole.OWNER, owner.getRole());
    }


    @Test //OWNER rimuove un membro
    void removeMember_OwnerRemovesMember(){

        Long groupId = 10L;
        Long ownerId = 1L;
        Long memberId = 2L;

        StudyGroup group = createGroup(groupId);

        GroupMembership owner = createMembership(
                group, ownerId, GroupRole.OWNER
        );

        GroupMembership member = createMembership(
                group, memberId, GroupRole.MEMBER
        );

        prepareLockedGroup(group, owner);

        when(groupMembershipRepository.findByGroup_IdAndUser_Id(groupId, memberId))
                .thenReturn(Optional.of(member));

        studyGroupService.removeMember(groupId, memberId);

        verify(groupMembershipRepository).delete(member);
        verify(groupMembershipRepository, never()).delete(owner);
    }


    @Test //OWNER rimuove un moderatore
    void removeMember_OwnerRemovesModerator(){

        Long groupId = 10L;
        Long ownerId = 1L;
        Long moderatorId = 2L;

        StudyGroup group = createGroup(groupId);

        GroupMembership owner = createMembership(
                group, ownerId, GroupRole.OWNER
        );

        GroupMembership moderator = createMembership(
                group, moderatorId, GroupRole.MODERATOR
        );

        prepareLockedGroup(group, owner);

        when(groupMembershipRepository.findByGroup_IdAndUser_Id(groupId, moderatorId))
                .thenReturn(Optional.of(moderator));

        studyGroupService.removeMember(groupId, moderatorId);

        verify(groupMembershipRepository).delete(moderator);
    }


    @Test //MODERATOR rimuove un MEMBER
    void removeMember_ModeratorRemovesMember(){

        Long groupId = 10L;
        Long moderatorId = 1L;
        Long memberId = 2L;

        StudyGroup group = createGroup(groupId);

        GroupMembership moderator = createMembership(
                group, moderatorId, GroupRole.MODERATOR
        );

        GroupMembership member = createMembership(
                group, memberId, GroupRole.MEMBER
        );

        prepareLockedGroup(group, moderator);

        when(groupMembershipRepository.findByGroup_IdAndUser_Id(groupId, memberId))
                .thenReturn(Optional.of(member));

        studyGroupService.removeMember(groupId, memberId);

        verify(groupMembershipRepository).delete(member);
    }


    @Test //MEMBER non può rimuovere altri membri
    void removeMember_MemberAccessDenied(){

        Long groupId = 10L;
        Long userId = 1L;
        Long targetId = 2L;

        StudyGroup group = createGroup(groupId);

        GroupMembership member = createMembership(
                group, userId, GroupRole.MEMBER
        );

        prepareLockedGroup(group, member);

        assertThrows(
                GroupAccessDeniedException.class,
                () -> studyGroupService.removeMember(groupId, targetId)
        );

        verify(groupMembershipRepository, never())
                .delete(any(GroupMembership.class));

        verify(groupMembershipRepository, never())
                .findByGroup_IdAndUser_Id(groupId, targetId);
    }


    @Test //MODERATOR non può rimuovere un altro MODERATOR
    void removeMember_ModeratorCannotRemoveModerator(){

        Long groupId = 10L;
        Long userId = 1L;
        Long targetId = 2L;

        StudyGroup group = createGroup(groupId);

        GroupMembership moderator1 = createMembership(
                group, userId, GroupRole.MODERATOR
        );

        GroupMembership moderator2 = createMembership(
                group, targetId, GroupRole.MODERATOR
        );

        prepareLockedGroup(group, moderator1);

        when(groupMembershipRepository.findByGroup_IdAndUser_Id(groupId, targetId))
                .thenReturn(Optional.of(moderator2));

        assertThrows(
                GroupAccessDeniedException.class,
                () -> studyGroupService.removeMember(groupId, targetId)
        );

        verify(groupMembershipRepository, never())
                .delete(any(GroupMembership.class));
    }


    @Test //Tentativo di rimuovere l'OWNER
    void removeMember_CannotRemoveOwner(){

        Long groupId = 10L;
        Long moderatorId = 1L;
        Long ownerId = 2L;

        StudyGroup group = createGroup(groupId);

        GroupMembership moderator = createMembership(
                group, moderatorId, GroupRole.MODERATOR
        );

        GroupMembership owner = createMembership(
                group, ownerId, GroupRole.OWNER
        );

        prepareLockedGroup(group, moderator);

        when(groupMembershipRepository.findByGroup_IdAndUser_Id(groupId, ownerId))
                .thenReturn(Optional.of(owner));

        assertThrows(
                GroupAccessDeniedException.class,
                () -> studyGroupService.removeMember(groupId, ownerId)
        );

        verify(groupMembershipRepository, never())
                .delete(any(GroupMembership.class));

        assertEquals(GroupRole.OWNER, owner.getRole());
    }


    @Test //Tentativo di espellere se stessi
    void removeMember_CannotRemoveSelf(){

        Long groupId = 10L;
        Long userId = 1L;

        StudyGroup group = createGroup(groupId);

        GroupMembership owner = createMembership(
                group, userId, GroupRole.OWNER
        );

        prepareLockedGroup(group, owner);

        assertThrows(
                GroupAccessDeniedException.class,
                () -> studyGroupService.removeMember(groupId, userId)
        );

        verify(groupMembershipRepository, never())
                .delete(any(GroupMembership.class));
    }


    @Test //Rimozione di un utente non appartenente al gruppo
    void removeMember_MemberNotFound(){

        Long groupId = 10L;
        Long ownerId = 1L;
        Long memberId = 2L;

        StudyGroup group = createGroup(groupId);

        GroupMembership owner = createMembership(
                group, ownerId, GroupRole.OWNER
        );

        prepareLockedGroup(group, owner);

        when(groupMembershipRepository.findByGroup_IdAndUser_Id(groupId, memberId))
                .thenReturn(Optional.empty());

        assertThrows(
                GroupMemberNotFoundException.class,
                () -> studyGroupService.removeMember(groupId, memberId)
        );

        verify(groupMembershipRepository, never())
                .delete(any(GroupMembership.class));
    }


    @Test //MEMBER abbandona il gruppo
    void leaveGroup_MemberSuccess(){

        Long groupId = 10L;
        Long userId = 1L;

        StudyGroup group = createGroup(groupId);

        GroupMembership member = createMembership(
                group, userId, GroupRole.MEMBER
        );

        prepareLockedGroup(group, member);

        studyGroupService.leaveGroup(groupId);

        verify(groupMembershipRepository).delete(member);
    }


    @Test //MODERATOR abbandona il gruppo
    void leaveGroup_ModeratorSuccess(){

        Long groupId = 10L;
        Long userId = 1L;

        StudyGroup group = createGroup(groupId);

        GroupMembership moderator = createMembership(
                group, userId, GroupRole.MODERATOR
        );

        prepareLockedGroup(group, moderator);

        studyGroupService.leaveGroup(groupId);

        verify(groupMembershipRepository).delete(moderator);
    }


    @Test //OWNER non può abbandonare il gruppo
    void leaveGroup_OwnerCannotLeave(){

        Long groupId = 10L;
        Long userId = 1L;

        StudyGroup group = createGroup(groupId);

        GroupMembership owner = createMembership(
                group, userId, GroupRole.OWNER
        );

        prepareLockedGroup(group, owner);

        assertThrows(
                OwnerCannotLeaveException.class,
                () -> studyGroupService.leaveGroup(groupId)
        );

        verify(groupMembershipRepository, never())
                .delete(any(GroupMembership.class));
    }


    @Test //Utente esterno che prova ad abbandonare il gruppo
    void leaveGroup_ExternalUser(){

        Long groupId = 10L;
        Long userId = 1L;

        StudyGroup group = createGroup(groupId);

        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(userId);

        when(studyGroupRepository.findByIdForUpdate(groupId))
                .thenReturn(Optional.of(group));

        when(groupMembershipRepository.findByGroup_IdAndUser_Id(groupId, userId))
                .thenReturn(Optional.empty());

        assertThrows(
                GroupNotFoundException.class,
                () -> studyGroupService.leaveGroup(groupId)
        );

        verify(groupMembershipRepository, never())
                .delete(any(GroupMembership.class));
    }


    @Test //Trasferimento corretto della proprietà
    void transferOwnership_Test(){

        Long groupId = 10L;
        Long ownerId = 1L;
        Long newOwnerId = 2L;

        StudyGroup group = createGroup(groupId);

        GroupMembership currentOwner = createMembership(
                group, ownerId, GroupRole.OWNER
        );

        GroupMembership newOwner = createMembership(
                group, newOwnerId, GroupRole.MODERATOR
        );

        GroupOwnerTransferRequest request =
                new GroupOwnerTransferRequest(newOwnerId);

        prepareLockedGroup(group, currentOwner);

        when(groupMembershipRepository.findByGroup_IdAndUser_Id(groupId, newOwnerId))
                .thenReturn(Optional.of(newOwner));

        //Al momento del flush il vecchio OWNER deve essere già MEMBER
        doAnswer(invocation -> {

            assertEquals(GroupRole.MEMBER, currentOwner.getRole());
            assertEquals(GroupRole.MODERATOR, newOwner.getRole());

            return null;

        }).when(groupMembershipRepository).flush();

        studyGroupService.transferOwnership(groupId, request);

        assertEquals(GroupRole.MEMBER, currentOwner.getRole());
        assertEquals(GroupRole.OWNER, newOwner.getRole());

        //Verifichiamo l'ordine delle operazioni
        InOrder inOrder = inOrder(
                studyGroupRepository,
                groupMembershipRepository
        );

        inOrder.verify(studyGroupRepository)
                .findByIdForUpdate(groupId);

        inOrder.verify(groupMembershipRepository)
                .findByGroup_IdAndUser_Id(groupId, ownerId);

        inOrder.verify(groupMembershipRepository)
                .findByGroup_IdAndUser_Id(groupId, newOwnerId);

        inOrder.verify(groupMembershipRepository).flush();
    }


    @Test //Trasferimento della proprietà a un MEMBER
    void transferOwnership_ToMember(){

        Long groupId = 10L;
        Long ownerId = 1L;
        Long memberId = 2L;

        StudyGroup group = createGroup(groupId);

        GroupMembership owner = createMembership(
                group, ownerId, GroupRole.OWNER
        );

        GroupMembership member = createMembership(
                group, memberId, GroupRole.MEMBER
        );

        GroupOwnerTransferRequest request =
                new GroupOwnerTransferRequest(memberId);

        prepareLockedGroup(group, owner);

        when(groupMembershipRepository.findByGroup_IdAndUser_Id(groupId, memberId))
                .thenReturn(Optional.of(member));

        studyGroupService.transferOwnership(groupId, request);

        assertEquals(GroupRole.MEMBER, owner.getRole());
        assertEquals(GroupRole.OWNER, member.getRole());

        verify(groupMembershipRepository).flush();
    }


    @Test //Tentativo di trasferire la proprietà a se stessi
    void transferOwnership_ToSelf(){

        Long groupId = 10L;
        Long ownerId = 1L;

        StudyGroup group = createGroup(groupId);

        GroupMembership owner = createMembership(
                group, ownerId, GroupRole.OWNER
        );

        GroupOwnerTransferRequest request =
                new GroupOwnerTransferRequest(ownerId);

        prepareLockedGroup(group, owner);

        assertThrows(
                InvalidOwnershipTransferException.class,
                () -> studyGroupService.transferOwnership(groupId, request)
        );

        assertEquals(GroupRole.OWNER, owner.getRole());

        verify(groupMembershipRepository, never()).flush();
    }


    @Test //Trasferimento della proprietà senza permessi
    void transferOwnership_AccessDenied(){

        Long groupId = 10L;
        Long userId = 1L;
        Long targetId = 2L;

        StudyGroup group = createGroup(groupId);

        GroupMembership moderator = createMembership(
                group, userId, GroupRole.MODERATOR
        );

        GroupOwnerTransferRequest request =
                new GroupOwnerTransferRequest(targetId);

        prepareLockedGroup(group, moderator);

        assertThrows(
                GroupAccessDeniedException.class,
                () -> studyGroupService.transferOwnership(groupId, request)
        );

        assertEquals(GroupRole.MODERATOR, moderator.getRole());

        verify(groupMembershipRepository, never()).flush();

        verify(groupMembershipRepository, never())
                .findByGroup_IdAndUser_Id(groupId, targetId);
    }


    @Test //Trasferimento della proprietà a un utente non iscritto
    void transferOwnership_MemberNotFound(){

        Long groupId = 10L;
        Long ownerId = 1L;
        Long targetId = 2L;

        StudyGroup group = createGroup(groupId);

        GroupMembership owner = createMembership(
                group, ownerId, GroupRole.OWNER
        );

        GroupOwnerTransferRequest request =
                new GroupOwnerTransferRequest(targetId);

        prepareLockedGroup(group, owner);

        when(groupMembershipRepository.findByGroup_IdAndUser_Id(groupId, targetId))
                .thenReturn(Optional.empty());

        assertThrows(
                GroupMemberNotFoundException.class,
                () -> studyGroupService.transferOwnership(groupId, request)
        );

        assertEquals(GroupRole.OWNER, owner.getRole());

        verify(groupMembershipRepository, never()).flush();
    }


    @Test //Errore durante il flush del vecchio OWNER
    void transferOwnership_FlushError(){

        Long groupId = 10L;
        Long ownerId = 1L;
        Long newOwnerId = 2L;

        StudyGroup group = createGroup(groupId);

        GroupMembership owner = createMembership(
                group, ownerId, GroupRole.OWNER
        );

        GroupMembership newOwner = createMembership(
                group, newOwnerId, GroupRole.MODERATOR
        );

        GroupOwnerTransferRequest request =
                new GroupOwnerTransferRequest(newOwnerId);

        prepareLockedGroup(group, owner);

        when(groupMembershipRepository.findByGroup_IdAndUser_Id(groupId, newOwnerId))
                .thenReturn(Optional.of(newOwner));

        doThrow(new IllegalStateException("Errore durante il flush"))
                .when(groupMembershipRepository).flush();

        assertThrows(
                IllegalStateException.class,
                () -> studyGroupService.transferOwnership(groupId, request)
        );

        //Il nuovo proprietario non deve essere promosso
        assertEquals(GroupRole.MODERATOR, newOwner.getRole());

        verify(groupMembershipRepository).flush();
    }


    //METODI DI SUPPORTO

    //Crea un gruppo con id e data già impostati
    private StudyGroup createGroup(Long groupId){

        StudyGroup group = new StudyGroup(
                "Basi di Dati",
                "Preparazione esame"
        );

        ReflectionTestUtils.setField(group, "id", groupId);

        ReflectionTestUtils.setField(
                group,
                "createdAt",
                Instant.parse("2026-11-12T10:00:00Z")
        );

        return group;
    }


    //Crea l'appartenenza di un utente al gruppo
    private GroupMembership createMembership(
            StudyGroup group,
            Long userId,
            GroupRole role
    ){

        User user = new User(
                "utente" + userId + "@example.com",
                "passwordHash",
                "Utente",
                "Test"
        );

        ReflectionTestUtils.setField(user, "id", userId);

        GroupMembership membership =
                new GroupMembership(group, user, role);

        ReflectionTestUtils.setField(
                membership,
                "joinedAt",
                Instant.parse("2026-11-12T10:00:00Z")
        );

        return membership;
    }


    //Simula il lock del gruppo e l'appartenenza dell'utente corrente
    private void prepareLockedGroup(
            StudyGroup group,
            GroupMembership membership
    ){

        Long groupId = group.getId();
        Long userId = membership.getUser().getId();

        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(userId);

        when(studyGroupRepository.findByIdForUpdate(groupId))
                .thenReturn(Optional.of(group));

        when(groupMembershipRepository.findByGroup_IdAndUser_Id(groupId, userId))
                .thenReturn(Optional.of(membership));
    }
}
