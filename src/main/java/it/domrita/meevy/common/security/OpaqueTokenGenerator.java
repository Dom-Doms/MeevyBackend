
package it.domrita.meevy.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

public final class OpaqueTokenGenerator {

    private static final SecureRandom secureRandom = new SecureRandom();

    private OpaqueTokenGenerator() {
    }

    //Genera un token casuale utilizzabile anche negli URL.
    public static String generate(){

        byte[] randomBytes = new byte[32];

        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }


    //Calcola l'hash SHA-256 del token.
    public static String hash(String token){

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException ex) {

            throw new IllegalStateException(
                    "Algoritmo SHA-256 non disponibile.",
                    ex
            );
        }
    }
}
