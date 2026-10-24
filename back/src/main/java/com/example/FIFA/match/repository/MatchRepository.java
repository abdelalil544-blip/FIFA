package com.example.FIFA.match.repository;

import com.example.FIFA.match.entity.Match;
import com.example.FIFA.match.enums.MatchStage;
import com.example.FIFA.match.enums.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MatchRepository extends JpaRepository<Match, UUID> {
    List<Match> findByTournamentId(UUID tournamentId);
    List<Match> findByGroupId(UUID groupId);
    List<Match> findByTournamentIdAndStage(UUID tournamentId, MatchStage stage);
    List<Match> findByTournamentIdAndStatus(UUID tournamentId, MatchStatus status);
}
