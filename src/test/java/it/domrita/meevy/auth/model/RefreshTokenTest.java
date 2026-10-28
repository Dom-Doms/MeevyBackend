
package it.domrita.meevy.auth.model;

import it.domrita.meevy.user.model.User;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Test unitari del ciclo di vita di RefreshToken.
 *
 * Utilizziamo l'entità reale senza Mockito
 * e senza collegarci a PostgreSQL.
 */
class RefreshTokenTest {

    private final Instant createdAt =
            Instant.parse("2026-10-28T10:00:00Z");

    private final Instant expiresAt =
            createdAt.plus(Duration.ofDays(30));

    private User createUser() {
        return new User(
                "dom@example.com",
                "password-hash",
                "Dom",
                "Rita"
        );
    }

    private RefreshToken createToken() {

        return new RefreshToken(
                createUser(),
                "hash-del-token",
                createdAt,
                expiresAt
        );
    }

    @Test
    void creazioneTokenImpostaDatiCorretti() {

        User user = createUser();

        RefreshToken token = new RefreshToken(
                user,
                "hash-del-token",
                createdAt,
                expiresAt
        );

        assertSame(user, token.getUser());
        assertEquals("hash-del-token", token.getTokenHash());
        assertEquals(createdAt, token.getCreatedAt());
        assertEquals(expiresAt, token.getExpiresAt());
        assertNull(token.getRevokedAt());
    }

    @Test
    void tokenValidoPrimaDellaScadenza() {

        RefreshToken token = createToken();

        Instant now = createdAt.plus(Duration.ofDays(1));

        assertTrue(token.isUsableAt(now));
    }

    @Test
    void tokenScadutoNonEUtilizzabile() {

        RefreshToken token = createToken();

        // Esattamente alla scadenza.
        assertFalse(token.isUsableAt(expiresAt));

        // Dopo la scadenza.
        assertFalse(token.isUsableAt(
                expiresAt.plusSeconds(1)
        ));
    }

    @Test
    void revocaRendeIlTokenNonUtilizzabile() {

        RefreshToken token = createToken();

        Instant revokedAt = createdAt.plusSeconds(100);

        assertTrue(token.isUsableAt(revokedAt));

        token.revoke(revokedAt);

        assertEquals(revokedAt, token.getRevokedAt());

        // Il token non deve più essere utilizzabile,
        // anche se non è ancora scaduto.
        assertFalse(token.isUsableAt(
                revokedAt.plusSeconds(1)
        ));
    }

    @Test
    void doppiaRevocaMantieneLaPrimaData() {

        RefreshToken token = createToken();

        Instant firstRevocation =
                createdAt.plusSeconds(100);

        Instant secondRevocation =
                createdAt.plusSeconds(200);

        token.revoke(firstRevocation);
        token.revoke(secondRevocation);

        // La seconda chiamata non modifica la prima revoca.
        assertEquals(
                firstRevocation,
                token.getRevokedAt()
        );
    }

    @Test
    void scadenzaNonSuccessivaAllaCreazioneVieneRifiutata() {

        User user = createUser();

        // Scadenza uguale alla creazione.
        assertThrows(
                IllegalArgumentException.class,
                () -> new RefreshToken(
                        user,
                        "hash",
                        createdAt,
                        createdAt
                )
        );

        // Scadenza precedente alla creazione.
        assertThrows(
                IllegalArgumentException.class,
                () -> new RefreshToken(
                        user,
                        "hash",
                        createdAt,
                        createdAt.minusSeconds(1)
                )
        );
    }

    @Test
    void datiObbligatoriNullVengonoRifiutati() {

        User user = createUser();

        assertThrows(
                NullPointerException.class,
                () -> new RefreshToken(
                        null, "hash", createdAt, expiresAt
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> new RefreshToken(
                        user, null, createdAt, expiresAt
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> new RefreshToken(
                        user, "hash", null, expiresAt
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> new RefreshToken(
                        user, "hash", createdAt, null
                )
        );
    }
}
