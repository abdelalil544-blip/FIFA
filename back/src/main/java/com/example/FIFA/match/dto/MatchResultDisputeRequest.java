package com.example.FIFA.match.dto;

import jakarta.validation.constraints.NotBlank;
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
public class MatchResultDisputeRequest {

    @NotBlank(message = "A reason is required to dispute the score")
    @Size(max = 500, message = "The dispute reason cannot exceed 500 characters")
    private String reason;
}
