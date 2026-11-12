
package it.domrita.meevy.group.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "study_group")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //Nome del gruppo di studio.

    @Column(nullable = false, length = 150)
    private String name;


    @Column(length = 500)
    private String description;

    /*
     * Data di creazione del gruppo.
     * Non può essere modificata successivamente.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    //Imposta la data prima del primo salvataggio.

    @PrePersist
    private void onCreate() {
        createdAt = Instant.now();
    }

    // Crea un nuovo gruppo di studio. L'appartenenza del creatore con ruolo OWNER è Service.

    public StudyGroup(String name, String description) {
        this.name = name;
        this.description = description;
    }


    public void updateDetails(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
