
package it.domrita.meevy.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/*
 * Dati necessari per terminare una sessione.
 *
 * Il client invia il refresh token da revocare.
 * Non è necessario inviare un access JWT valido.
 */
public record LogoutRequest(

        @NotBlank
        @Size(max = 256)
        String refreshToken

) {}
