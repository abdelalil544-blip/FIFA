package com.example.FIFA.tournament.dto;

import com.example.FIFA.player.enums.Platform;
import com.example.FIFA.tournament.enums.TournamentFormat;
import com.example.FIFA.tournament.enums.TournamentStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentCreateRequest {

    @NotBlank(message = "Le nom du tournoi est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom du tournoi doit comporter entre 2 et 100 caractères")
    private String name;

    @Size(max = 2000, message = "La description ne doit pas dépasser 2000 caractères")
    private String description;

    @Size(max = 100, message = "Le titre du jeu ne doit pas dépasser 100 caractères")
    private String gameTitle;

    private Platform platform;

    @NotNull(message = "Le format du tournoi est obligatoire")
    private TournamentFormat format;

    private TournamentStatus status;

    @NotNull(message = "Le nombre maximum de joueurs est obligatoire")
    @Positive(message = "Le nombre de joueurs doit être positif")
    @Min(value = 2, message = "Un tournoi doit comporter au moins 2 joueurs")
    private Integer maxPlayers;

    @Positive(message = "Le nombre de joueurs par groupe doit être positif")
    private Integer playersPerGroup;

    @Positive(message = "Le nombre de qualifiés par groupe doit être positif")
    private Integer qualifiersPerGroup;

    @NotNull(message = "La date de début des inscriptions est obligatoire")
    private LocalDateTime registrationStart;

    @NotNull(message = "La date de fin des inscriptions est obligatoire")
    private LocalDateTime registrationEnd;

    @NotNull(message = "La date de début du tournoi est obligatoire")
    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @NotNull(message = "L'identifiant de l'organisateur est obligatoire")
    private UUID organizerId;
}
