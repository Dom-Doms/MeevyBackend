package it.domrita.meevy.flashcard.model;

import it.domrita.meevy.subject.model.Subject;
import it.domrita.meevy.user.model.User;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "flashcard")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)

public class Flashcard {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    //Imposta le date alla creazione.
    @PrePersist //Viene eseguito prima dell'inserimento nel database
    private void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    //Aggiorna la flashcard quando viene modificata
    @PreUpdate //Viene eseguito prima dell'aggiornamento
    private void onUpdate() {
        updatedAt = Instant.now();
    }

    //crea uan nuova flashcard
    public Flashcard(User user, Subject subject, String question, String answer) {
        this.user = user;
        this.subject = subject;
        this.question = question;
        this.answer = answer;
    }

    public void updateContent(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }
}
