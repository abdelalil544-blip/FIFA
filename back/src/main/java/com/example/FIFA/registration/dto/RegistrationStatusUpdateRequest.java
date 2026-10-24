package com.example.FIFA.registration.dto;

import com.example.FIFA.registration.enums.RegistrationStatus;
import jakarta.validation.constraints.NotNull;
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
public class RegistrationStatusUpdateRequest {

    @NotNull(message = "Le statut est obligatoire")
    private RegistrationStatus status;

    @NotNull(message = "L'identifiant de la personne qui valide ou rejette est obligatoire")
    private UUID reviewedById;
}
