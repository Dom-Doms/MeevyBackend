package it.domrita.meevy.chat.websocket;


// Segnala un nuovo messaggio. Il testo viene recuperato via REST.
public record GroupMessageCreatedEvent(
        Long groupId,
        Long messageId
) {}
