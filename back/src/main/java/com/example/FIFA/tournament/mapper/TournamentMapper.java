package com.example.FIFA.tournament.mapper;

import com.example.FIFA.tournament.dto.TournamentCreateRequest;
import com.example.FIFA.tournament.dto.TournamentResponse;
import com.example.FIFA.tournament.dto.TournamentUpdateRequest;
import com.example.FIFA.tournament.entity.Tournament;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TournamentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organizer", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Tournament toEntity(TournamentCreateRequest request);

    @Mapping(target = "organizerId", source = "organizer.id")
    @Mapping(target = "organizerUsername", source = "organizer.username")
    @Mapping(target = "championId", source = "champion.id")
    @Mapping(target = "championGamertag", source = "champion.gamertag")
    TournamentResponse toResponse(Tournament entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organizer", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(TournamentUpdateRequest request, @MappingTarget Tournament entity);
}
