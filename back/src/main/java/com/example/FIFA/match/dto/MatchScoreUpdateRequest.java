package com.example.FIFA.match.dto;

import com.example.FIFA.match.enums.MatchStatus;
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
public class MatchScoreUpdateRequest {

    @NotNull(message = "Le score du joueur 1 est obligatoire")
    @Min(value = 0, message = "Le score ne peut pas être négatif")
    private Integer scorePlayer1;

    @NotNull(message = "Le score du joueur 2 est obligatoire")
    @Min(value = 0, message = "Le score ne peut pas être négatif")
    private Integer scorePlayer2;

    private UUID winnerId;

    @NotNull(message = "Le statut du match est obligatoire")
    private MatchStatus status;

    @NotNull(message = "L'identifiant de l'utilisateur effectuant la modification est obligatoire")
    private UUID changedById;

    @Size(max = 500, message = "La raison ne doit pas dépasser 500 caractères")
    private String reason;
}
