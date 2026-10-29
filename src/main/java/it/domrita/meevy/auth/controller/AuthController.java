
package it.domrita.meevy.auth.controller;

import it.domrita.meevy.auth.dto.LoginRequest;
import it.domrita.meevy.auth.dto.LoginResponse;
import it.domrita.meevy.auth.dto.LogoutRequest;
import it.domrita.meevy.auth.dto.RefreshRequest;
import it.domrita.meevy.auth.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
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

    /*
     * POST /auth/refresh
     *
     * Riceve il refresh token e restituisce
     * un nuovo access JWT e un nuovo refresh token.
     */
    @PostMapping("/refresh")
    public LoginResponse refresh(
            @Valid @RequestBody RefreshRequest request
    ) {
        return authService.refresh(request);
    }


    /*
     * POST /auth/logout
     *
     * Revoca il refresh token del dispositivo corrente.
     * Restituisce 204 No Content.
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @Valid @RequestBody LogoutRequest request
    ) {
        authService.logout(request);
    }

}
