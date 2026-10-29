package it.domrita.meevy.auth.provider;

import it.domrita.meevy.auth.repository.CurrentUserIdProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/*
 * Implementazione temporanea per lo sviluppo locale.
 *
 * Restituisce l'ID di un utente di prova configurato.
 * NON deve essere utilizzata in produzione.
 */

@Component
@Profile("dev")

public class DevCurrentUserIdProvider
        implements CurrentUserIdProvider {

    private final Long userId;

    public DevCurrentUserIdProvider(
            @Value("${meevy.dev.user-id}") Long userId
    ) {
        this.userId = userId;
    }

    @Override
    public Long getCurrentUserId() {
        return userId;
    }
}
