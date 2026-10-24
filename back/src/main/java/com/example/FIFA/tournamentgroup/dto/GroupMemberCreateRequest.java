package com.example.FIFA.tournamentgroup.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class GroupMemberCreateRequest {

    @NotNull(message = "L'identifiant du tournoi est obligatoire")
    private UUID tournamentId;

    @NotNull(message = "L'identifiant du groupe est obligatoire")
    private UUID groupId;

    @NotNull(message = "L'identifiant du joueur est obligatoire")
    private UUID playerId;

    @Positive(message = "La tête de série doit être un entier positif")
    private Integer seed;
}
