
package it.domrita.meevy.auth.provider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Test dell'estrazione dell'ID utente dal JWT.
 *
 * Non verifica la firma crittografica:
 * quella è responsabilità del JwtDecoder.
 */
class JwtCurrentUserIdProviderTest {

    private final JwtCurrentUserIdProvider provider =
            new JwtCurrentUserIdProvider();

    /*
     * Pulisce il SecurityContext al termine di
     * ogni test per evitare interferenze.
     */
    @AfterEach
    void cleanSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // Crea un JWT di prova con un claim sub.
    private Jwt createJwt(String subject) {

        return Jwt.withTokenValue("jwt-di-test")
                .header("alg", "HS256")
                .claim("sub", subject)
                .issuedAt(Instant.parse("2026-10-28T10:00:00Z"))
                .expiresAt(Instant.parse("2026-10-28T11:00:00Z"))
                .build();
    }

    @Test
    void jwtValidoRestituisceIdUtente() {

        Jwt jwt = createJwt("42");

        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt)
        );

        Long userId = provider.getCurrentUserId();

        assertEquals(42L, userId);
    }

    @Test
    void autenticazioneAssenteVieneRifiutata() {

        assertThrows(
                BadCredentialsException.class,
                provider::getCurrentUserId
        );
    }

    @Test
    void autenticazioneNonJwtVieneRifiutata() {

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "utente", "password"
                )
        );

        assertThrows(
                BadCredentialsException.class,
                provider::getCurrentUserId
        );
    }

    @Test
    void jwtSenzaSubjectVieneRifiutato() {

        Jwt jwt = Jwt.withTokenValue("jwt-di-test")
                .header("alg", "HS256")
                .claim("iss", "meevy")
                .issuedAt(Instant.parse("2026-10-28T10:00:00Z"))
                .expiresAt(Instant.parse("2026-10-28T11:00:00Z"))
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt)
        );

        assertThrows(
                BadCredentialsException.class,
                provider::getCurrentUserId
        );
    }

    @Test
    void jwtConSubjectVuotoVieneRifiutato() {

        Jwt jwt = createJwt("   ");

        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt)
        );

        assertThrows(
                BadCredentialsException.class,
                provider::getCurrentUserId
        );
    }

    @Test
    void jwtConSubjectNonNumericoVieneRifiutato() {

        Jwt jwt = createJwt("utente-abc");

        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt)
        );

        assertThrows(
                BadCredentialsException.class,
                provider::getCurrentUserId
        );
    }
}
