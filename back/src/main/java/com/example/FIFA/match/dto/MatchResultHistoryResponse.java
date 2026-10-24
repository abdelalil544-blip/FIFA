package com.example.FIFA.match.dto;

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
public class MatchResultHistoryResponse {

    private UUID id;
    private UUID matchId;
    private Integer previousScorePlayer1;
    private Integer previousScorePlayer2;
    private Integer newScorePlayer1;
    private Integer newScorePlayer2;
    private UUID changedById;
    private String changedByUsername;
    private String reason;
    private LocalDateTime changedAt;
}
