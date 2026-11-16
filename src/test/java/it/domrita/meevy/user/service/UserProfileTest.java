
package it.domrita.meevy.user.service;

import it.domrita.meevy.auth.repository.CurrentUserIdProvider;
import it.domrita.meevy.role.AccountStatus;
import it.domrita.meevy.role.PlatformRole;
import it.domrita.meevy.user.controller.UserController;
import it.domrita.meevy.user.dto.UserProfileResponse;
import it.domrita.meevy.user.model.User;
import it.domrita.meevy.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserProfileTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CurrentUserIdProvider currentUserIdProvider;

    @InjectMocks
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new UserController(userService))
                .build();
    }

    /*
     * Verifica il recupero dei dati dell'utente
     * identificato dal provider di autenticazione.
     */
    @Test
    void getCurrentProfileRestituisceUtenteAutenticato() {

        User user = mock(User.class);

        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(10L);

        when(userRepository.findById(10L))
                .thenReturn(Optional.of(user));

        when(user.getId()).thenReturn(10L);
        when(user.getEmail()).thenReturn("dom@example.com");
        when(user.getFirstName()).thenReturn("Dom");
        when(user.getLastName()).thenReturn("Rita");
        when(user.getPlatformRole()).thenReturn(PlatformRole.USER);
        when(user.getAccountStatus()).thenReturn(AccountStatus.ACTIVE);

        UserProfileResponse response =
                userService.getCurrentProfile();

        assertEquals(10L, response.id());
        assertEquals("dom@example.com", response.email());
        assertEquals("Dom", response.firstName());
        assertEquals("Rita", response.lastName());
        assertEquals(PlatformRole.USER, response.platformRole());
        assertEquals(AccountStatus.ACTIVE, response.accountStatus());

        verify(userRepository).findById(10L);
    }

    /*
     * Un ID autenticato non presente nel database
     * deve produrre un errore 404.
     */
    @Test
    void getCurrentProfileUtenteInesistente() {

        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(99L);

        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.getCurrentProfile()
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );
    }

    /*
     * Verifica che il Controller restituisca HTTP 200
     * e il JSON previsto dal frontend.
     */
    @Test
    void getUserMeRestituisce200() throws Exception {

        User user = mock(User.class);

        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(10L);

        when(userRepository.findById(10L))
                .thenReturn(Optional.of(user));

        when(user.getId()).thenReturn(10L);
        when(user.getEmail()).thenReturn("dom@example.com");
        when(user.getFirstName()).thenReturn("Dom");
        when(user.getLastName()).thenReturn("Rita");
        when(user.getPlatformRole()).thenReturn(PlatformRole.ADMIN);
        when(user.getAccountStatus()).thenReturn(AccountStatus.ACTIVE);

        mockMvc.perform(get("/user/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.email").value("dom@example.com"))
                .andExpect(jsonPath("$.firstName").value("Dom"))
                .andExpect(jsonPath("$.lastName").value("Rita"))
                .andExpect(jsonPath("$.platformRole").value("ADMIN"))
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));

        verify(currentUserIdProvider).getCurrentUserId();
    }

    /*
     * Verifica la risposta HTTP quando il profilo
     * dell'utente autenticato non esiste.
     */
    @Test
    void getUserMeUtenteInesistenteRestituisce404()
            throws Exception {

        when(currentUserIdProvider.getCurrentUserId())
                .thenReturn(99L);

        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/user/me"))
                .andExpect(status().isNotFound());
    }
}
