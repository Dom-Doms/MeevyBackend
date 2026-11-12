
package it.domrita.meevy.group.model;

import it.domrita.meevy.group.role.GroupRole;
import it.domrita.meevy.user.model.User;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
        name = "group_membership",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_group_membership_group_user",
                        columnNames = {"group_id", "user_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupMembership {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Gruppo a cui appartiene l'utente.
     *
     * Un gruppo può avere più appartenenze,
     * mentre ogni appartenenza riguarda un solo gruppo.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private StudyGroup group;

    /*
     * Utente iscritto al gruppo.
     *
     * Uno stesso utente può appartenere
     * a diversi gruppi di studio.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupRole role;


    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;


    @PrePersist
    private void onCreate() {
        joinedAt = Instant.now();
    }

    /*
     * Crea una nuova appartenenza a un gruppo.
     *
     * Il Service determina il ruolo da assegnare:
     * OWNER per il creatore del gruppo,
     * MEMBER per chi entra tramite invito.
     */
    public GroupMembership(
            StudyGroup group,
            User user,
            GroupRole role
    ) {
        this.group = group;
        this.user = user;
        this.role = role;
    }

    // Modifica il ruolo dell'utente nel gruppo.

    public void changeRole(GroupRole newRole) {
        this.role = newRole;
    }
}
