
package it.domrita.meevy.user.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

/*
 * Rappresenta una sessione di autenticazione persistente.
 *
 * Il refresh token originale viene consegnato al client.
 * Nel database conserviamo soltanto il suo hash SHA-256.
 *
 * Ogni refresh token appartiene a un utente e può
 * essere utilizzato fino alla scadenza o alla revoca.
 */
@Entity
@Table(
        name = "refresh_token",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_refresh_token_hash",
                        columnNames = "token_hash"
                )
        },
        indexes = {
                @Index(
                        name = "idx_refresh_token_user_id",
                        columnList = "user_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Utente proprietario della sessione.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_refresh_token_user")
    )
    private User user;

    // Hash SHA-256, non il refresh token originale.
    @Column(name = "token_hash", nullable = false,
            length = 64, updatable = false)
    private String tokenHash;

    @Column(name = "created_at", nullable = false,
            updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false,
            updatable = false)
    private Instant expiresAt;

    // Null significa che il token non è stato revocato.
    @Column(name = "revoked_at")
    private Instant revokedAt;

    public RefreshToken(
            User user,
            String tokenHash,
            Instant createdAt,
            Instant expiresAt
    ) {
        this.user = Objects.requireNonNull(user);
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.expiresAt = Objects.requireNonNull(expiresAt);

        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException(
                    "La scadenza deve essere successiva alla creazione."
            );
        }
    }

    /*
     * Un token è utilizzabile se non è stato revocato
     * e la sua scadenza non è ancora arrivata.
     */
    public boolean isUsableAt(Instant now) {
        return revokedAt == null && now.isBefore(expiresAt);
    }

    /*
     * Revoca il token.
     * Se è già revocato, mantiene la revoca originale.
     */
    public void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = Objects.requireNonNull(now);
        }
    }
}
