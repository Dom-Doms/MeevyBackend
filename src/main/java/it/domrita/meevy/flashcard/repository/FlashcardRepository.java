package it.domrita.meevy.flashcard.repository;
import it.domrita.meevy.flashcard.model.Flashcard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FlashcardRepository extends JpaRepository<Flashcard, Long> {


    List<Flashcard> findAllByUser_IdOrderByIdAsc(Long userId);

    List<Flashcard> findAllByUser_IdAndSubject_IdOrderByIdAsc(Long userId, Long subjectId);

    Optional<Flashcard> findByIdAndUser_Id(Long id, Long userId);


}
