package com.example.FIFA.tournamentgroup.dto;

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
public class TournamentGroupResponse {

    private UUID id;
    private String name;
    private UUID tournamentId;
    private String tournamentName;
    private Integer position;
}
