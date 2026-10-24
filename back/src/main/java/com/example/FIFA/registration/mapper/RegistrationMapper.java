package com.example.FIFA.registration.mapper;

import com.example.FIFA.registration.dto.RegistrationCreateRequest;
import com.example.FIFA.registration.dto.RegistrationResponse;
import com.example.FIFA.registration.entity.Registration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RegistrationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "player", ignore = true)
    @Mapping(target = "tournament", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "requestedAt", ignore = true)
    @Mapping(target = "reviewedAt", ignore = true)
    @Mapping(target = "reviewedBy", ignore = true)
    Registration toEntity(RegistrationCreateRequest request);

    @Mapping(target = "playerId", source = "player.id")
    @Mapping(target = "playerGamertag", source = "player.gamertag")
    @Mapping(target = "tournamentId", source = "tournament.id")
    @Mapping(target = "tournamentName", source = "tournament.name")
    @Mapping(target = "reviewedById", source = "reviewedBy.id")
    @Mapping(target = "reviewedByUsername", source = "reviewedBy.username")
    RegistrationResponse toResponse(Registration entity);
}
