package com.example.FIFA.player.mapper;

import com.example.FIFA.player.dto.PlayerCreateRequest;
import com.example.FIFA.player.dto.PlayerResponse;
import com.example.FIFA.player.dto.PlayerUpdateRequest;
import com.example.FIFA.player.entity.Player;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PlayerMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Player toEntity(PlayerCreateRequest request);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    PlayerResponse toResponse(Player entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromRequest(PlayerUpdateRequest request, @MappingTarget Player entity);
}
