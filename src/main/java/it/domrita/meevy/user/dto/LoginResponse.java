
package it.domrita.meevy.user.dto;

/*
 * Risposta restituita dopo un login riuscito.
 *
 * accessToken: JWT utilizzato per accedere alle API.
 * refreshToken: token utilizzato per rinnovare l'accesso.
 * tokenType: tipo di autenticazione HTTP (Bearer).
 * expiresInSeconds: durata dell'access token in secondi.
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds
) {}
