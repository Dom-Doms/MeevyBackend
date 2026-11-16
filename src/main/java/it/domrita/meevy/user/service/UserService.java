
package it.domrita.meevy.user.service;

import it.domrita.meevy.auth.dto.LoginRequest;
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



import it.domrita.meevy.auth.repository.CurrentUserIdProvider;
import it.domrita.meevy.user.dto.UserProfileResponse;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserIdProvider currentUserIdProvider;

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
     * Recupera il profilo dell'utente autenticato.
     *
     * L'identificativo viene ottenuto dal JWT già
     * verificato da Spring Security.
     *
     * Non riceviamo l'ID dal client, perché un utente
     * non deve poter richiedere il profilo di un altro.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentProfile() {

        Long userId = currentUserIdProvider.getCurrentUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Utente autenticato non trovato."
                ));

        // Convertiamo l'Entity nei soli dati esposti al frontend.
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPlatformRole(),
                user.getAccountStatus()
        );
    }


}
