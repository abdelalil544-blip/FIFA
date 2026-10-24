package com.example.FIFA.player.dto;

import com.example.FIFA.player.enums.Platform;
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
public class PlayerResponse {

    private UUID id;
    private UUID userId;
    private String username;
    private String gamertag;
    private Platform platform;
    private String gameId;
    private LocalDateTime createdAt;
}
