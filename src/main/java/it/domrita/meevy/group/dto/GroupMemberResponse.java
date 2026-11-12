
package it.domrita.meevy.group.dto;

import it.domrita.meevy.group.role.GroupRole;

import java.time.Instant;

public record GroupMemberResponse(

        Long userId,
        String firstName,
        String lastName,
        GroupRole role,
        Instant joinedAt

) {}
