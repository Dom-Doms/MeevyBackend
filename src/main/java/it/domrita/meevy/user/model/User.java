package it.domrita.meevy.user.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import it.domrita.meevy.role.PlatformRole ;
import it.domrita.meevy.role.AccountStatus ;
import java.time.Instant;

@Entity

/*
 * Vincolo di unicità sull'email.
 *
 * Impedisce che due utenti vengano registrati
 * con lo stesso indirizzo email.
 *
 * "uk_user_email" è il nome del vincolo UNIQUE
 * definito nel database.
 */
@Table(
        name = "user",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_email",
                        columnNames = "email"
                )
        }
)
@Getter
@NoArgsConstructor
public class User {

    /*
     * Identificatore interno dell'utente.
     * Viene generato automaticamente dal database.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Email dell'utente.
     *
     * È obbligatoria e univoca perché identifica
     * l'account all'interno della piattaforma.
     *
     * TODO:
     * - normalizzare l'email, ad esempio convertendola in lowercase;
     * - validarne il formato durante registrazione/modifica.
     */
    @Column(nullable = false, length = 254)
    private String email;

    /*
     * Password già sottoposta ad hashing.
     *
     * Non deve mai contenere la password in chiaro.
     *
     * TODO:
     * - configurare PasswordEncoder tramite Spring Security;
     * - eseguire l'hashing prima della creazione dello User.
     */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /*
     * Nome reale dell'utente.
     *
     * Non è univoco: più utenti possono naturalmente
     * avere lo stesso nome e cognome.
     */
    @Column(name = "first_name", nullable = true, length = 100)
    private String firstName;

    /*
     * Cognome reale dell'utente.
     */
    @Column(name = "last_name", nullable = true, length = 100)
    private String lastName;

    /*
     * Numero di telefono opzionale.
     *
     * Al momento non viene utilizzato per login
     * o verifica dell'identità.
     *
     * TODO:
     * - definire formato e normalizzazione;
     * - valutare eventualmente una verifica futura.
     */
    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform_role", nullable = false, length = 20)
    private PlatformRole platformRole = PlatformRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    private void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /*
     * Costruttore utilizzato durante la registrazione.
     *
     * id e createdAt vengono generati automaticamente.
     * Il numero di telefono può essere aggiunto in seguito.
     */
    public User(
            String email,
            String passwordHash,
            String firstName,
            String lastName
    ) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.platformRole = PlatformRole.USER;
        this.accountStatus = AccountStatus.ACTIVE;
    }
}