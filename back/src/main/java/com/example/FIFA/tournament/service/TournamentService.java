package com.example.FIFA.tournament.service;

import com.example.FIFA.common.exception.BusinessException;
import com.example.FIFA.common.exception.ResourceNotFoundException;
import com.example.FIFA.common.response.PageResponse;
import com.example.FIFA.player.enums.Platform;
import com.example.FIFA.security.user.CustomUserDetails;
import com.example.FIFA.tournament.dto.TournamentCreateRequest;
import com.example.FIFA.tournament.dto.TournamentResponse;
import com.example.FIFA.tournament.dto.TournamentUpdateRequest;
import com.example.FIFA.tournament.entity.Tournament;
import com.example.FIFA.tournament.enums.TournamentFormat;
import com.example.FIFA.tournament.enums.TournamentStatus;
import com.example.FIFA.tournament.mapper.TournamentMapper;
import com.example.FIFA.tournament.repository.TournamentRepository;
import com.example.FIFA.user.entity.User;
import com.example.FIFA.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final UserRepository userRepository;
    private final TournamentMapper tournamentMapper;

    @Transactional
    public TournamentResponse createTournament(TournamentCreateRequest request, CustomUserDetails currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Authentication is required to create a tournament.");
        }

        validateDates(request.getRegistrationStart(), request.getRegistrationEnd(), request.getStartDate(),
                request.getEndDate());
        validateFormatRules(request.getFormat(), request.getMaxPlayers(), request.getPlayersPerGroup(),
                request.getQualifiersPerGroup());

        User organizer;
        boolean isAdmin = isAdmin(currentUser);
        if (isAdmin && request.getOrganizerId() != null) {
            organizer = userRepository.findById(request.getOrganizerId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Organizer not found with id: " + request.getOrganizerId()));
        } else {
            organizer = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found in the database."));
        }

        Tournament tournament = tournamentMapper.toEntity(request);
        tournament.setOrganizer(organizer);
        if (tournament.getStatus() == null) {
            tournament.setStatus(TournamentStatus.DRAFT);
        }

        Tournament saved = tournamentRepository.save(tournament);
        log.info("Tournament '{}' (ID: {}) created successfully by user '{}'", saved.getName(), saved.getId(),
                organizer.getUsername());
        return tournamentMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public TournamentResponse getTournamentById(UUID id) {
        Tournament tournament = findTournamentById(id);
        return tournamentMapper.toResponse(tournament);
    }

    @Transactional(readOnly = true)
    public PageResponse<TournamentResponse> getTournaments(
            TournamentStatus status,
            TournamentFormat format,
            Platform platform,
            UUID organizerId,
            String search,
            Pageable pageable) {

        Specification<Tournament> spec = buildTournamentSpecification(status, format, platform, organizerId, search);
        Page<Tournament> tournamentPage = tournamentRepository.findAll(spec, pageable);
        Page<TournamentResponse> responsePage = tournamentPage.map(tournamentMapper::toResponse);
        return PageResponse.from(responsePage);
    }

    private Specification<Tournament> buildTournamentSpecification(
            TournamentStatus status,
            TournamentFormat format,
            Platform platform,
            UUID organizerId,
            String search) {

        return Specification.where(hasStatus(status))
                .and(hasFormat(format))
                .and(hasPlatform(platform))
                .and(hasOrganizer(organizerId))
                .and(containsSearch(search));
    }

    private Specification<Tournament> hasStatus(TournamentStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    private Specification<Tournament> hasFormat(TournamentFormat format) {
        return (root, query, cb) -> format == null ? null : cb.equal(root.get("format"), format);
    }

    private Specification<Tournament> hasPlatform(Platform platform) {
        return (root, query, cb) -> platform == null ? null : cb.equal(root.get("platform"), platform);
    }

    private Specification<Tournament> hasOrganizer(UUID organizerId) {
        return (root, query, cb) -> organizerId == null ? null : cb.equal(root.get("organizer").get("id"), organizerId);
    }

    private Specification<Tournament> containsSearch(String search) {
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

    @Transactional
    public TournamentResponse updateTournament(UUID id, TournamentUpdateRequest request,
            CustomUserDetails currentUser) {
        Tournament tournament = findTournamentById(id);
        verifyOwnershipOrAdmin(tournament, currentUser);

        boolean isImmutable = tournament.getStatus() == TournamentStatus.GROUP_STAGE
                || tournament.getStatus() == TournamentStatus.PLAYOFFS
                || tournament.getStatus() == TournamentStatus.COMPLETED
                || tournament.getStatus() == TournamentStatus.CANCELLED;

        if (isImmutable) {
            if (request.getFormat() != null || request.getMaxPlayers() != null
                    || request.getPlayersPerGroup() != null || request.getQualifiersPerGroup() != null
                    || request.getRegistrationStart() != null || request.getRegistrationEnd() != null) {
                throw new BusinessException(
                        "It is not possible to modify the structural parameters of a tournament that is in progress, completed, or cancelled.");
            }
        }

        LocalDateTime regStart = request.getRegistrationStart() != null ? request.getRegistrationStart()
                : tournament.getRegistrationStart();
        LocalDateTime regEnd = request.getRegistrationEnd() != null ? request.getRegistrationEnd()
                : tournament.getRegistrationEnd();
        LocalDateTime start = request.getStartDate() != null ? request.getStartDate() : tournament.getStartDate();
        LocalDateTime end = request.getEndDate() != null ? request.getEndDate() : tournament.getEndDate();
        validateDates(regStart, regEnd, start, end);

        TournamentFormat format = request.getFormat() != null ? request.getFormat() : tournament.getFormat();
        Integer maxPlayers = request.getMaxPlayers() != null ? request.getMaxPlayers() : tournament.getMaxPlayers();
        Integer playersPerGroup = request.getPlayersPerGroup() != null ? request.getPlayersPerGroup()
                : tournament.getPlayersPerGroup();
        Integer qualifiersPerGroup = request.getQualifiersPerGroup() != null ? request.getQualifiersPerGroup()
                : tournament.getQualifiersPerGroup();
        validateFormatRules(format, maxPlayers, playersPerGroup, qualifiersPerGroup);

        tournamentMapper.updateEntityFromRequest(request, tournament);
        Tournament updated = tournamentRepository.save(tournament);
        log.info("Tournament '{}' (ID: {}) updated successfully by '{}'", updated.getName(), updated.getId(),
                currentUser.getUsername());
        return tournamentMapper.toResponse(updated);
    }

    @Transactional
    public TournamentResponse openRegistrations(UUID id, CustomUserDetails currentUser) {
        Tournament tournament = findTournamentById(id);
        verifyOwnershipOrAdmin(tournament, currentUser);

        if (tournament.getStatus() == TournamentStatus.REGISTRATION_OPEN) {
            throw new BusinessException("Registrations are already open for this tournament.");
        }
        if (tournament.getStatus() != TournamentStatus.DRAFT
                && tournament.getStatus() != TournamentStatus.REGISTRATION_CLOSED) {
            throw new BusinessException(
                    "It is not possible to open registrations for a tournament in status: " + tournament.getStatus());
        }
        if (tournament.getStartDate() != null && tournament.getStartDate().isBefore(LocalDateTime.now())) {
            throw new BusinessException(
                    "It is not possible to open registrations because the tournament start date has already passed.");
        }

        tournament.setStatus(TournamentStatus.REGISTRATION_OPEN);
        Tournament saved = tournamentRepository.save(tournament);
        log.info("Registrations opened for tournament '{}' (ID: {})", saved.getName(), saved.getId());
        return tournamentMapper.toResponse(saved);
    }

    @Transactional
    public TournamentResponse closeRegistrations(UUID id, CustomUserDetails currentUser) {
        Tournament tournament = findTournamentById(id);
        verifyOwnershipOrAdmin(tournament, currentUser);

        if (tournament.getStatus() == TournamentStatus.REGISTRATION_CLOSED) {
            throw new BusinessException("Registrations are already closed for this tournament.");
        }
        if (tournament.getStatus() != TournamentStatus.REGISTRATION_OPEN) {
            throw new BusinessException(
                    "It is not possible to close registrations because the tournament is not currently accepting registrations (current status: "
                            + tournament.getStatus() + ").");
        }

        tournament.setStatus(TournamentStatus.REGISTRATION_CLOSED);
        Tournament saved = tournamentRepository.save(tournament);
        log.info("Registrations closed for tournament '{}' (ID: {})", saved.getName(), saved.getId());
        return tournamentMapper.toResponse(saved);
    }

    @Transactional
    public void deleteTournament(UUID id, CustomUserDetails currentUser) {
        Tournament tournament = findTournamentById(id);
        verifyOwnershipOrAdmin(tournament, currentUser);

        if (tournament.getStatus() == TournamentStatus.GROUP_STAGE
                || tournament.getStatus() == TournamentStatus.PLAYOFFS
                || tournament.getStatus() == TournamentStatus.COMPLETED) {
            throw new BusinessException("It is not possible to delete a tournament that is in progress or completed.");
        }

        tournamentRepository.delete(tournament);
        log.info("Tournament '{}' (ID: {}) deleted by '{}'", tournament.getName(), tournament.getId(),
                currentUser.getUsername());
    }

    private Tournament findTournamentById(UUID id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournoi introuvable avec l'id : " + id));
    }

    public void verifyOwnershipOrAdmin(Tournament tournament, CustomUserDetails currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Authentication is required to perform this action.");
        }
        boolean isAdmin = isAdmin(currentUser);
        boolean isOwner = tournament.getOrganizer() != null
                && tournament.getOrganizer().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException(
                    "Access denied: you must be the organizer of this tournament or an administrator.");
        }
    }

    private boolean isAdmin(CustomUserDetails currentUser) {
        return currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private void validateDates(LocalDateTime regStart, LocalDateTime regEnd, LocalDateTime start, LocalDateTime end) {
        if (regStart != null && regEnd != null && regStart.isAfter(regEnd)) {
            throw new BusinessException("The registration start date must be before the registration end date.");
        }
        if (regEnd != null && start != null && regEnd.isAfter(start)) {
            throw new BusinessException(
                    "The registration end date must be before or equal to the tournament start date.");
        }
        if (start != null && end != null && start.isAfter(end)) {
            throw new BusinessException("The tournament start date must be before the end date.");
        }
    }

    private void validateFormatRules(TournamentFormat format, Integer maxPlayers, Integer playersPerGroup,
            Integer qualifiersPerGroup) {
        if (format == TournamentFormat.GROUPS_AND_KNOCKOUT || format == TournamentFormat.GROUPS_ONLY) {
            if (playersPerGroup == null || playersPerGroup < 2) {
                throw new BusinessException(
                        "The number of players per group must be at least 2 for formats that include groups.");
            }
            if (qualifiersPerGroup == null || qualifiersPerGroup < 1) {
                throw new BusinessException("The number of qualifiers per group must be at least 1.");
            }
            if (qualifiersPerGroup >= playersPerGroup) {
                throw new BusinessException(
                        "The number of qualifiers per group must be strictly lower than the number of players per group.");
            }
            if (maxPlayers != null && maxPlayers < playersPerGroup) {
                throw new BusinessException(
                        "The maximum number of players must be greater than or equal to the number of players per group.");
            }
        }
    }
}
