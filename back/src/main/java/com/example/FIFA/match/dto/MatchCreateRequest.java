package com.example.FIFA.match.dto;

import com.example.FIFA.match.enums.MatchStage;
import com.example.FIFA.match.enums.NextMatchSlot;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
public class MatchCreateRequest {

    @NotNull(message = "L'identifiant du tournoi est obligatoire")
    private UUID tournamentId;

    private UUID groupId;

    @NotNull(message = "L'étape du match est obligatoire")
    private MatchStage stage;

    @NotNull(message = "Le numéro de round est obligatoire")
    @Min(value = 1, message = "Le round doit être supérieur ou égal à 1")
    private Integer roundNumber;

    @NotNull(message = "Le numéro de match est obligatoire")
    @Min(value = 1, message = "Le match doit être supérieur ou égal à 1")
    private Integer matchNumber;

    private UUID player1Id;

    private UUID player2Id;

    private LocalDateTime scheduledAt;

    private UUID nextMatchId;

    private NextMatchSlot nextMatchSlot;
}
