
package it.domrita.meevy.group.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GroupOwnerTransferRequest(

        @NotNull
        @Positive
        Long newOwnerUserId

) {}
