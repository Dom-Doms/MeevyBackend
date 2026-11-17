package it.domrita.meevy.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GroupMessageRequest(
        @NotBlank(message = "Scrivi un messaggio prima di inviarlo.")
        @Size(max = 4000, message = "Il messaggio non può superare 4000 caratteri.")
        String content
) {}