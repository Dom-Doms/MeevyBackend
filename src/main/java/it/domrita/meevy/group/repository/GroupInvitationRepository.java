
package it.domrita.meevy.group.repository;

import it.domrita.meevy.group.model.GroupInvitation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GroupInvitationRepository
        extends JpaRepository<GroupInvitation, Long> {

    //Recupera solo l'id del gruppo associato al token.
    @Query("""
            SELECT i.group.id
            FROM GroupInvitation i
            WHERE i.tokenHash = :tokenHash
            """)
    Optional<Long> findGroupIdByTokenHash(
            @Param("tokenHash") String tokenHash
    );

    //Recupera l'invito tramite il suo hash.
    Optional<GroupInvitation> findByTokenHash(String tokenHash);
}
