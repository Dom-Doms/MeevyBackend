
package it.domrita.meevy.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GroupInvitationAcceptRequest(

        @NotBlank
        @Size(min = 43, max = 43)
        String token

) {}
