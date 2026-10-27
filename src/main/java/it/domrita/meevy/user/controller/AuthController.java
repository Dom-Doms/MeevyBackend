
package it.domrita.meevy.user.controller;

import it.domrita.meevy.user.dto.LoginRequest;
import it.domrita.meevy.user.dto.LoginResponse;
import it.domrita.meevy.user.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

/*
 * Gestisce gli endpoint HTTP di autenticazione.
 *
 * Il Controller riceve i dati e delega la logica
 * di autenticazione ad AuthService.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Profile("!dev")
public class AuthController {

    private final AuthService authService;

    /*
     * POST /auth/login
     *
     * Riceve email e password.
     * Se valide, restituisce access token e refresh token.
     */
    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request
    ) {
        return authService.login(request);
    }
}
