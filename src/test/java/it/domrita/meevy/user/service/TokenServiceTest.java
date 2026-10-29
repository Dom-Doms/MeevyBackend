
package it.domrita.meevy.user.service;

import it.domrita.meevy.auth.service.TokenService;
import it.domrita.meevy.user.model.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.jwt.*;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TokenServiceTest {

    private JwtEncoder jwtEncoder;
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        jwtEncoder = mock(JwtEncoder.class);
        tokenService = new TokenService(jwtEncoder, "meevy");
    }

    @Test
    void accessTokenContieneIdUtenteEScadenza() {

        User user = mock(User.class);
        when(user.getId()).thenReturn(42L);

        Jwt signedJwt = mock(Jwt.class);
        when(signedJwt.getTokenValue()).thenReturn("jwt-firmato");

        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(signedJwt);

        String result = tokenService.generateAccessToken(user);

        assertEquals("jwt-firmato", result);

        ArgumentCaptor<JwtEncoderParameters> captor =
                ArgumentCaptor.forClass(JwtEncoderParameters.class);

        verify(jwtEncoder).encode(captor.capture());

        JwtClaimsSet claims = captor.getValue().getClaims();

        assertEquals("42", claims.getSubject());
        assertEquals("meevy", claims.getClaim("iss"));

        assertEquals(
                Duration.ofMinutes(30),
                Duration.between(
                        claims.getIssuedAt(),
                        claims.getExpiresAt()
                )
        );
    }

    @Test
    void refreshTokenSonoCasualiEFormatoValido() {

        String first = tokenService.generateRefreshToken();
        String second = tokenService.generateRefreshToken();

        // 32 byte codificati in Base64URL senza padding.
        assertEquals(43, first.length());
        assertTrue(first.matches("[A-Za-z0-9_-]{43}"));

        // Due generazioni devono produrre valori differenti.
        assertNotEquals(first, second);
    }

    @Test
    void hashRefreshTokenUsaSha256() {

        String hash = tokenService.hashRefreshToken("abc");

        assertEquals(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                hash
        );

        assertEquals(64, hash.length());
    }

    @Test
    void refreshTokenScadeDopoTrentaGiorni() {

        Instant earliest = Instant.now().plus(Duration.ofDays(30));

        Instant expires = tokenService.calculateRefreshExpiration();

        Instant latest = Instant.now().plus(Duration.ofDays(30));

        assertFalse(expires.isBefore(earliest));
        assertFalse(expires.isAfter(latest));
    }
}
