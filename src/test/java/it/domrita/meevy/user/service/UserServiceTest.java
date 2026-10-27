
package it.domrita.meevy.user.service;

import it.domrita.meevy.role.AccountStatus;
import it.domrita.meevy.user.dto.LoginRequest;
import it.domrita.meevy.user.model.User;
import it.domrita.meevy.user.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/*
 * Test della verifica delle credenziali.
 *
 * Non utilizza password reali e non si collega
 * al database: Repository ed Encoder sono simulati.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void credenzialiCorretteAutenticanoUtente() {

        LoginRequest request =
                new LoginRequest("  DOM@EXAMPLE.COM  ", "password");

        User user = new User(
                "dom@example.com", "hash", null, null
        );

        when(userRepository.findByEmail("dom@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("password", "hash"))
                .thenReturn(true);

        User result = userService.authenticate(request);

        assertSame(user, result);

        verify(userRepository)
                .findByEmail("dom@example.com");
    }

    @Test
    void emailInesistenteVieneRifiutata() {

        LoginRequest request =
                new LoginRequest("unknown@example.com", "password");

        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                BadCredentialsException.class,
                () -> userService.authenticate(request)
        );

        // Non viene nemmeno controllata la password.
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void passwordSbagliataVieneRifiutata() {

        LoginRequest request =
                new LoginRequest("dom@example.com", "sbagliata");

        User user = new User(
                "dom@example.com", "hash", null, null
        );

        when(userRepository.findByEmail("dom@example.com"))
                .thenReturn(Optional.of(user));

        assertThrows(
                BadCredentialsException.class,
                () -> userService.authenticate(request)
        );

        verify(passwordEncoder)
                .matches("sbagliata", "hash");
    }

    @Test
    void accountNonAttivoVieneRifiutato() {

        // Seleziona uno stato diverso da ACTIVE.
        AccountStatus nonActive = Arrays.stream(
                        AccountStatus.values()
                )
                .filter(status -> status != AccountStatus.ACTIVE)
                .findFirst()
                .orElseThrow();

        LoginRequest request =
                new LoginRequest("dom@example.com", "password");

        User user = mock(User.class);

        when(user.getPasswordHash()).thenReturn("hash");
        when(user.getAccountStatus()).thenReturn(nonActive);

        when(userRepository.findByEmail("dom@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("password", "hash"))
                .thenReturn(true);

        assertThrows(
                BadCredentialsException.class,
                () -> userService.authenticate(request)
        );
    }
}
