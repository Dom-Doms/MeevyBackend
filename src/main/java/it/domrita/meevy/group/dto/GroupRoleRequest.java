package it.domrita.meevy.group.dto;

import it.domrita.meevy.group.role.GroupRole;
import jakarta.validation.constraints.NotNull;

public record GroupRoleRequest(

        @NotNull
        GroupRole role

) {}