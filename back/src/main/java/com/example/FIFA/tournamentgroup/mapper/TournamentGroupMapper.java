package com.example.FIFA.tournamentgroup.mapper;

import com.example.FIFA.tournamentgroup.dto.GroupMemberCreateRequest;
import com.example.FIFA.tournamentgroup.dto.GroupMemberResponse;
import com.example.FIFA.tournamentgroup.dto.TournamentGroupCreateRequest;
import com.example.FIFA.tournamentgroup.dto.TournamentGroupResponse;
import com.example.FIFA.tournamentgroup.entity.GroupMember;
import com.example.FIFA.tournamentgroup.entity.TournamentGroup;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TournamentGroupMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tournament", ignore = true)
    TournamentGroup toEntity(TournamentGroupCreateRequest request);

    @Mapping(target = "tournamentId", source = "tournament.id")
    @Mapping(target = "tournamentName", source = "tournament.name")
    TournamentGroupResponse toResponse(TournamentGroup entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tournament", ignore = true)
    @Mapping(target = "group", ignore = true)
    @Mapping(target = "player", ignore = true)
    @Mapping(target = "joinedAt", ignore = true)
    GroupMember toEntity(GroupMemberCreateRequest request);

    @Mapping(target = "tournamentId", source = "tournament.id")
    @Mapping(target = "groupId", source = "group.id")
    @Mapping(target = "groupName", source = "group.name")
    @Mapping(target = "playerId", source = "player.id")
    @Mapping(target = "playerGamertag", source = "player.gamertag")
    GroupMemberResponse toResponse(GroupMember entity);
}
