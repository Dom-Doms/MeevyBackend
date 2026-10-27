
package it.domrita.meevy.user.service;

import it.domrita.meevy.user.model.User;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/*
 * Gestisce la creazione dei token di autenticazione.
 *
 * ACCESS TOKEN:
 * - JWT firmato con HS256;
 * - durata di 30 minuti;
 * - contiene l'ID dell'utente.
 *
 * REFRESH TOKEN:
 * - valore casuale crittograficamente sicuro;
 * - durata prevista di 30 giorni;
 * - nel database verrà salvato soltanto il suo hash.
 *
 * La persistenza e la rotazione dei refresh token
 * saranno implementate successivamente.
 */
@Service
@Profile("!dev")
public class TokenService {

    public static final Duration ACCESS_TOKEN_DURATION =
            Duration.ofMinutes(30);

    public static final Duration REFRESH_TOKEN_DURATION =
            Duration.ofDays(30);

    private final JwtEncoder jwtEncoder;
    private final String issuer;

    private final SecureRandom secureRandom = new SecureRandom();

    /*
     * Spring fornisce il JwtEncoder configurato
     * in JwtConfig e legge l'issuer dalle properties.
     */
    public TokenService(
            JwtEncoder jwtEncoder,
            @Value("${meevy.security.jwt.issuer}") String issuer
    ) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
    }

    /*
     * Genera un access token JWT per l'utente.
     *
     * Il token verrà utilizzato nelle richieste
     * HTTP tramite Authorization: Bearer <token>.
     */
    public String generateAccessToken(User user) {

        Instant now = Instant.now();

        // Informazioni contenute nel JWT.
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(ACCESS_TOKEN_DURATION))
                .build();

        // Algoritmo di firma del token.
        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .type("JWT")
                .build();

        // Firma e restituisce il JWT.
        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }

    /*
     * Genera un refresh token casuale.
     *
     * Non è un JWT: è una stringa imprevedibile
     * che verrà conservata dal client.
     */
    public String generateRefreshToken() {

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    /*
     * Calcola l'hash SHA-256 del refresh token.
     *
     * Salveremo questo hash nel database,
     * non il token originale.
     */
    public String hashRefreshToken(String refreshToken) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    refreshToken.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "Algoritmo SHA-256 non disponibile.",
                    ex
            );
        }
    }

    /*
     * Calcola quando deve scadere un refresh token.
     */
    public Instant calculateRefreshExpiration() {
        return Instant.now().plus(REFRESH_TOKEN_DURATION);
    }
}
