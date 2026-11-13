
package it.domrita.meevy.group.model;

import it.domrita.meevy.user.model.User;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "group_invitation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private StudyGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    //Nel database salviamo solo l'hash del token.
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "used_by_user_id")
    private User usedBy;


    public GroupInvitation(
            StudyGroup group,
            User createdBy,
            String tokenHash,
            Instant createdAt,
            Instant expiresAt
    ) {
        this.group = group;
        this.createdBy = createdBy;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }


    public boolean isUsed(){
        return usedAt != null;
    }


    public boolean isExpired(Instant now){
        return !now.isBefore(expiresAt);
    }


    //Registra l'utilizzo dell'invito.
    public void markAsUsed(User user, Instant now){

        if (isUsed()) {
            throw new IllegalStateException("Invito già utilizzato.");
        }

        if (isExpired(now) || now.isBefore(createdAt)) {
            throw new IllegalStateException("Invito non valido.");
        }

        this.usedAt = now;
        this.usedBy = user;
    }
}
