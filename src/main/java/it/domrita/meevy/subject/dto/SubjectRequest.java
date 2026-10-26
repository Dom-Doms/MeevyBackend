package it.domrita.meevy.subject.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubjectRequest (
        @NotBlank
        @Size(max = 150)
        String name

){ }
