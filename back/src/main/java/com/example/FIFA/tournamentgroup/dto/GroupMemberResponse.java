package com.example.FIFA.tournamentgroup.dto;

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
public class GroupMemberResponse {

    private UUID id;
    private UUID tournamentId;
    private UUID groupId;
    private String groupName;
    private UUID playerId;
    private String playerGamertag;
    private Integer seed;
    private LocalDateTime joinedAt;
}
