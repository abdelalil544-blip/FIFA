package com.example.FIFA.registration.dto;

import com.example.FIFA.registration.enums.RegistrationStatus;
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
public class RegistrationResponse {

    private UUID id;
    private UUID playerId;
    private String playerGamertag;
    private UUID tournamentId;
    private String tournamentName;
    private RegistrationStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime reviewedAt;
    private UUID reviewedById;
    private String reviewedByUsername;
}
