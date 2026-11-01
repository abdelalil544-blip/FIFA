package com.example.FIFA.match.dto;

import jakarta.validation.constraints.Min;
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
public class MatchForfeitRequest {

    @NotNull(message = "The forfeiting player is required")
    private UUID forfeitingPlayerId;

    @NotNull(message = "The beneficiary player is required")
    private UUID beneficiaryPlayerId;

    @Min(value = 0, message = "The score cannot be negative")
    private Integer scorePlayer1;

    @Min(value = 0, message = "The score cannot be negative")
    private Integer scorePlayer2;

    @Size(max = 500, message = "The reason cannot exceed 500 characters")
    private String reason;
}
