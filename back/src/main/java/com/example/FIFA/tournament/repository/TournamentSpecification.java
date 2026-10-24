package com.example.FIFA.tournament.repository;

import com.example.FIFA.player.enums.Platform;
import com.example.FIFA.tournament.entity.Tournament;
import com.example.FIFA.tournament.enums.TournamentFormat;
import com.example.FIFA.tournament.enums.TournamentStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class TournamentSpecification {

    public static Specification<Tournament> withFilters(
            TournamentStatus status,
            TournamentFormat format,
            Platform platform,
            UUID organizerId,
            String search
    ) {
        return Specification
                .where(hasStatus(status))
                .and(hasFormat(format))
                .and(hasPlatform(platform))
                .and(hasOrganizer(organizerId))
                .and(containsSearch(search));
    }

    public static Specification<Tournament> hasStatus(TournamentStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Tournament> hasFormat(TournamentFormat format) {
        return (root, query, cb) -> format == null ? null : cb.equal(root.get("format"), format);
    }

    public static Specification<Tournament> hasPlatform(Platform platform) {
        return (root, query, cb) -> platform == null ? null : cb.equal(root.get("platform"), platform);
    }

    public static Specification<Tournament> hasOrganizer(UUID organizerId) {
        return (root, query, cb) -> organizerId == null ? null : cb.equal(root.get("organizer").get("id"), organizerId);
    }

    public static Specification<Tournament> containsSearch(String search) {
        return (root, query, cb) -> {
            if (search == null || search.trim().isEmpty()) {
                return null;
            }
            String pattern = "%" + search.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("gameTitle")), pattern)
            );
        };
    }
}
