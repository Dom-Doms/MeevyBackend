
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

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StudyGroupService {

    private final StudyGroupRepository studyGroupRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final UserRepository userRepository;
    private final CurrentUserIdProvider currentUserIdProvider;

    // Crea il gruppo e iscrive il creatore come OWNER.
    public StudyGroupResponse createGroup(StudyGroupRequest request) {

        Long userId = currentUserIdProvider.getCurrentUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException(
                        "Utente autenticato non presente nel database."
                ));

        StudyGroup group = new StudyGroup(
                request.name().trim(),
                request.description()
        );

        StudyGroup savedGroup = studyGroupRepository.save(group);

        GroupMembership membership = new GroupMembership(
                savedGroup,
                user,
                GroupRole.OWNER
        );

        GroupMembership savedMembership =
                groupMembershipRepository.save(membership);

        return toGroupResponse(savedMembership);
    }

    // Recupera i gruppi dell'utente corrente.
    @Transactional(readOnly = true)
    public List<StudyGroupResponse> getAllGroups() {

        Long userId = currentUserIdProvider.getCurrentUserId();

        return groupMembershipRepository
                .findAllByUser_IdOrderByIdAsc(userId)
                .stream()
                .map(this::toGroupResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudyGroupResponse getGroupById(Long groupId) {

        Long userId = currentUserIdProvider.getCurrentUserId();

        GroupMembership membership =
                getCurrentMembership(groupId, userId);

        return toGroupResponse(membership);
    }

    // Solo l'OWNER può modificare il gruppo.
    public StudyGroupResponse updateGroup(
            Long groupId,
            StudyGroupRequest request
    ) {

        Long userId = currentUserIdProvider.getCurrentUserId();

        StudyGroup group = lockGroup(groupId);

        GroupMembership membership =
                getCurrentMembership(groupId, userId);

        requireOwner(membership); //controlla che un utente sia owner per modificarlo

        group.updateDetails(
                request.name().trim(),
                request.description()
        );

        return toGroupResponse(membership);
    }

    // Elimina il gruppo e le relative appartenenze.
    public void deleteGroup(Long groupId) {

        Long userId = currentUserIdProvider.getCurrentUserId();

        StudyGroup group = lockGroup(groupId);

        GroupMembership membership =
                getCurrentMembership(groupId, userId);

        requireOwner(membership);

        studyGroupRepository.delete(group);
    }

    // Solo i membri possono vedere gli altri iscritti.
    @Transactional(readOnly = true)
    public List<GroupMemberResponse> getMembers(Long groupId) {

        Long userId = currentUserIdProvider.getCurrentUserId();

        getCurrentMembership(groupId, userId);

        return groupMembershipRepository
                .findAllByGroup_IdOrderByJoinedAtAscIdAsc(groupId)
                .stream()
                .map(this::toMemberResponse)
                .toList();
    }

    // Nomina o revoca un moderatore.
    public GroupMemberResponse updateMemberRole(
            Long groupId,
            Long targetUserId,
            GroupRoleRequest request
    ) {

        Long userId = currentUserIdProvider.getCurrentUserId();

        lockGroup(groupId);

        GroupMembership currentMembership =
                getCurrentMembership(groupId, userId);

        requireOwner(currentMembership);

        GroupRole newRole = request.role();

        // OWNER si assegna solo tramite il trasferimento di proprietà.
        if (newRole != GroupRole.MODERATOR
                && newRole != GroupRole.MEMBER) {

            throw new InvalidGroupRoleException(
                    "È possibile assegnare solo i ruoli MODERATOR e MEMBER."
            );
        }

        GroupMembership targetMembership =
                getTargetMembership(groupId, targetUserId);

        if (targetMembership.getRole() == GroupRole.OWNER) {
            throw new InvalidGroupRoleException(
                    "Non è possibile modificare direttamente il ruolo del proprietario."
            );
        }

        targetMembership.changeRole(newRole);

        return toMemberResponse(targetMembership);
    }

    // Rimuove un membro rispettando la gerarchia dei ruoli.
    public void removeMember(
            Long groupId,
            Long targetUserId
    ) {

        Long userId = currentUserIdProvider.getCurrentUserId();

        lockGroup(groupId);

        GroupMembership currentMembership =
                getCurrentMembership(groupId, userId);

        GroupRole currentRole = currentMembership.getRole();

        if (currentRole == GroupRole.MEMBER) {
            throw new GroupAccessDeniedException(
                    "Non hai i permessi per rimuovere membri."
            );
        }

        if (userId.equals(targetUserId)) {
            throw new GroupAccessDeniedException(
                    "Per abbandonare il gruppo utilizza l'apposita funzionalità."
            );
        }

        GroupMembership targetMembership =
                getTargetMembership(groupId, targetUserId);

        GroupRole targetRole = targetMembership.getRole();

        // Nessuno può espellere il proprietario.
        if (targetRole == GroupRole.OWNER) {
            throw new GroupAccessDeniedException(
                    "Non è possibile rimuovere il proprietario."
            );
        }

        // Un moderatore può rimuovere solo i MEMBER.
        if (currentRole == GroupRole.MODERATOR
                && targetRole != GroupRole.MEMBER) {

            throw new GroupAccessDeniedException(
                    "Un moderatore non può rimuovere un altro moderatore."
            );
        }

        groupMembershipRepository.delete(targetMembership);
    }

    // L'utente corrente abbandona il gruppo.
    public void leaveGroup(Long groupId) {

        Long userId = currentUserIdProvider.getCurrentUserId();

        lockGroup(groupId);

        GroupMembership membership =
                getCurrentMembership(groupId, userId);

        if (membership.getRole() == GroupRole.OWNER) {
            throw new OwnerCannotLeaveException();
        }

        groupMembershipRepository.delete(membership);
    }

    // Trasferisce la proprietà a un altro membro del gruppo.
    public void transferOwnership(
            Long groupId,
            GroupOwnerTransferRequest request
    ) {

        Long userId = currentUserIdProvider.getCurrentUserId();
        Long newOwnerUserId = request.newOwnerUserId();

        lockGroup(groupId);

        GroupMembership currentOwner =
                getCurrentMembership(groupId, userId);

        requireOwner(currentOwner);

        if (userId.equals(newOwnerUserId)) {
            throw new InvalidOwnershipTransferException(
                    "Sei già il proprietario del gruppo."
            );
        }

        GroupMembership newOwner =
                getTargetMembership(groupId, newOwnerUserId);

        // Prima rimuove il ruolo OWNER al vecchio proprietario.
        currentOwner.changeRole(GroupRole.MEMBER);

        // Evita di violare l'indice univoco della V8.
        groupMembershipRepository.flush();

        newOwner.changeRole(GroupRole.OWNER);
    }


    // METODI DI SUPPORTO

    // Recupera l'appartenenza dell'utente autenticato.
    private GroupMembership getCurrentMembership(
            Long groupId,
            Long userId
    ) {

        return groupMembershipRepository
                .findByGroup_IdAndUser_Id(groupId, userId)
                .orElseThrow(() ->
                        new GroupNotFoundException(groupId));
    }

    // Recupera il membro sul quale si vuole operare.
    private GroupMembership getTargetMembership(
            Long groupId,
            Long userId
    ) {

        return groupMembershipRepository
                .findByGroup_IdAndUser_Id(groupId, userId)
                .orElseThrow(() ->
                        new GroupMemberNotFoundException(userId));
    }

    // Verifica che l'utente sia il proprietario.
    private void requireOwner(GroupMembership membership) {

        if (membership.getRole() != GroupRole.OWNER) {
            throw new GroupAccessDeniedException(
                    "Solo il proprietario può eseguire questa operazione."
            );
        }
    }

    // Serializza le modifiche concorrenti sullo stesso gruppo.
    private StudyGroup lockGroup(Long groupId) {

        return studyGroupRepository
                .findByIdForUpdate(groupId)
                .orElseThrow(() ->
                        new GroupNotFoundException(groupId));
    }

    private StudyGroupResponse toGroupResponse(
            GroupMembership membership
    ) {

        StudyGroup group = membership.getGroup();

        long memberCount =
                groupMembershipRepository.countByGroup_Id(group.getId());

        return new StudyGroupResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getCreatedAt(),
                membership.getRole(),
                memberCount
        );
    }

    private GroupMemberResponse toMemberResponse(
            GroupMembership membership
    ) {

        User user = membership.getUser();

        return new GroupMemberResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                membership.getRole(),
                membership.getJoinedAt()
        );
    }
}
