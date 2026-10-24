package com.example.FIFA.tournamentgroup.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class TournamentGroupCreateRequest {

    @NotBlank(message = "Le nom du groupe est obligatoire")
    @Size(max = 50, message = "Le nom du groupe ne doit pas dépasser 50 caractères")
    private String name;

    @NotNull(message = "L'identifiant du tournoi est obligatoire")
    private UUID tournamentId;

    @NotNull(message = "Le numéro de position du groupe est obligatoire")
    @Min(value = 1, message = "La position du groupe doit être supérieure ou égale à 1")
    private Integer position;
}
