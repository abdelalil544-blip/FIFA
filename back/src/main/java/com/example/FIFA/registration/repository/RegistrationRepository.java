package com.example.FIFA.registration.repository;

import com.example.FIFA.registration.entity.Registration;
import com.example.FIFA.registration.enums.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, UUID> {
    Optional<Registration> findByPlayerIdAndTournamentId(UUID playerId, UUID tournamentId);
    List<Registration> findByTournamentId(UUID tournamentId);
    List<Registration> findByTournamentIdAndStatus(UUID tournamentId, RegistrationStatus status);
    boolean existsByPlayerIdAndTournamentId(UUID playerId, UUID tournamentId);
}
