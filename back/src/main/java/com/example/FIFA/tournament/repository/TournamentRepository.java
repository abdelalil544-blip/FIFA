package com.example.FIFA.tournament.repository;

import com.example.FIFA.tournament.entity.Tournament;
import com.example.FIFA.tournament.enums.TournamentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, UUID>, JpaSpecificationExecutor<Tournament> {
    List<Tournament> findByStatus(TournamentStatus status);
    List<Tournament> findByOrganizerId(UUID organizerId);
}
