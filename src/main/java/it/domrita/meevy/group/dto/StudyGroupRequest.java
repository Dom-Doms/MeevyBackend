
package it.domrita.meevy.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudyGroupRequest(

        @NotBlank
        @Size(max = 150)
        String name,

        @Size(max = 500)
        String description

) {}
