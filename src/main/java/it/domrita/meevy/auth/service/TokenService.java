
package it.domrita.meevy.auth.service;

import it.domrita.meevy.user.model.User;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import it.domrita.meevy.common.security.OpaqueTokenGenerator;
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

    //Genera un refresh token casuale che verrà mantenuto dal client
    public String generateRefreshToken() {

        return OpaqueTokenGenerator.generate();
    }

    //Calcola l'hash del refresh token che salviamo nel db
    public String hashRefreshToken(String refreshToken) {

        return OpaqueTokenGenerator.hash(refreshToken);
    }


     //Calcola quando deve scadere un refresh token.

    public Instant calculateRefreshExpiration() {
        return Instant.now().plus(REFRESH_TOKEN_DURATION);
    }
}
