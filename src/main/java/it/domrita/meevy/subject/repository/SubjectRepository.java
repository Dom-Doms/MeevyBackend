package it.domrita.meevy.subject.repository;

import it.domrita.meevy.subject.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;



public interface SubjectRepository
            extends JpaRepository<Subject, Long> {

    //Recupera tutte le materie di un utente
    List<Subject> findAllByUser_IdOrderByIdAsc(Long userId);


    //Cerca una materia con quel Id, ma solo se appartiene all'utente indicato
    Optional<Subject> findByIdAndUser_Id(Long id, Long userId);


}
