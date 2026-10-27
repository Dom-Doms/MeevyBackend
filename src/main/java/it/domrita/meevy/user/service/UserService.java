
package it.domrita.meevy.user.service;

import it.domrita.meevy.user.dto.LoginRequest;
import it.domrita.meevy.user.dto.RegisterRequest;
import it.domrita.meevy.user.dto.RegisterResponse;
import it.domrita.meevy.user.exception.EmailAlreadyRegisteredException;
import it.domrita.meevy.user.model.User;
import it.domrita.meevy.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterResponse register(RegisterRequest request) {

        /*
         * Normalizza l'email per evitare che le differenze
         * tra maiuscole e minuscole creino account duplicati.
         */
        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        /*
         * Controlla se l'email è già registrata.
         */
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        /*
         * Genera l'hash della password.
         * La password originale non viene salvata.
         */
        String passwordHash = passwordEncoder.encode(request.password());

        /*
         * Nome e cognome sono facoltativi.
         */
        User user = new User(
                email,
                passwordHash,
                request.firstName(),
                request.lastName()
        );

        /*
         * Salva l'utente nel database.
         */
        User savedUser = userRepository.saveAndFlush(user);

        /*
         * Restituisce soltanto i dati pubblicabili
         * nella risposta di registrazione.
         */
        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName()
        );
    }


    /*
     * Verifica le credenziali inserite durante il login.
     *
     * Se sono corrette e l'account è attivo,
     * restituisce l'utente autenticato.
     *
     * La generazione dei token sarà gestita
     * separatamente dal TokenService.
     */
    public User authenticate(LoginRequest request) {

        // Normalizza l'email come nella registrazione.
        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        // Cerca l'utente nel database.
        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            throw new BadCredentialsException("Credenziali non valide.");
        }

        User user = optionalUser.get();

        // Confronta la password inserita con l'hash salvato.
        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )) {
            throw new BadCredentialsException("Credenziali non valide.");
        }

        // Gli account non attivi non possono autenticarsi.
        if (user.getAccountStatus() != it.domrita.meevy.role.AccountStatus.ACTIVE) {
            throw new BadCredentialsException("Credenziali non valide.");
        }

        return user;
    }

}
