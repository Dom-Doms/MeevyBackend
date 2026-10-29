
package it.domrita.meevy.auth.service;

import it.domrita.meevy.auth.dto.LoginRequest;
import it.domrita.meevy.auth.dto.LoginResponse;
import it.domrita.meevy.auth.dto.LogoutRequest;
import it.domrita.meevy.auth.dto.RefreshRequest;
import it.domrita.meevy.auth.exception.InvalidRefreshTokenException;
import it.domrita.meevy.auth.model.RefreshToken;
import it.domrita.meevy.auth.repository.RefreshTokenRepository;
import it.domrita.meevy.role.AccountStatus;
import it.domrita.meevy.user.model.User;
import it.domrita.meevy.user.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/*
 * Test unitari di AuthService.
 *
 * Verificano autenticazione, creazione delle sessioni,
 * rotazione dei refresh token e logout.
 *
 * Repository, PasswordEncoder e TokenService
 * sono simulati tramite Mockito.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AuthService authService;

    // ========================================
    // LOGIN
    // ========================================

    @Test
    void loginValidoGeneraTokenESalvaSessione() {

        LoginRequest request =
                new LoginRequest("  DOM@EXAMPLE.COM  ", "password");

        User user = new User(
                "dom@example.com", "hash", null, null
        );

        // Verifica che l'email venga normalizzata.
        when(userRepository.findByEmail("dom@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("password", "hash"))
                .thenReturn(true);

        when(tokenService.generateRefreshToken())
                .thenReturn("refresh-originale");

        when(tokenService.hashRefreshToken("refresh-originale"))
                .thenReturn("hash-del-refresh");

        when(tokenService.generateAccessToken(user))
                .thenReturn("jwt-firmato");

        LoginResponse response = authService.login(request);

        assertEquals("jwt-firmato", response.accessToken());
        assertEquals("refresh-originale", response.refreshToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(1800, response.expiresInSeconds());

        ArgumentCaptor<RefreshToken> captor =
                ArgumentCaptor.forClass(RefreshToken.class);

        verify(refreshTokenRepository)
                .saveAndFlush(captor.capture());

        RefreshToken saved = captor.getValue();

        assertSame(user, saved.getUser());
        assertEquals("hash-del-refresh", saved.getTokenHash());
        assertNull(saved.getRevokedAt());

        assertEquals(
                TokenService.REFRESH_TOKEN_DURATION,
                Duration.between(
                        saved.getCreatedAt(),
                        saved.getExpiresAt()
                )
        );
    }

    @Test
    void loginConPasswordErrataVieneRifiutato() {

        LoginRequest request =
                new LoginRequest("dom@example.com", "errata");

        User user = new User(
                "dom@example.com", "hash", null, null
        );

        when(userRepository.findByEmail("dom@example.com"))
                .thenReturn(Optional.of(user));

        // Password errata.
        when(passwordEncoder.matches("errata", "hash"))
                .thenReturn(false);

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(request)
        );

        // Nessun token e nessuna sessione creati.
        verifyNoInteractions(
                tokenService,
                refreshTokenRepository
        );
    }

    @Test
    void loginConEmailInesistenteVieneRifiutato() {

        LoginRequest request =
                new LoginRequest("unknown@example.com", "password");

        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(request)
        );

        // Non controlla la password di utenti inesistenti.
        verifyNoInteractions(
                passwordEncoder,
                tokenService,
                refreshTokenRepository
        );
    }

    @Test
    void loginConAccountNonAttivoVieneRifiutato() {

        AccountStatus nonActive = Arrays.stream(
                        AccountStatus.values()
                )
                .filter(status -> status != AccountStatus.ACTIVE)
                .findFirst()
                .orElseThrow();

        LoginRequest request =
                new LoginRequest("dom@example.com", "password");

        User user = mock(User.class);

        when(user.getPasswordHash()).thenReturn("hash");
        when(user.getAccountStatus()).thenReturn(nonActive);

        when(userRepository.findByEmail("dom@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("password", "hash"))
                .thenReturn(true);

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(request)
        );

        verifyNoInteractions(
                tokenService,
                refreshTokenRepository
        );
    }

    // ========================================
    // REFRESH
    // ========================================

    @Test
    void refreshValidoRuotaIlToken() {

        RefreshRequest request = new RefreshRequest("vecchio");

        RefreshToken session = mock(RefreshToken.class);
        User user = mock(User.class);

        when(tokenService.hashRefreshToken("vecchio"))
                .thenReturn("hash-vecchio");

        when(refreshTokenRepository.findByTokenHashForUpdate(
                "hash-vecchio"
        )).thenReturn(Optional.of(session));

        when(session.isUsableAt(any(Instant.class)))
                .thenReturn(true);

        when(session.getUser()).thenReturn(user);

        when(user.getAccountStatus())
                .thenReturn(AccountStatus.ACTIVE);

        when(tokenService.generateRefreshToken())
                .thenReturn("nuovo");

        when(tokenService.hashRefreshToken("nuovo"))
                .thenReturn("hash-nuovo");

        when(tokenService.generateAccessToken(user))
                .thenReturn("nuovo-jwt");

        LoginResponse response = authService.refresh(request);

        assertEquals("nuovo", response.refreshToken());
        assertEquals("nuovo-jwt", response.accessToken());
        assertEquals(1800, response.expiresInSeconds());

        // Il vecchio token viene revocato.
        verify(session).revoke(any(Instant.class));

        // Viene salvata una nuova sessione.
        ArgumentCaptor<RefreshToken> captor =
                ArgumentCaptor.forClass(RefreshToken.class);

        verify(refreshTokenRepository)
                .saveAndFlush(captor.capture());

        RefreshToken newSession = captor.getValue();

        assertSame(user, newSession.getUser());
        assertEquals("hash-nuovo", newSession.getTokenHash());
        assertNull(newSession.getRevokedAt());
    }

    @Test
    void refreshInesistenteVieneRifiutato() {

        when(tokenService.hashRefreshToken("inesistente"))
                .thenReturn("hash-inesistente");

        when(refreshTokenRepository.findByTokenHashForUpdate(
                "hash-inesistente"
        )).thenReturn(Optional.empty());

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> authService.refresh(
                        new RefreshRequest("inesistente")
                )
        );

        verify(refreshTokenRepository, never())
                .saveAndFlush(any(RefreshToken.class));
    }

    @Test
    void refreshRevocatoOScadutoVieneRifiutato() {

        RefreshToken session = mock(RefreshToken.class);

        when(tokenService.hashRefreshToken("non-valido"))
                .thenReturn("hash");

        when(refreshTokenRepository.findByTokenHashForUpdate("hash"))
                .thenReturn(Optional.of(session));

        when(session.isUsableAt(any(Instant.class)))
                .thenReturn(false);

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> authService.refresh(
                        new RefreshRequest("non-valido")
                )
        );

        verify(session, never())
                .revoke(any(Instant.class));

        verify(refreshTokenRepository, never())
                .saveAndFlush(any(RefreshToken.class));
    }

    @Test
    void refreshConAccountNonAttivoVieneRifiutato() {

        AccountStatus nonActive = Arrays.stream(
                        AccountStatus.values()
                )
                .filter(status -> status != AccountStatus.ACTIVE)
                .findFirst()
                .orElseThrow();

        RefreshToken session = mock(RefreshToken.class);
        User user = mock(User.class);

        when(tokenService.hashRefreshToken("token"))
                .thenReturn("hash");

        when(refreshTokenRepository.findByTokenHashForUpdate("hash"))
                .thenReturn(Optional.of(session));

        when(session.isUsableAt(any(Instant.class)))
                .thenReturn(true);

        when(session.getUser()).thenReturn(user);
        when(user.getAccountStatus()).thenReturn(nonActive);

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> authService.refresh(
                        new RefreshRequest("token")
                )
        );

        verify(session, never())
                .revoke(any(Instant.class));

        verify(refreshTokenRepository, never())
                .saveAndFlush(any(RefreshToken.class));
    }

    // ========================================
    // LOGOUT
    // ========================================

    @Test
    void logoutRevocaLaSessione() {

        RefreshToken session = mock(RefreshToken.class);

        when(tokenService.hashRefreshToken("token"))
                .thenReturn("hash");

        when(refreshTokenRepository.findByTokenHashForUpdate("hash"))
                .thenReturn(Optional.of(session));

        authService.logout(new LogoutRequest("token"));

        verify(session).revoke(any(Instant.class));
    }

    @Test
    void logoutConTokenInesistenteNonGeneraErrori() {

        when(tokenService.hashRefreshToken("inesistente"))
                .thenReturn("hash");

        when(refreshTokenRepository.findByTokenHashForUpdate("hash"))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(
                () -> authService.logout(
                        new LogoutRequest("inesistente")
                )
        );

        verify(refreshTokenRepository, never())
                .saveAndFlush(any(RefreshToken.class));
    }
}
