package com.example.FIFA.tournamentgroup.repository;

import com.example.FIFA.tournamentgroup.entity.TournamentGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TournamentGroupRepository extends JpaRepository<TournamentGroup, UUID> {
    List<TournamentGroup> findByTournamentIdOrderByPositionAsc(UUID tournamentId);
    Optional<TournamentGroup> findByTournamentIdAndName(UUID tournamentId, String name);
    Optional<TournamentGroup> findByTournamentIdAndPosition(UUID tournamentId, Integer position);
}
