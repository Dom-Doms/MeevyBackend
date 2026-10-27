
package it.domrita.meevy.user.auth;

import org.springframework.context.annotation.Profile;
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

        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new IllegalStateException(
                    "Nessun utente JWT autenticato."
            );
        }

        return Long.valueOf(
                jwtAuth.getToken().getSubject() //TODO da verificare questo errore
        );
    }
}
