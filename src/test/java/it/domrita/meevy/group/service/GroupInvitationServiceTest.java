
package it.domrita.meevy.group.service;

import it.domrita.meevy.auth.repository.CurrentUserIdProvider;
import it.domrita.meevy.common.security.OpaqueTokenGenerator;

import it.domrita.meevy.group.dto.*;
import it.domrita.meevy.group.exception.*;
import it.domrita.meevy.group.model.GroupInvitation;
import it.domrita.meevy.group.model.GroupMembership;
import it.domrita.meevy.group.model.StudyGroup;
import it.domrita.meevy.group.repository.GroupInvitationRepository;
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

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GroupInvitationServiceTest {

    @Mock
    private GroupInvitationRepository groupInvitationRepository;

    @Mock
    private GroupMembershipRepository groupMembershipRepository;

    @Mock
    private StudyGroupRepository studyGroupRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserIdProvider currentUserIdProvider;

    @InjectMocks
    private GroupInvitationService groupInvitationService;

    private final Long groupId = 10L;

    private final Long ownerId = 1L;

    private final Long memberId = 2L;

    private final String token = "a".repeat(43);

    private final String tokenHash =
            OpaqueTokenGenerator.hash(token);


    @Test //Creazione corretta dell'invito da parte dell'OWNER
    void createInvitation_OwnerSuccess(){

        StudyGroup group = createGroup();

        User owner = createUser(ownerId);

        GroupMembership membership = new GroupMembership(
                group,
                owner,
                GroupRole.OWNER
        );

        prepareCreation(group, membership);

        when(groupInvitationRepository.save(any(GroupInvitation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GroupInvitationResponse response =
                groupInvitationService.createInvitation(groupId);

        assertEquals(groupId, response.groupId());
        assertNotNull(response.token());
        assertEquals(43, response.token().length());
        assertTrue(response.token().matches("[A-Za-z0-9_-]{43}"));

        ArgumentCaptor<GroupInvitation> captor =
                ArgumentCaptor.forClass(GroupInvitation.class);

        verify(groupInvitationRepository).save(captor.capture());

        GroupInvitation savedInvitation = captor.getValue();

        assertSame(group, savedInvitation.getGroup());
        assertSame(owner, savedInvitation.getCreatedBy());

        //Verifichiamo che venga salvato l'hash e non il token originale
        assertEquals(
                OpaqueTokenGenerator.hash(response.token()),
                savedInvitation.getTokenHash()
        );

        assertNotEquals(
                response.token(),
                savedInvitation.getTokenHash()
        );

        assertEquals(64, savedInvitation.getTokenHash().length());

        assertEquals(
                Duration.ofHours(24),
                Duration.between(
                        savedInvitation.getCreatedAt(),
                        savedInvitation.getExpiresAt()
                )
        );

        assertEquals(
                savedInvitation.getExpiresAt(),
                response.expiresAt()
        );

        assertFalse(savedInvitation.isUsed());
        assertNull(savedInvitation.getUsedBy());

        //Verifichiamo che il lock preceda il controllo dei permessi
        InOrder inOrder = inOrder(
                studyGroupRepository,
                groupMembershipRepository,
                groupInvitationRepository
        );

        inOrder.verify(studyGroupRepository)
                .findByIdForUpdate(groupId);

        inOrder.verify(groupMembershipRepository)
                .findByGroup_IdAndUser_Id(groupId, ownerId);

        inOrder.verify(groupInvitationRepository)
                .save(savedInvitation);
    }


    @Test //Creazione dell'invito da parte di un MODERATOR
    void createInvitation_ModeratorSuccess(){

        StudyGroup group = createGroup();

        User moderator = createUser(memberId);

        GroupMembership membership = new GroupMembership(
                group,
                moderator,
                GroupRole.MODERATOR
        );

        prepareCreation(group, membership);

        GroupInvitationResponse response =
                groupInvitationService.createInvitation(groupId);

        assertEquals(groupId, response.groupId());
        assertNotNull(response.token());

        ArgumentCaptor<GroupInvitation> captor =
                ArgumentCaptor.forClass(GroupInvitation.class);

        verify(groupInvitationRepository).save(captor.capture());

        assertSame(moderator, captor.getValue().getCreatedBy());
        assertFalse(captor.getValue().isUsed());
    }


    @Test //Un MEMBER non può creare inviti
    void createInvitation_MemberAccessDenied(){

        StudyGroup group = createGroup();

        User member = createUser(memberId);

        GroupMembership membership = new GroupMembership(
                group,
                member,
                GroupRole.MEMBER
        );

        prepareCreation(group, membership);

        assertThrows(
                GroupAccessDeniedException.class,
                () -> groupInvitationService.createInvitation(groupId)
        );

        verifyNoInteractions(groupInvitationRepository);
    }


    @Test //Creazione dell'invito per un gruppo inesistente
    void createInvitation_GroupNotFound(){

        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(ownerId);

        when(studyGroupRepository.findByIdForUpdate(groupId))
                .thenReturn(Optional.empty());

        assertThrows(
                GroupNotFoundException.class,
                () -> groupInvitationService.createInvitation(groupId)
        );

        verifyNoInteractions(
                groupInvitationRepository,
                groupMembershipRepository
        );
    }


    @Test //Utente esterno che prova a creare un invito
    void createInvitation_ExternalUser(){

        StudyGroup group = createGroup();

        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(memberId);

        when(studyGroupRepository.findByIdForUpdate(groupId))
                .thenReturn(Optional.of(group));

        when(groupMembershipRepository.findByGroup_IdAndUser_Id(
                groupId, memberId
        )).thenReturn(Optional.empty());

        assertThrows(
                GroupNotFoundException.class,
                () -> groupInvitationService.createInvitation(groupId)
        );

        verifyNoInteractions(groupInvitationRepository);
    }


    @Test //Accettazione corretta dell'invito
    void acceptInvitation_Test(){

        StudyGroup group = createGroup();

        User member = createUser(memberId);

        GroupInvitation invitation = createInvitation(group);

        GroupInvitationAcceptRequest request =
                new GroupInvitationAcceptRequest(token);

        prepareAcceptance(group, invitation);

        when(groupMembershipRepository.existsByGroup_IdAndUser_Id(
                groupId, memberId
        )).thenReturn(false);

        when(userRepository.findById(memberId))
                .thenReturn(Optional.of(member));

        when(groupMembershipRepository.countByGroup_Id(groupId))
                .thenReturn(2L);

        StudyGroupResponse response =
                groupInvitationService.acceptInvitation(request);

        assertEquals(groupId, response.id());
        assertEquals(group.getName(), response.name());
        assertEquals(group.getDescription(), response.description());
        assertEquals(group.getCreatedAt(), response.createdAt());
        assertEquals(GroupRole.MEMBER, response.myRole());
        assertEquals(2L, response.memberCount());

        //Verifichiamo che l'invito sia stato consumato
        assertTrue(invitation.isUsed());
        assertSame(member, invitation.getUsedBy());
        assertNotNull(invitation.getUsedAt());

        ArgumentCaptor<GroupMembership> captor =
                ArgumentCaptor.forClass(GroupMembership.class);

        verify(groupMembershipRepository).save(captor.capture());

        GroupMembership savedMembership = captor.getValue();

        assertSame(group, savedMembership.getGroup());
        assertSame(member, savedMembership.getUser());
        assertEquals(GroupRole.MEMBER, savedMembership.getRole());

        //Verifichiamo l'ordine delle operazioni
        InOrder inOrder = inOrder(
                groupInvitationRepository,
                studyGroupRepository,
                groupMembershipRepository,
                userRepository
        );

        inOrder.verify(groupInvitationRepository)
                .findGroupIdByTokenHash(tokenHash);

        inOrder.verify(studyGroupRepository)
                .findByIdForUpdate(groupId);

        inOrder.verify(groupInvitationRepository)
                .findByTokenHash(tokenHash);

        inOrder.verify(groupMembershipRepository)
                .existsByGroup_IdAndUser_Id(groupId, memberId);

        inOrder.verify(userRepository)
                .findById(memberId);

        inOrder.verify(groupMembershipRepository)
                .save(savedMembership);

        inOrder.verify(groupMembershipRepository)
                .countByGroup_Id(groupId);
    }


    @Test //Token inesistente
    void acceptInvitation_InvalidToken(){

        GroupInvitationAcceptRequest request =
                new GroupInvitationAcceptRequest(token);

        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(memberId);

        when(groupInvitationRepository.findGroupIdByTokenHash(tokenHash))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidGroupInvitationException.class,
                () -> groupInvitationService.acceptInvitation(request)
        );

        verifyNoInteractions(
                studyGroupRepository,
                groupMembershipRepository,
                userRepository
        );

        verify(groupInvitationRepository, never())
                .findByTokenHash(anyString());
    }


    @Test //Il gruppo viene eliminato prima dell'acquisizione del lock
    void acceptInvitation_GroupNotFound(){

        GroupInvitationAcceptRequest request =
                new GroupInvitationAcceptRequest(token);

        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(memberId);

        when(groupInvitationRepository.findGroupIdByTokenHash(tokenHash))
                .thenReturn(Optional.of(groupId));

        when(studyGroupRepository.findByIdForUpdate(groupId))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidGroupInvitationException.class,
                () -> groupInvitationService.acceptInvitation(request)
        );

        verify(groupInvitationRepository, never())
                .findByTokenHash(anyString());

        verifyNoInteractions(
                groupMembershipRepository,
                userRepository
        );
    }


    @Test //Invito non più presente dopo il lock
    void acceptInvitation_InvitationNotFound(){

        StudyGroup group = createGroup();

        GroupInvitationAcceptRequest request =
                new GroupInvitationAcceptRequest(token);

        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(memberId);

        when(groupInvitationRepository.findGroupIdByTokenHash(tokenHash))
                .thenReturn(Optional.of(groupId));

        when(studyGroupRepository.findByIdForUpdate(groupId))
                .thenReturn(Optional.of(group));

        when(groupInvitationRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidGroupInvitationException.class,
                () -> groupInvitationService.acceptInvitation(request)
        );

        verifyNoInteractions(
                groupMembershipRepository,
                userRepository
        );
    }


    @Test //Invito già utilizzato
    void acceptInvitation_AlreadyUsed(){

        StudyGroup group = createGroup();

        GroupInvitation invitation = createInvitation(group);

        User firstMember = createUser(3L);

        Instant firstUsage = Instant.now();

        invitation.markAsUsed(firstMember, firstUsage);

        GroupInvitationAcceptRequest request =
                new GroupInvitationAcceptRequest(token);

        prepareAcceptance(group, invitation);

        assertThrows(
                GroupInvitationUsedException.class,
                () -> groupInvitationService.acceptInvitation(request)
        );

        assertSame(firstMember, invitation.getUsedBy());
        assertEquals(firstUsage, invitation.getUsedAt());

        verifyNoInteractions(
                groupMembershipRepository,
                userRepository
        );
    }


    @Test //Invito scaduto
    void acceptInvitation_ExpiredInvitation(){

        StudyGroup group = createGroup();

        User owner = createUser(ownerId);

        Instant now = Instant.now();

        GroupInvitation invitation = new GroupInvitation(
                group,
                owner,
                tokenHash,
                now.minusSeconds(86400),
                now.minusSeconds(60)
        );

        GroupInvitationAcceptRequest request =
                new GroupInvitationAcceptRequest(token);

        prepareAcceptance(group, invitation);

        assertThrows(
                GroupInvitationExpiredException.class,
                () -> groupInvitationService.acceptInvitation(request)
        );

        assertFalse(invitation.isUsed());
        assertNull(invitation.getUsedBy());

        verifyNoInteractions(
                groupMembershipRepository,
                userRepository
        );
    }


    @Test //Utente già appartenente al gruppo
    void acceptInvitation_AlreadyMember(){

        StudyGroup group = createGroup();

        GroupInvitation invitation = createInvitation(group);

        GroupInvitationAcceptRequest request =
                new GroupInvitationAcceptRequest(token);

        prepareAcceptance(group, invitation);

        when(groupMembershipRepository.existsByGroup_IdAndUser_Id(
                groupId, memberId
        )).thenReturn(true);

        assertThrows(
                AlreadyGroupMemberException.class,
                () -> groupInvitationService.acceptInvitation(request)
        );

        //L'invito deve rimanere disponibile
        assertFalse(invitation.isUsed());

        verify(groupMembershipRepository, never())
                .save(any(GroupMembership.class));

        verifyNoInteractions(userRepository);
    }


    @Test //Utente autenticato non presente nel database
    void acceptInvitation_UserNotFound(){

        StudyGroup group = createGroup();

        GroupInvitation invitation = createInvitation(group);

        GroupInvitationAcceptRequest request =
                new GroupInvitationAcceptRequest(token);

        prepareAcceptance(group, invitation);

        when(groupMembershipRepository.existsByGroup_IdAndUser_Id(
                groupId, memberId
        )).thenReturn(false);

        when(userRepository.findById(memberId))
                .thenReturn(Optional.empty());

        assertThrows(
                BadCredentialsException.class,
                () -> groupInvitationService.acceptInvitation(request)
        );

        assertFalse(invitation.isUsed());

        verify(groupMembershipRepository, never())
                .save(any(GroupMembership.class));
    }


    @Test //Errore durante il salvataggio dell'appartenenza
    void acceptInvitation_SaveMembershipError(){

        StudyGroup group = createGroup();

        User member = createUser(memberId);

        GroupInvitation invitation = createInvitation(group);

        GroupInvitationAcceptRequest request =
                new GroupInvitationAcceptRequest(token);

        prepareAcceptance(group, invitation);

        when(groupMembershipRepository.existsByGroup_IdAndUser_Id(
                groupId, memberId
        )).thenReturn(false);

        when(userRepository.findById(memberId))
                .thenReturn(Optional.of(member));

        when(groupMembershipRepository.save(any(GroupMembership.class)))
                .thenThrow(new IllegalStateException(
                        "Errore durante il salvataggio"
                ));

        assertThrows(
                IllegalStateException.class,
                () -> groupInvitationService.acceptInvitation(request)
        );

        //L'invito non deve essere consumato
        assertFalse(invitation.isUsed());
        assertNull(invitation.getUsedBy());

        verify(groupMembershipRepository, never())
                .countByGroup_Id(anyLong());
    }


    //METODI DI SUPPORTO

    private StudyGroup createGroup(){

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


    private User createUser(Long userId){

        User user = new User(
                "utente" + userId + "@example.com",
                "passwordHash",
                "Utente",
                "Test"
        );

        ReflectionTestUtils.setField(user, "id", userId);

        return user;
    }


    private GroupInvitation createInvitation(StudyGroup group){

        Instant now = Instant.now();

        return new GroupInvitation(
                group,
                createUser(ownerId),
                tokenHash,
                now.minusSeconds(60),
                now.plusSeconds(86400)
        );
    }


    //Simula il gruppo bloccato e il membro che crea l'invito
    private void prepareCreation(
            StudyGroup group,
            GroupMembership membership
    ){

        Long userId = membership.getUser().getId();

        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(userId);

        when(studyGroupRepository.findByIdForUpdate(groupId))
                .thenReturn(Optional.of(group));

        when(groupMembershipRepository.findByGroup_IdAndUser_Id(
                groupId, userId
        )).thenReturn(Optional.of(membership));
    }


    //Simula le tre operazioni eseguite durante l'accettazione
    private void prepareAcceptance(
            StudyGroup group,
            GroupInvitation invitation
    ){

        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(memberId);

        when(groupInvitationRepository.findGroupIdByTokenHash(tokenHash))
                .thenReturn(Optional.of(groupId));

        when(studyGroupRepository.findByIdForUpdate(groupId))
                .thenReturn(Optional.of(group));

        when(groupInvitationRepository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(invitation));
    }
}
