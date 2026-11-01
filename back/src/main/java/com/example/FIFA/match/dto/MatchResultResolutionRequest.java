package com.example.FIFA.match.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchResultResolutionRequest {

    @NotNull(message = "The decision to approve or reject the dispute is required")
    private Boolean approved;

    @Min(value = 0, message = "The score cannot be negative")
    private Integer scorePlayer1;

    @Min(value = 0, message = "The score cannot be negative")
    private Integer scorePlayer2;

    @Size(max = 500, message = "The reason cannot exceed 500 characters")
    private String reason;
}
