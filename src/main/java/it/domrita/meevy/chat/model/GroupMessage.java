package it.domrita.meevy.chat.model;
import it.domrita.meevy.group.model.StudyGroup;
import it.domrita.meevy.user.model.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "group_message")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private StudyGroup group;

    // Puntiamo allo User, non alla membership: se uno esce, i messaggi restano.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_user_id", nullable = false)
    private User sender;

    @Column(nullable = false, length = 4000)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public GroupMessage(StudyGroup group, User sender, String content) {
        this.group = group;
        this.sender = sender;
        this.content = content;
    }

    @PrePersist
    private void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}