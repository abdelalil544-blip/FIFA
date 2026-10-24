package com.example.FIFA.tournamentgroup.repository;

import com.example.FIFA.tournamentgroup.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {
    List<GroupMember> findByGroupId(UUID groupId);
    List<GroupMember> findByTournamentId(UUID tournamentId);
    Optional<GroupMember> findByTournamentIdAndPlayerId(UUID tournamentId, UUID playerId);
    boolean existsByTournamentIdAndPlayerId(UUID tournamentId, UUID playerId);
}
