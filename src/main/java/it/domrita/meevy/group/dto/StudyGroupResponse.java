
package it.domrita.meevy.group.dto;

import it.domrita.meevy.group.role.GroupRole;

import java.time.Instant;

public record StudyGroupResponse(

        Long id,
        String name,
        String description,
        Instant createdAt,
        GroupRole myRole,
        long memberCount

) {}
