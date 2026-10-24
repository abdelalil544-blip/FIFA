package com.example.FIFA.match.dto;

import com.example.FIFA.match.enums.MatchStage;
import com.example.FIFA.match.enums.MatchStatus;
import com.example.FIFA.match.enums.NextMatchSlot;
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
public class MatchResponse {

    private UUID id;
    private UUID tournamentId;
    private UUID groupId;
    private String groupName;
    private MatchStage stage;
    private Integer roundNumber;
    private Integer matchNumber;
    private UUID player1Id;
    private String player1Gamertag;
    private UUID player2Id;
    private String player2Gamertag;
    private Integer scorePlayer1;
    private Integer scorePlayer2;
    private UUID winnerId;
    private String winnerGamertag;
    private MatchStatus status;
    private LocalDateTime scheduledAt;
    private LocalDateTime playedAt;
    private UUID nextMatchId;
    private NextMatchSlot nextMatchSlot;
}
