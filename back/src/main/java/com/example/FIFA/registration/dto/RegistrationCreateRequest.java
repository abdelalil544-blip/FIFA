package com.example.FIFA.registration.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationCreateRequest {

    @NotNull(message = "L'identifiant du joueur est obligatoire")
    private UUID playerId;

    @NotNull(message = "L'identifiant du tournoi est obligatoire")
    private UUID tournamentId;
}
