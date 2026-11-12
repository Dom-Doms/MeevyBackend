
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

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupInvitationService {

    private static final Duration INVITATION_DURATION =
            Duration.ofHours(24);

    private final GroupInvitationRepository groupInvitationRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final StudyGroupRepository studyGroupRepository;
    private final UserRepository userRepository;
    private final CurrentUserIdProvider currentUserIdProvider;


    //Genera un nuovo invito per il gruppo.
    public GroupInvitationResponse createInvitation(Long groupId){

        Long userId = currentUserIdProvider.getCurrentUserId();

        StudyGroup group = studyGroupRepository
                .findByIdForUpdate(groupId)
                .orElseThrow(() ->
                        new GroupNotFoundException(groupId));

        GroupMembership membership = groupMembershipRepository
                .findByGroup_IdAndUser_Id(groupId, userId)
                .orElseThrow(() ->
                        new GroupNotFoundException(groupId));

        //Solo OWNER e MODERATOR possono creare inviti.
        if (membership.getRole() != GroupRole.OWNER
                && membership.getRole() != GroupRole.MODERATOR) {

            throw new GroupAccessDeniedException(
                    "Non hai i permessi per creare inviti."
            );
        }

        String token = OpaqueTokenGenerator.generate();

        String tokenHash = OpaqueTokenGenerator.hash(token);

        Instant now = Instant.now();
        Instant expiresAt = now.plus(INVITATION_DURATION);

        GroupInvitation invitation = new GroupInvitation(
                group,
                membership.getUser(),
                tokenHash,
                now,
                expiresAt
        );

        groupInvitationRepository.save(invitation);

        return new GroupInvitationResponse(
                groupId,
                token,
                expiresAt
        );
    }


    //Accetta un invito e iscrive l'utente al gruppo.
    public StudyGroupResponse acceptInvitation(
            GroupInvitationAcceptRequest request
    ){

        Long userId = currentUserIdProvider.getCurrentUserId();

        String tokenHash =
                OpaqueTokenGenerator.hash(request.token());

        //Recuperiamo solo l'id prima di acquisire il lock.
        Long groupId = groupInvitationRepository
                .findGroupIdByTokenHash(tokenHash)
                .orElseThrow(InvalidGroupInvitationException::new);

        StudyGroup group = studyGroupRepository
                .findByIdForUpdate(groupId)
                .orElseThrow(InvalidGroupInvitationException::new);

        //Rileggiamo l'invito dopo aver acquisito il lock.
        GroupInvitation invitation = groupInvitationRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(InvalidGroupInvitationException::new);

        if (invitation.isUsed()) {
            throw new GroupInvitationUsedException();
        }

        Instant now = Instant.now();

        if (invitation.isExpired(now)) {
            throw new GroupInvitationExpiredException();
        }

        //L'utente non deve essere già iscritto al gruppo.
        if (groupMembershipRepository
                .existsByGroup_IdAndUser_Id(groupId, userId)) {

            throw new AlreadyGroupMemberException();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException(
                        "Utente autenticato non presente nel database."
                ));

        GroupMembership membership = new GroupMembership(
                group,
                user,
                GroupRole.MEMBER
        );

        groupMembershipRepository.save(membership);

        //L'invito non potrà più essere utilizzato.
        invitation.markAsUsed(user, now);

        long memberCount =
                groupMembershipRepository.countByGroup_Id(groupId);

        return new StudyGroupResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getCreatedAt(),
                GroupRole.MEMBER,
                memberCount
        );
    }
}
