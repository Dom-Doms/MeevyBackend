
package it.domrita.meevy.user.dto;

public record RegisterResponse(
        Long id,
        String email,
        String firstName,
        String lastName
) {}
