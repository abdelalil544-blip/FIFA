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
import com.example.FIFA.tournament.repository.TournamentSpecification;
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
            throw new AccessDeniedException("Authentification requise pour créer un tournoi.");
        }

        validateDates(request.getRegistrationStart(), request.getRegistrationEnd(), request.getStartDate(), request.getEndDate());
        validateFormatRules(request.getFormat(), request.getMaxPlayers(), request.getPlayersPerGroup(), request.getQualifiersPerGroup());

        User organizer;
        boolean isAdmin = isAdmin(currentUser);
        if (isAdmin && request.getOrganizerId() != null) {
            organizer = userRepository.findById(request.getOrganizerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Organisateur introuvable avec l'id : " + request.getOrganizerId()));
        } else {
            organizer = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur authentifié introuvable en base."));
        }

        Tournament tournament = tournamentMapper.toEntity(request);
        tournament.setOrganizer(organizer);
        if (tournament.getStatus() == null) {
            tournament.setStatus(TournamentStatus.DRAFT);
        }

        Tournament saved = tournamentRepository.save(tournament);
        log.info("Tournoi '{}' (ID: {}) créé avec succès par l'utilisateur '{}'", saved.getName(), saved.getId(), organizer.getUsername());
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
            Pageable pageable
    ) {
        Specification<Tournament> spec = TournamentSpecification.withFilters(status, format, platform, organizerId, search);
        Page<Tournament> tournamentPage = tournamentRepository.findAll(spec, pageable);
        Page<TournamentResponse> responsePage = tournamentPage.map(tournamentMapper::toResponse);
        return PageResponse.from(responsePage);
    }

    @Transactional
    public TournamentResponse updateTournament(UUID id, TournamentUpdateRequest request, CustomUserDetails currentUser) {
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
                throw new BusinessException("Impossible de modifier les paramètres structurels d'un tournoi en cours, complété ou annulé.");
            }
        }

        LocalDateTime regStart = request.getRegistrationStart() != null ? request.getRegistrationStart() : tournament.getRegistrationStart();
        LocalDateTime regEnd = request.getRegistrationEnd() != null ? request.getRegistrationEnd() : tournament.getRegistrationEnd();
        LocalDateTime start = request.getStartDate() != null ? request.getStartDate() : tournament.getStartDate();
        LocalDateTime end = request.getEndDate() != null ? request.getEndDate() : tournament.getEndDate();
        validateDates(regStart, regEnd, start, end);

        TournamentFormat format = request.getFormat() != null ? request.getFormat() : tournament.getFormat();
        Integer maxPlayers = request.getMaxPlayers() != null ? request.getMaxPlayers() : tournament.getMaxPlayers();
        Integer playersPerGroup = request.getPlayersPerGroup() != null ? request.getPlayersPerGroup() : tournament.getPlayersPerGroup();
        Integer qualifiersPerGroup = request.getQualifiersPerGroup() != null ? request.getQualifiersPerGroup() : tournament.getQualifiersPerGroup();
        validateFormatRules(format, maxPlayers, playersPerGroup, qualifiersPerGroup);

        tournamentMapper.updateEntityFromRequest(request, tournament);
        Tournament updated = tournamentRepository.save(tournament);
        log.info("Tournoi '{}' (ID: {}) mis à jour avec succès par '{}'", updated.getName(), updated.getId(), currentUser.getUsername());
        return tournamentMapper.toResponse(updated);
    }

    @Transactional
    public TournamentResponse openRegistrations(UUID id, CustomUserDetails currentUser) {
        Tournament tournament = findTournamentById(id);
        verifyOwnershipOrAdmin(tournament, currentUser);

        if (tournament.getStatus() == TournamentStatus.REGISTRATION_OPEN) {
            throw new BusinessException("Les inscriptions sont déjà ouvertes pour ce tournoi.");
        }
        if (tournament.getStatus() != TournamentStatus.DRAFT && tournament.getStatus() != TournamentStatus.REGISTRATION_CLOSED) {
            throw new BusinessException("Impossible d'ouvrir les inscriptions pour un tournoi au statut : " + tournament.getStatus());
        }
        if (tournament.getStartDate() != null && tournament.getStartDate().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Impossible d'ouvrir les inscriptions : la date de début du tournoi est déjà passée.");
        }

        tournament.setStatus(TournamentStatus.REGISTRATION_OPEN);
        Tournament saved = tournamentRepository.save(tournament);
        log.info("Inscriptions ouvertes pour le tournoi '{}' (ID: {})", saved.getName(), saved.getId());
        return tournamentMapper.toResponse(saved);
    }

    @Transactional
    public TournamentResponse closeRegistrations(UUID id, CustomUserDetails currentUser) {
        Tournament tournament = findTournamentById(id);
        verifyOwnershipOrAdmin(tournament, currentUser);

        if (tournament.getStatus() == TournamentStatus.REGISTRATION_CLOSED) {
            throw new BusinessException("Les inscriptions sont déjà fermées pour ce tournoi.");
        }
        if (tournament.getStatus() != TournamentStatus.REGISTRATION_OPEN) {
            throw new BusinessException("Impossible de fermer les inscriptions : le tournoi n'a pas les inscriptions ouvertes (statut actuel : " + tournament.getStatus() + ").");
        }

        tournament.setStatus(TournamentStatus.REGISTRATION_CLOSED);
        Tournament saved = tournamentRepository.save(tournament);
        log.info("Inscriptions fermées pour le tournoi '{}' (ID: {})", saved.getName(), saved.getId());
        return tournamentMapper.toResponse(saved);
    }

    @Transactional
    public void deleteTournament(UUID id, CustomUserDetails currentUser) {
        Tournament tournament = findTournamentById(id);
        verifyOwnershipOrAdmin(tournament, currentUser);

        if (tournament.getStatus() == TournamentStatus.GROUP_STAGE
                || tournament.getStatus() == TournamentStatus.PLAYOFFS
                || tournament.getStatus() == TournamentStatus.COMPLETED) {
            throw new BusinessException("Impossible de supprimer un tournoi en cours ou terminé.");
        }

        tournamentRepository.delete(tournament);
        log.info("Tournoi '{}' (ID: {}) supprimé par '{}'", tournament.getName(), tournament.getId(), currentUser.getUsername());
    }

    private Tournament findTournamentById(UUID id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournoi introuvable avec l'id : " + id));
    }

    public void verifyOwnershipOrAdmin(Tournament tournament, CustomUserDetails currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Authentification requise pour effectuer cette action.");
        }
        boolean isAdmin = isAdmin(currentUser);
        boolean isOwner = tournament.getOrganizer() != null && tournament.getOrganizer().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("Accès refusé : vous devez être l'organisateur de ce tournoi ou un administrateur.");
        }
    }

    private boolean isAdmin(CustomUserDetails currentUser) {
        return currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private void validateDates(LocalDateTime regStart, LocalDateTime regEnd, LocalDateTime start, LocalDateTime end) {
        if (regStart != null && regEnd != null && regStart.isAfter(regEnd)) {
            throw new BusinessException("La date de début des inscriptions doit être antérieure à la date de fin.");
        }
        if (regEnd != null && start != null && regEnd.isAfter(start)) {
            throw new BusinessException("La date de fin des inscriptions doit être antérieure ou égale à la date de début du tournoi.");
        }
        if (start != null && end != null && start.isAfter(end)) {
            throw new BusinessException("La date de début du tournoi doit être antérieure à la date de fin.");
        }
    }

    private void validateFormatRules(TournamentFormat format, Integer maxPlayers, Integer playersPerGroup, Integer qualifiersPerGroup) {
        if (format == TournamentFormat.GROUPS_AND_KNOCKOUT || format == TournamentFormat.GROUPS_ONLY) {
            if (playersPerGroup == null || playersPerGroup < 2) {
                throw new BusinessException("Le nombre de joueurs par groupe doit être d'au moins 2 pour les formats comprenant des poules.");
            }
            if (qualifiersPerGroup == null || qualifiersPerGroup < 1) {
                throw new BusinessException("Le nombre de qualifiés par groupe doit être d'au moins 1.");
            }
            if (qualifiersPerGroup >= playersPerGroup) {
                throw new BusinessException("Le nombre de qualifiés par groupe doit être strictement inférieur au nombre de joueurs par groupe.");
            }
            if (maxPlayers != null && maxPlayers < playersPerGroup) {
                throw new BusinessException("Le nombre maximum de joueurs doit être supérieur ou égal au nombre de joueurs par groupe.");
            }
        }
    }
}
