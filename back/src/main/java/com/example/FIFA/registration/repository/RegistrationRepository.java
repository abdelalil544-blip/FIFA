package com.example.FIFA.registration.repository;

import com.example.FIFA.player.entity.Player;
import com.example.FIFA.registration.entity.Registration;
import com.example.FIFA.registration.enums.RegistrationStatus;
import com.example.FIFA.tournament.entity.Tournament;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, UUID> {

    Optional<Registration> findByPlayerAndTournament(Player player, Tournament tournament);

    Page<Registration> findByTournamentId(UUID tournamentId, Pageable pageable);

    Page<Registration> findByTournamentIdAndStatus(UUID tournamentId, RegistrationStatus status, Pageable pageable);

    Page<Registration> findByPlayerId(UUID playerId, Pageable pageable);

    Page<Registration> findByPlayerIdAndStatus(UUID playerId, RegistrationStatus status, Pageable pageable);

    List<Registration> findByTournamentId(UUID tournamentId);

    List<Registration> findByTournamentIdAndStatus(UUID tournamentId, RegistrationStatus status);

    long countByTournamentIdAndStatus(UUID tournamentId, RegistrationStatus status);

    boolean existsByPlayerIdAndTournamentId(UUID playerId, UUID tournamentId);
}
