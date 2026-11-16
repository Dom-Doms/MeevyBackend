
package it.domrita.meevy.user.controller;

import it.domrita.meevy.user.dto.RegisterRequest;
import it.domrita.meevy.user.dto.RegisterResponse;
import it.domrita.meevy.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import it.domrita.meevy.user.dto.UserProfileResponse;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /*
     * Registra un nuovo utente.
     *
     * POST /api/users/register
     *
     * Restituisce HTTP 201 se la registrazione
     * viene completata correttamente.
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        RegisterResponse response = userService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // GET /user/me
     //Restituisce le informazioni dell'utente autenticato.

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentProfile() {

        UserProfileResponse response =
                userService.getCurrentProfile();

        return ResponseEntity.ok(response);
    }

}
