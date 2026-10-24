package com.example.FIFA.player.dto;

import com.example.FIFA.player.enums.Platform;
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
public class PlayerUpdateRequest {

    @Size(min = 2, max = 50, message = "Le gamertag doit contenir entre 2 et 50 caractères")
    private String gamertag;

    private Platform platform;

    @Size(max = 100, message = "L'identifiant in-game ne doit pas dépasser 100 caractères")
    private String gameId;
}
