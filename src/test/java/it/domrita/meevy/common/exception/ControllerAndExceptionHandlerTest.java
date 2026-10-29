
package it.domrita.meevy.common.exception;

import it.domrita.meevy.auth.controller.AuthController;
import it.domrita.meevy.auth.dto.LoginResponse;
import it.domrita.meevy.auth.exception.InvalidRefreshTokenException;
import it.domrita.meevy.auth.service.AuthService;
import it.domrita.meevy.user.controller.UserController;
import it.domrita.meevy.user.dto.RegisterRequest;
import it.domrita.meevy.user.dto.RegisterResponse;
import it.domrita.meevy.user.exception.EmailAlreadyRegisteredException;
import it.domrita.meevy.user.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/*
 * Test HTTP dei Controller e della gestione degli errori.
 *
 * MockMvc simula le richieste HTTP senza avviare
 * Spring Boot, PostgreSQL o un server reale.
 *
 * La configurazione Spring Security non viene caricata.
 */
@ExtendWith(MockitoExtension.class)
class ControllerAndExceptionHandlerTest {

    @Mock
    private UserService userService;

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new UserController(userService),
                        new AuthController(authService)
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ========================================
    // REGISTRAZIONE
    // ========================================

    @Test
    void registrazioneValidaRestituisce201() throws Exception {

        when(userService.register(any(RegisterRequest.class)))
                .thenReturn(new RegisterResponse(
                        12L, "dom@example.com", "Dom", "Rita"
                ));

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "email": "dom@example.com",
                          "password": "password123",
                          "firstName": "Dom",
                          "lastName": "Rita"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.email")
                        .value("dom@example.com"))
                .andExpect(jsonPath("$.firstName")
                        .value("Dom"));

        verify(userService).register(any(RegisterRequest.class));
    }

    @Test
    void registrazioneEmailDuplicataRestituisce409()
            throws Exception {

        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(new EmailAlreadyRegisteredException());

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "email": "dom@example.com",
                          "password": "password123"
                        }
                        """))
                .andExpect(status().isConflict())
                .andExpect(content().string(
                        containsString("EMAIL_ALREADY_REGISTERED")
                ));
    }

    @Test
    void registrazioneNonValidaRestituisce400()
            throws Exception {

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "email": "email-non-valida",
                          "password": "123"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        containsString("VALIDATION_ERROR")
                ))
                .andExpect(content().string(
                        containsString("email")
                ));

        verifyNoInteractions(userService);
    }

    @Test
    void jsonNonValidoRestituisce400() throws Exception {

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"email": "dom@example.com", "password":
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        containsString("INVALID_JSON")
                ));

        verifyNoInteractions(userService);
    }

    @Test
    void erroreVincoloDatabaseRestituisce409()
            throws Exception {

        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "Violazione vincolo database"
                ));

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "email": "dom@example.com",
                          "password": "password123"
                        }
                        """))
                .andExpect(status().isConflict())
                .andExpect(content().string(
                        containsString("DATA_CONFLICT")
                ));
    }

    // ========================================
    // LOGIN
    // ========================================

    @Test
    void loginValidoRestituisce200EToken() throws Exception {

        when(authService.login(any()))
                .thenReturn(new LoginResponse(
                        "access-jwt",
                        "refresh-token",
                        "Bearer",
                        1800
                ));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "email": "dom@example.com",
                          "password": "password123"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken")
                        .value("access-jwt"))
                .andExpect(jsonPath("$.refreshToken")
                        .value("refresh-token"))
                .andExpect(jsonPath("$.tokenType")
                        .value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds")
                        .value(1800));
    }

    @Test
    void loginCredenzialiErrateRestituisce401()
            throws Exception {

        when(authService.login(any()))
                .thenThrow(new BadCredentialsException(
                        "Credenziali non valide."
                ));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "email": "dom@example.com",
                          "password": "sbagliata"
                        }
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_CREDENTIALS"));
    }

    @Test
    void loginSenzaPasswordRestituisce400()
            throws Exception {

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "email": "dom@example.com"
                        }
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    // ========================================
    // REFRESH
    // ========================================

    @Test
    void refreshValidoRestituisce200() throws Exception {

        when(authService.refresh(any()))
                .thenReturn(new LoginResponse(
                        "nuovo-access",
                        "nuovo-refresh",
                        "Bearer",
                        1800
                ));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "refreshToken": "vecchio-refresh"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken")
                        .value("nuovo-access"))
                .andExpect(jsonPath("$.refreshToken")
                        .value("nuovo-refresh"));
    }

    @Test
    void refreshNonValidoRestituisce401()
            throws Exception {

        when(authService.refresh(any()))
                .thenThrow(new InvalidRefreshTokenException());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "refreshToken": "token-non-valido"
                        }
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void refreshVuotoRestituisce400() throws Exception {

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "refreshToken": ""
                        }
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    // ========================================
    // LOGOUT
    // ========================================

    @Test
    void logoutValidoRestituisce204() throws Exception {

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "refreshToken": "token-valido"
                        }
                        """))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(authService).logout(any());
    }

    @Test
    void logoutSenzaTokenRestituisce400() throws Exception {

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {}
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }
}
