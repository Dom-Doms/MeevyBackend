package it.domrita.meevy.chat.websocket;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Profile("!dev")
@RequiredArgsConstructor
public class GroupMessageNotificationListener {

    private static final Logger log =
            LoggerFactory.getLogger(
                    GroupMessageNotificationListener.class
            );

    private static final String DESTINATION =
            "/queue/group-message-notifications";

    private final GroupMessageRecipientsService recipientsService;
    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageCreated(GroupMessageCreatedEvent event) {

        try {
            for (Long userId : recipientsService.currentMemberIds(event.groupId())) {

                try {
                    messagingTemplate.convertAndSendToUser(
                            userId.toString(),
                            DESTINATION,
                            event
                    );
                } catch (RuntimeException ex) {
                    log.warn(
                            "Notifica chat non consegnata all'utente {}",
                            userId,
                            ex
                    );
                }
            }

        } catch (RuntimeException ex) {
            // Il messaggio e' gia' nel DB: un errore del WebSocket
            // non deve far fallire il POST e provocare un doppio invio.
            log.warn(
                    "Impossibile notificare il nuovo messaggio {}",
                    event.messageId(),
                    ex
            );
        }
    }
}
