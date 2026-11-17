package it.domrita.meevy.chat.service;

import it.domrita.meevy.auth.repository.CurrentUserIdProvider;
import it.domrita.meevy.group.exception.GroupNotFoundException;
import it.domrita.meevy.chat.dto.GroupMessageRequest;
import it.domrita.meevy.chat.dto.GroupMessageResponse;
import it.domrita.meevy.chat.exception.InvalidGroupMessageException;
import it.domrita.meevy.chat.model.GroupMessage;
import it.domrita.meevy.chat.repository.GroupMessageRepository;
import it.domrita.meevy.group.model.StudyGroup;
import it.domrita.meevy.group.repository.GroupMembershipRepository;
import it.domrita.meevy.group.repository.StudyGroupRepository;
import it.domrita.meevy.user.model.User;
import it.domrita.meevy.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

//websocket
import it.domrita.meevy.chat.websocket.GroupMessageCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;

@Service
@RequiredArgsConstructor
public class GroupMessageService {

    private final GroupMessageRepository groupMessageRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final StudyGroupRepository studyGroupRepository;
    private final UserRepository userRepository;
    private final CurrentUserIdProvider currentUserIdProvider;
    //wwbsocket
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public GroupMessageResponse sendMessage(Long groupId, GroupMessageRequest request) {

        // Il Controller valida già il JSON, ma il Service potrebbe essere usato
        // anche dal WebSocket: non ci fidiamo solo delle annotazioni sul DTO.
        String content = request == null || request.content() == null
                ? ""
                : request.content().strip();

        if (content.isBlank() || content.length() > 4000) {
            throw new InvalidGroupMessageException(
                    "Il testo del messaggio non è valido."
            );
        }

        Long userId = currentUserIdProvider.getCurrentUserId();

        // Il lock è lo stesso usato per uscita ed espulsione dal gruppo.
        // Così non si può perdere la membership a metà dell'invio.
        StudyGroup group = studyGroupRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new GroupNotFoundException(groupId));

        requireMembership(groupId, userId);

        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException(
                        "Utente autenticato non presente nel database."
                ));

        GroupMessage message = new GroupMessage(
                group,
                sender,
                content
        );

        GroupMessage saved = groupMessageRepository.saveAndFlush(message);

        // Sara' notificato solo se la transazione termina correttamente.
        eventPublisher.publishEvent(
                new GroupMessageCreatedEvent(groupId, saved.getId())
        );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<GroupMessageResponse> getMessages(
            Long groupId,
            Long beforeId,
            int limit
    ) {

        if (limit < 1 || limit > 100) {
            throw new InvalidGroupMessageException(
                    "Il limite deve essere compreso tra 1 e 100."
            );
        }

        if (beforeId != null && beforeId <= 0) {
            throw new InvalidGroupMessageException(
                    "beforeId deve essere positivo."
            );
        }

        Long userId = currentUserIdProvider.getCurrentUserId();

        //controllo per verificare che l'utente faccia parte del gruppo
        requireMembership(groupId, userId);

        Pageable page = PageRequest.of(0, limit);

        List<GroupMessage> messages = beforeId == null
                ? groupMessageRepository.findByGroup_IdOrderByIdDesc(
                groupId,
                page
        )
                : groupMessageRepository.findByGroup_IdAndIdLessThanOrderByIdDesc(
                groupId,
                beforeId,
                page
        );

        // Più recenti prima: Flutter può invertirli per mostrarli dal basso.
        return messages.stream()
                .map(this::toResponse)
                .toList();
    }

    private void requireMembership(Long groupId, Long userId) {

        if (!groupMembershipRepository.existsByGroup_IdAndUser_Id(
                groupId,
                userId
        )) {
            // Stesso 404 sia per gruppo inesistente sia per gruppo non accessibile.
            throw new GroupNotFoundException(groupId);
        }
    }

    private GroupMessageResponse toResponse(GroupMessage message) {

        User sender = message.getSender();

        return new GroupMessageResponse(
                message.getId(),
                message.getGroup().getId(),
                sender.getId(),
                sender.getFirstName(),
                sender.getLastName(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}


/*
Immagina questa situazione:
1. Thread A: Dom sta inviando un messaggio.
2. Thread B: Rita, che è Owner, sta espellendo Dom dal gruppo.
Senza lock potrebbe succedere che:
- Il Thread A verifica che Dom appartiene al gruppo.
- Il Thread B espelle Dom.
- Il Thread A salva comunque il messaggio, nonostante Dom sia già stato espulso.
Cosa cambia con il lock?
StudyGroup group = studyGroupRepository.findByIdForUpdate(groupId)
        .orElseThrow(() -> new GroupNotFoundException(groupId));


findByIdForUpdate() utilizza PESSIMISTIC_WRITE, cioè richiede a PostgreSQL di bloccare la riga del gruppo durante la transazione.

Se Dom inizia prima:
1. Il Thread A acquisisce il lock.
2. Verifica che Dom sia membro.
3. Salva il messaggio.
4. Termina la transazione e rilascia il lock.
5. Soltanto allora il Thread B può proseguire con l'espulsione.
Se Rita acquisisce il lock per prima, Dom verrà espulso prima che il Thread A verifichi la membership, quindi il messaggio sarà rifiutato.
Perché blocchiamo il gruppo e non il messaggio?
Perché sia l'invio sia l'espulsione riguardano lo stesso gruppo, e il vostro StudyGroupService usa già questo lock per espellere i membri.

In pratica, il gruppo diventa il punto comune di sincronizzazione.

Attenzione: il lock funziona come previsto perché sendMessage() è @Transactional e perché anche l'espulsione acquisisce lo stesso lock prima di modificare la membership.

Non è il lock da solo a impedire l'invio: è la combinazione lock → verifica membership → salvataggio.
 */