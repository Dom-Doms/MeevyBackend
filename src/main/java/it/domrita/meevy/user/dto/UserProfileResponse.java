
package it.domrita.meevy.user.dto;

import it.domrita.meevy.role.AccountStatus;
import it.domrita.meevy.role.PlatformRole;

/*
 * Rappresenta le informazioni del profilo
 * dell'utente autenticato.
 *
 * Non espone passwordHash o altri dati sensibili.
 */
public record UserProfileResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        PlatformRole platformRole,
        AccountStatus accountStatus
) {
}
