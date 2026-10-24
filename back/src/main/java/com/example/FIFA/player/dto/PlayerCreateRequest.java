package com.example.FIFA.player.dto;

import com.example.FIFA.player.enums.Platform;
import jakarta.validation.constraints.NotBlank;
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
public class PlayerCreateRequest {

    @NotNull(message = "L'identifiant de l'utilisateur est obligatoire")
    private UUID userId;

    @NotBlank(message = "Le gamertag est obligatoire")
    @Size(min = 2, max = 50, message = "Le gamertag doit contenir entre 2 et 50 caractères")
    private String gamertag;

    @NotNull(message = "La plateforme de jeu est obligatoire")
    private Platform platform;

    @Size(max = 100, message = "L'identifiant in-game ne doit pas dépasser 100 caractères")
    private String gameId;
}
