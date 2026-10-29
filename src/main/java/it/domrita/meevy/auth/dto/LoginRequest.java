
package it.domrita.meevy.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/*
 * Dati necessari per effettuare il login.
 *
 * Il client invia email e password in formato JSON.
 */
public record LoginRequest(

        @NotBlank
        @Email
        String email,

        @NotBlank
        String password

) {}
