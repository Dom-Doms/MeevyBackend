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
}
