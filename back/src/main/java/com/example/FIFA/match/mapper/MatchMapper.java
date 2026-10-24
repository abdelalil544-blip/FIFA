package com.example.FIFA.match.mapper;

import com.example.FIFA.match.dto.MatchCreateRequest;
import com.example.FIFA.match.dto.MatchResponse;
import com.example.FIFA.match.dto.MatchResultHistoryResponse;
import com.example.FIFA.match.entity.Match;
import com.example.FIFA.match.entity.MatchResultHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MatchMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tournament", ignore = true)
    @Mapping(target = "group", ignore = true)
    @Mapping(target = "player1", ignore = true)
    @Mapping(target = "player2", ignore = true)
    @Mapping(target = "scorePlayer1", ignore = true)
    @Mapping(target = "scorePlayer2", ignore = true)
    @Mapping(target = "winner", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "playedAt", ignore = true)
    @Mapping(target = "nextMatch", ignore = true)
    Match toEntity(MatchCreateRequest request);

    @Mapping(target = "tournamentId", source = "tournament.id")
    @Mapping(target = "groupId", source = "group.id")
    @Mapping(target = "groupName", source = "group.name")
    @Mapping(target = "player1Id", source = "player1.id")
    @Mapping(target = "player1Gamertag", source = "player1.gamertag")
    @Mapping(target = "player2Id", source = "player2.id")
    @Mapping(target = "player2Gamertag", source = "player2.gamertag")
    @Mapping(target = "winnerId", source = "winner.id")
    @Mapping(target = "winnerGamertag", source = "winner.gamertag")
    @Mapping(target = "nextMatchId", source = "nextMatch.id")
    MatchResponse toResponse(Match entity);

    @Mapping(target = "matchId", source = "match.id")
    @Mapping(target = "changedById", source = "changedBy.id")
    @Mapping(target = "changedByUsername", source = "changedBy.username")
    MatchResultHistoryResponse toResponse(MatchResultHistory entity);
}
