
package it.domrita.meevy.group.dto;

import java.time.Instant;

public record GroupInvitationResponse(

        Long groupId,
        String token,
        Instant expiresAt

) {}
