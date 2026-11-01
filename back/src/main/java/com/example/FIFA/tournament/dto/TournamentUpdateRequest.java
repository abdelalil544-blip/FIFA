package com.example.FIFA.tournament.dto;

import com.example.FIFA.player.enums.Platform;
import com.example.FIFA.tournament.enums.RoundRobinFormat;
import com.example.FIFA.tournament.enums.TournamentFormat;
import com.example.FIFA.tournament.enums.TournamentStatus;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentUpdateRequest {

    @Size(min = 2, max = 100, message = "Le nom du tournoi doit comporter entre 2 et 100 caractères")
    private String name;

    @Size(max = 2000, message = "La description ne doit pas dépasser 2000 caractères")
    private String description;

    @Size(max = 100, message = "Le titre du jeu ne doit pas dépasser 100 caractères")
    private String gameTitle;

    private Platform platform;

    private TournamentFormat format;

    private RoundRobinFormat roundRobinFormat;

    private TournamentStatus status;

    @Positive(message = "Le nombre de joueurs doit être positif")
    private Integer maxPlayers;

    @Positive(message = "Le nombre de joueurs par groupe doit être positif")
    private Integer playersPerGroup;

    @Positive(message = "Le nombre de qualifiés par groupe doit être positif")
    private Integer qualifiersPerGroup;

    private LocalDateTime registrationStart;

    private LocalDateTime registrationEnd;

    private LocalDateTime startDate;

    private LocalDateTime endDate;
}
