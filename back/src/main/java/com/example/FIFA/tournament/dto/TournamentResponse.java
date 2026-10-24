package com.example.FIFA.tournament.dto;

import com.example.FIFA.player.enums.Platform;
import com.example.FIFA.tournament.enums.TournamentFormat;
import com.example.FIFA.tournament.enums.TournamentStatus;
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
public class TournamentResponse {

    private UUID id;
    private String name;
    private String description;
    private String gameTitle;
    private Platform platform;
    private TournamentFormat format;
    private TournamentStatus status;
    private Integer maxPlayers;
    private Integer playersPerGroup;
    private Integer qualifiersPerGroup;
    private LocalDateTime registrationStart;
    private LocalDateTime registrationEnd;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private UUID organizerId;
    private String organizerUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
