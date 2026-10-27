
package it.domrita.meevy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/*
 * Configura la generazione e la verifica dei JWT.
 *
 * Utilizza una chiave segreta esterna per firmare
 * i token con l'algoritmo HS256.
 */
@Configuration
public class JwtConfig {

    /*
     * Legge la chiave segreta dalle variabili d'ambiente
     * e la converte in un oggetto SecretKey.
     */
    @Bean
    public SecretKey jwtSecretKey(
            @Value("${meevy.security.jwt.secret-base64}")
            String secretBase64
    ) {
        byte[] keyBytes = Base64.getDecoder()
                .decode(secretBase64);

        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "La chiave JWT deve contenere almeno 32 byte."
            );
        }

        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    /*
     * Genera e firma i JWT dopo un login valido.
     */
    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return NimbusJwtEncoder
                .withSecretKey(jwtSecretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    /*
     * Verifica la firma, la scadenza
     * e l'emittente dei JWT ricevuti.
     */
    @Bean
    public JwtDecoder jwtDecoder(
            SecretKey jwtSecretKey,
            @Value("${meevy.security.jwt.issuer}") String issuer
    ) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(issuer)
        );

        return decoder;
    }
}
