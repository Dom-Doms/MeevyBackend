
package it.domrita.meevy.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/*
 * Contiene il refresh token inviato dal client
 * per ottenere un nuovo access JWT.
 */
public record RefreshRequest(

        @NotBlank
        @Size(max = 256)
        String refreshToken

) {}
