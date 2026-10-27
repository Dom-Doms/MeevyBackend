
package it.domrita.meevy.user.auth;

import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/*
 * Recupera l'ID dell'utente autenticato
 * dal JWT verificato da Spring Security.
 *
 * È utilizzato quando NON è attivo il profilo dev.
 */
@Component
@Profile("!dev")
public class JwtCurrentUserIdProvider
        implements CurrentUserIdProvider {

    @Override
    public Long getCurrentUserId() {

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        // Controlla che l'utente sia autenticato tramite JWT.
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new BadCredentialsException(
                    "Nessun utente JWT autenticato."
            );
        }

        // Recupera l'ID dell'utente dal campo "sub" del JWT.
        String subject = jwtAuth.getToken().getSubject();

        // Verifica che l'identificativo sia presente.
        if (subject == null || subject.isBlank()) {
            throw new BadCredentialsException(
                    "JWT senza identificativo utente."
            );
        }

        // Converte l'identificativo da String a Long.
        try {
            return Long.valueOf(subject);

        } catch (NumberFormatException ex) {
            throw new BadCredentialsException(
                    "Identificativo utente non valido.",
                    ex
            );
        }
    }
}
