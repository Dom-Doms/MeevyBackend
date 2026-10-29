
package it.domrita.meevy.user.service;

import it.domrita.meevy.user.dto.RegisterRequest;
import it.domrita.meevy.user.dto.RegisterResponse;
import it.domrita.meevy.user.exception.EmailAlreadyRegisteredException;
import it.domrita.meevy.user.model.User;
import it.domrita.meevy.user.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/*
 * Test unitari per la registrazione degli utenti.
 *
 * Il database e il PasswordEncoder vengono simulati
 * tramite Mockito.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    /*
     * Verifica che la registrazione:
     * - normalizzi l'email;
     * - codifichi la password;
     * - salvi correttamente l'utente;
     * - restituisca i dati previsti.
     */
    @Test
    void registrazioneValidaCreaUtente() {

        RegisterRequest request = new RegisterRequest(
                "  DOM@EXAMPLE.COM  ",
                "password123",
                "Dom",
                "Rita"
        );

        when(userRepository.existsByEmail("dom@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("password123"))
                .thenReturn("password-hash");

        User savedUser = mock(User.class);

        when(savedUser.getId()).thenReturn(1L);
        when(savedUser.getEmail()).thenReturn("dom@example.com");
        when(savedUser.getFirstName()).thenReturn("Dom");
        when(savedUser.getLastName()).thenReturn("Rita");

        when(userRepository.saveAndFlush(any(User.class)))
                .thenReturn(savedUser);

        RegisterResponse response = userService.register(request);

        // Controlla i dati restituiti.
        assertEquals(1L, response.id());
        assertEquals("dom@example.com", response.email());
        assertEquals("Dom", response.firstName());
        assertEquals("Rita", response.lastName());

        // Controlla cosa viene realmente passato al Repository.
        ArgumentCaptor<User> captor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).saveAndFlush(captor.capture());

        User created = captor.getValue();

        assertEquals("dom@example.com", created.getEmail());
        assertEquals("password-hash", created.getPasswordHash());
        assertEquals("Dom", created.getFirstName());
        assertEquals("Rita", created.getLastName());

        // La password originale non deve essere salvata.
        assertNotEquals("password123", created.getPasswordHash());

        verify(passwordEncoder).encode("password123");
    }

    /*
     * Verifica che un'email già registrata
     * impedisca la creazione di un nuovo account.
     */
    @Test
    void registrazioneEmailDuplicataVieneRifiutata() {

        RegisterRequest request = new RegisterRequest(
                "  DOM@EXAMPLE.COM  ",
                "password123",
                "Dom",
                "Rita"
        );

        when(userRepository.existsByEmail("dom@example.com"))
                .thenReturn(true);

        assertThrows(
                EmailAlreadyRegisteredException.class,
                () -> userService.register(request)
        );

        // Non deve generare password né salvare utenti.
        verifyNoInteractions(passwordEncoder);

        verify(userRepository, never())
                .saveAndFlush(any(User.class));
    }
}
