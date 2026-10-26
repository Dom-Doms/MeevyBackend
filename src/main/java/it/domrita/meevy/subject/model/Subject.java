package it.domrita.meevy.subject.model;
import it.domrita.meevy.user.model.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "subject")
@Getter
@NoArgsConstructor

public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    /*
     * Utente proprietario della materia.
     *
     * Ogni Subject appartiene a un solo User.
     * Uno User può possedere più Subject.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Subject(String name, User user) {
        this.name = name;
        this.user = user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void rename(String newName) {
        this.name = newName;
    }
}
