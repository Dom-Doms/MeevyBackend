
package it.domrita.meevy.common.exception;

import it.domrita.meevy.user.exception.EmailAlreadyRegisteredException;

import it.domrita.meevy.auth.exception.InvalidRefreshTokenException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    /*
     * Gestisce il tentativo di registrazione
     * con un'email già utilizzata.
     */
    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ProblemDetail handleDuplicateEmail(
            EmailAlreadyRegisteredException ex
    ) {
        return createProblem(
                HttpStatus.CONFLICT,
                "EMAIL_ALREADY_REGISTERED",
                ex.getMessage()
        );
    }

    /*
     * Gestisce gli errori di validazione dei DTO.
     * Ad esempio: email non valida o password troppo corta.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> fields = new LinkedHashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(
                    error.getField(),
                    error.getDefaultMessage()
            );
        }

        ProblemDetail problem = createProblem(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Uno o più campi non sono validi."
        );

        problem.setProperty("fields", fields);

        return problem;
    }

    /*
     * Gestisce un JSON non leggibile o malformato.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleInvalidJson() {
        return createProblem(
                HttpStatus.BAD_REQUEST,
                "INVALID_JSON",
                "Il corpo della richiesta non è valido."
        );
    }
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(
            BadCredentialsException ex
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "code", "INVALID_CREDENTIALS",
                        "message", "Email o password non valide."
                ));
    }

    /*
     * Gestisce violazioni dei vincoli del database,
     * compresi eventuali conflitti di unicità.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataConflict() {
        return createProblem(
                HttpStatus.CONFLICT,
                "DATA_CONFLICT",
                "I dati inseriti violano un vincolo del database."
        );
    }

    /*
     * Gestisce i tentativi di rinnovo con
     * refresh token non validi o scaduti.
     */
    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<Map<String, String>> handleInvalidRefreshToken(
            InvalidRefreshTokenException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "code", "INVALID_REFRESH_TOKEN",
                        "message", ex.getMessage()
                ));
    }

    /*
     * Costruisce una risposta di errore uniforme.
     */
    private ProblemDetail createProblem(
            HttpStatus status,
            String code,
            String detail
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(status, detail);

        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("code", code);

        return problem;
    }
}
