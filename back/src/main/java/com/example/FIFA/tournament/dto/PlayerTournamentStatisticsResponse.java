package com.example.FIFA.tournament.dto;

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
public class PlayerTournamentStatisticsResponse {

    private UUID playerId;
    private String playerGamertag;
    private Integer matchesPlayed;
    private Integer wins;
    private Integer draws;
    private Integer losses;
    private Integer points;
    private Integer goalsFor;
    private Integer goalsAgainst;
    private Integer goalDifference;
    private Double winRate;
    private Integer pendingResults;
    private Integer disputedResults;
}
