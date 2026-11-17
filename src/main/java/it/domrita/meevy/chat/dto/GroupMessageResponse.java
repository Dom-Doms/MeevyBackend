package it.domrita.meevy.chat.dto;

import java.time.Instant;

public record GroupMessageResponse(
        Long id,
        Long groupId,
        Long senderUserId,
        String senderFirstName,
        String senderLastName,
        String content,
        Instant createdAt
) {}