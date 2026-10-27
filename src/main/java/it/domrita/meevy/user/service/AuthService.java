
package it.domrita.meevy.user.service;

import it.domrita.meevy.user.dto.LoginRequest;
import it.domrita.meevy.user.dto.LoginResponse;
import it.domrita.meevy.user.model.RefreshToken;
import it.domrita.meevy.user.model.User;
import it.domrita.meevy.user.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/*
 * Coordina il login dell'utente.
 *
 * 1. Verifica le credenziali tramite UserService.
 * 2. Genera un refresh token casuale.
 * 3. Salva soltanto l'hash nel database.
 * 4. Genera un access JWT.
 * 5. Restituisce entrambi i token al client.
 */
@Service
@Profile("!dev")
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final TokenService tokenService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public LoginResponse login(LoginRequest request) {

        // Verifica email, password e stato dell'account.
        User user = userService.authenticate(request);

        // Genera un refresh token casuale.
        String refreshToken = tokenService.generateRefreshToken();

        // Calcola l'hash da conservare nel database.
        String tokenHash = tokenService.hashRefreshToken(refreshToken);

        // Stabilisce creazione e scadenza della sessione.
        Instant now = Instant.now();

        Instant expiresAt = now.plus(
                TokenService.REFRESH_TOKEN_DURATION
        );

        // Crea l'entità della sessione.
        RefreshToken session = new RefreshToken(
                user,
                tokenHash,
                now,
                expiresAt
        );

        // Salva la sessione nel database.
        refreshTokenRepository.saveAndFlush(session);

        // Genera il JWT dell'utente autenticato.
        String accessToken = tokenService.generateAccessToken(user);

        // Restituisce i token al frontend.
        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                TokenService.ACCESS_TOKEN_DURATION.toSeconds()
        );
    }
}
