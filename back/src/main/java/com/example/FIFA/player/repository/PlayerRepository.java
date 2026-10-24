package com.example.FIFA.player.repository;

import com.example.FIFA.player.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerRepository extends JpaRepository<Player, UUID> {
    Optional<Player> findByGamertag(String gamertag);
    Optional<Player> findByUserId(UUID userId);
    boolean existsByGamertag(String gamertag);
}
