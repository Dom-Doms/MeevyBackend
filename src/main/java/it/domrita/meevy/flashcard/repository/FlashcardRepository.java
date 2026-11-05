package it.domrita.meevy.flashcard.repository;
import it.domrita.meevy.flashcard.model.Flashcard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlashcardRepository extends JpaRepository<Flashcard, Long> {


    List<Flashcard> findAllByUser_IdOrderByIdAsc(Long userId);


}
