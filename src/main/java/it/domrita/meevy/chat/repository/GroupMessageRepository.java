package it.domrita.meevy.chat.repository;

import it.domrita.meevy.chat.model.GroupMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupMessageRepository extends JpaRepository<GroupMessage, Long> {

    // Il mittente arriva già caricato: niente query aggiuntiva per ogni messaggio.
    @EntityGraph(attributePaths = "sender") //@EntityGraph serve a recuperare anche il mittente senza produrre una query aggiuntiva per ciascun messaggio.
    List<GroupMessage> findByGroup_IdOrderByIdDesc(
            Long groupId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "sender")
    List<GroupMessage> findByGroup_IdAndIdLessThanOrderByIdDesc(
            Long groupId,
            Long beforeId,
            Pageable pageable
    );
}

/*
non utilizziamo paginazione classica basata su page=0, page=1, page=2: in una chat arrivano continuamente nuovi messaggi e ciò può spostare i risultati delle pagine.

Usiamo invece beforeId.

Esempio: se i messaggi hanno ID 100, 99, 98, 97 e 96, chiedendo limit=3 riceviamo 100, 99 e 98. La richiesta successiva userà beforeId=98 per recuperare 97 e 96.
 */