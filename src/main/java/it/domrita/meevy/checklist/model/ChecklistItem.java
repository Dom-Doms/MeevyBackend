package it.domrita.meevy.checklist.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "checklist_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ogni attività appartiene a una checklist.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checklist_id", nullable = false)
    private Checklist checklist;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(nullable = false)
    private boolean completed;

    public ChecklistItem(Checklist checklist, String text) {
        this.checklist = checklist;
        this.text = text;
        this.completed = false;
    }

    // Modifica il testo dell'attività.
    public void updateText(String text) {
        this.text = text;
    }

    // Permette di spuntare o deselezionare l'attività.
    public void setCompleted(boolean completed) {
        this.completed = completed;}


}