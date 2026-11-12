
package it.domrita.meevy.group.repository;

import it.domrita.meevy.group.model.GroupMembership;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMembershipRepository
        extends JpaRepository<GroupMembership, Long> {

    // Recupera l'appartenenza di un utente a un gruppo.
    Optional<GroupMembership> findByGroup_IdAndUser_Id(
            Long groupId,
            Long userId
    );

    // Recupera tutte le appartenenze dell'utente.
    List<GroupMembership> findAllByUser_IdOrderByIdAsc(
            Long userId
    );

    // Recupera i membri in ordine di ingresso.
    List<GroupMembership> findAllByGroup_IdOrderByJoinedAtAscIdAsc(
            Long groupId
    );

    // Conta i membri del gruppo.
    long countByGroup_Id(Long groupId);

    // Verifica se l'utente appartiene al gruppo.
    boolean existsByGroup_IdAndUser_Id(
            Long groupId,
            Long userId
    );
}
