package it.domrita.meevy.subject.service;

import it.domrita.meevy.subject.dto.SubjectRequest;
import it.domrita.meevy.subject.dto.SubjectResponse;
//import it.domrita.meevy.subject.exception.SubjectNotFoundException;                  TODO
import it.domrita.meevy.subject.repository.SubjectRepository;
import it.domrita.meevy.subject.model.Subject;

import it.domrita.meevy.auth.repository.CurrentUserIdProvider;
import it.domrita.meevy.user.model.User;
import it.domrita.meevy.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional

public class SubjectService {
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final CurrentUserIdProvider currentUserIdProvider;

    //crea una materia associata all'utente corrente
    public SubjectResponse createSubject(SubjectRequest subjectRequest) {

        Long userId = currentUserIdProvider.getCurrentUserId();

        User user = userRepository.findById(userId) .orElseThrow(()
                -> new IllegalStateException("Utente non prsente nel database"));

        Subject subject = new Subject( subjectRequest.name().trim(), user);

        Subject savedSubject = subjectRepository.save(subject);

        return toResponse(savedSubject);
    }


    //Restituisce tutte le materie dell'utente corrente
    @Transactional(readOnly = true)
    public List<SubjectResponse> getAllSubjects() {

        long userId = currentUserIdProvider.getCurrentUserId();

        return subjectRepository.findAllByUser_IdOrderByIdAsc(userId)
                .stream().map(this::toResponse).toList();
    }


    //Recupera una materia tramite l'id, verificando la proprietà tramite l'id dell'utente

    //Converte un Entity in un DTO, ovvero riceve un oggetto Subject contenuto nel backend e lo
    //trasforma in un SubjectResponse contenente solo le informazioni utili che vogliamo inviare a Flutter
    private SubjectResponse toResponse(Subject subject) {
        return new SubjectResponse(
                subject.getId(), subject.getName()
        );
    }
}
