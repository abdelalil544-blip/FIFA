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
public class TournamentStatisticsResponse {

    private UUID tournamentId;
    private String tournamentName;
    private Long registeredPlayers;
    private Long confirmedPlayers;
    private Long totalMatches;
    private Long completedMatches;
    private Long pendingMatches;
    private Long disputedMatches;
    private Integer totalGoalsScored;
    private Double averageGoalsPerCompletedMatch;
    private UUID topScorerPlayerId;
    private String topScorerGamertag;
    private Integer topScorerGoals;
    private UUID topWinnerPlayerId;
    private String topWinnerGamertag;
    private Integer topWinnerWins;
}
