package it.domrita.meevy.checklist.model;

import it.domrita.meevy.subject.model.Subject;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "checklist")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Checklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ogni materia può avere una sola checklist.
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false, unique = true)
    private Subject subject;

    public Checklist(Subject subject) {
        this.subject = subject;
    }
}