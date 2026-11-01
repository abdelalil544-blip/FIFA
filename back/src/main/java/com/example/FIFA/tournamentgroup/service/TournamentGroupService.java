package com.example.FIFA.tournamentgroup.service;

import com.example.FIFA.common.exception.BusinessException;
import com.example.FIFA.common.exception.ResourceNotFoundException;
import com.example.FIFA.player.entity.Player;
import com.example.FIFA.registration.entity.Registration;
import com.example.FIFA.registration.enums.RegistrationStatus;
import com.example.FIFA.registration.repository.RegistrationRepository;
import com.example.FIFA.security.user.CustomUserDetails;
import com.example.FIFA.tournament.entity.Tournament;
import com.example.FIFA.tournament.enums.TournamentFormat;
import com.example.FIFA.tournament.enums.TournamentStatus;
import com.example.FIFA.tournament.repository.TournamentRepository;
import com.example.FIFA.tournamentgroup.dto.TournamentGroupResponse;
import com.example.FIFA.tournamentgroup.entity.GroupMember;
import com.example.FIFA.tournamentgroup.entity.TournamentGroup;
import com.example.FIFA.tournamentgroup.mapper.TournamentGroupMapper;
import com.example.FIFA.tournamentgroup.repository.GroupMemberRepository;
import com.example.FIFA.tournamentgroup.repository.TournamentGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TournamentGroupService {

    private final TournamentRepository tournamentRepository;
    private final TournamentGroupRepository tournamentGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final RegistrationRepository registrationRepository;
    private final TournamentGroupMapper tournamentGroupMapper;

    @Transactional(readOnly = true)
    public List<TournamentGroupResponse> getGroupsByTournament(UUID tournamentId) {
        findTournamentById(tournamentId);
        return tournamentGroupRepository.findByTournamentIdOrderByPositionAsc(tournamentId)
                .stream()
                .map(tournamentGroupMapper::toResponse)
                .toList();
    }

    @Transactional
    public List<TournamentGroupResponse> generateGroups(UUID tournamentId, CustomUserDetails currentUser) {
        Tournament tournament = findTournamentById(tournamentId);
        verifyOwnershipOrAdmin(tournament, currentUser);
        validateGenerationPrerequisites(tournament);

        if (!tournamentGroupRepository.findByTournamentIdOrderByPositionAsc(tournamentId).isEmpty()) {
            throw new BusinessException("Groups for this tournament have already been generated.");
        }

        List<Registration> confirmedRegistrations = registrationRepository.findByTournamentIdAndStatus(tournamentId, RegistrationStatus.CONFIRMED);
        if (confirmedRegistrations.isEmpty()) {
            throw new BusinessException("Cannot generate groups because no players are confirmed for this tournament.");
        }

        List<Player> players = confirmedRegistrations.stream()
                .map(Registration::getPlayer)
                .sorted(Comparator.comparing(Player::getGamertag, String.CASE_INSENSITIVE_ORDER))
                .toList();

        int playersPerGroup = tournament.getPlayersPerGroup();
        if (players.size() < playersPerGroup) {
            throw new BusinessException("Not enough confirmed players to fill the required group size.");
        }

        int totalGroups = (int) Math.ceil((double) players.size() / playersPerGroup);

        List<TournamentGroup> groups = new ArrayList<>();
        for (int i = 0; i < totalGroups; i++) {
            TournamentGroup group = TournamentGroup.builder()
                    .tournament(tournament)
                    .name("Group " + getGroupName(i))
                    .position(i + 1)
                    .build();
            groups.add(group);
        }

        List<TournamentGroup> savedGroups = tournamentGroupRepository.saveAll(groups);

        List<GroupMember> members = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            TournamentGroup assignedGroup = savedGroups.get(i % savedGroups.size());
            members.add(GroupMember.builder()
                    .tournament(tournament)
                    .group(assignedGroup)
                    .player(players.get(i))
                    .seed(i + 1)
                    .build());
        }

        groupMemberRepository.saveAll(members);
        tournament.setStatus(TournamentStatus.GROUP_STAGE);
        tournamentRepository.save(tournament);

        log.info("Generated {} groups and {} group members for tournament '{}' (ID: {})",
                savedGroups.size(), members.size(), tournament.getName(), tournament.getId());

        return savedGroups.stream()
                .map(tournamentGroupMapper::toResponse)
                .toList();
    }

    private void validateGenerationPrerequisites(Tournament tournament) {
        if (tournament.getFormat() == TournamentFormat.KNOCKOUT_ONLY) {
            throw new BusinessException("Group generation is not available for knockout-only tournaments.");
        }

        if (tournament.getPlayersPerGroup() == null || tournament.getPlayersPerGroup() < 2) {
            throw new BusinessException("A tournament that generates groups must define at least 2 players per group.");
        }

        if (tournament.getStatus() != TournamentStatus.REGISTRATION_CLOSED
                && tournament.getStatus() != TournamentStatus.GROUP_STAGE) {
            throw new BusinessException("Groups can only be generated after registrations are closed.");
        }
    }

    private Tournament findTournamentById(UUID id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + id));
    }

    private void verifyOwnershipOrAdmin(Tournament tournament, CustomUserDetails currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Authentication is required to perform this action.");
        }

        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        boolean isOwner = tournament.getOrganizer() != null
                && tournament.getOrganizer().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("Access denied: you must be the organizer of this tournament or an administrator.");
        }
    }

    private String getGroupName(int index) {
        int quotient = index / 26;
        int remainder = index % 26;
        char letter = (char) ('A' + remainder);
        if (quotient == 0) {
            return String.valueOf(letter);
        }
        return String.valueOf((char) ('A' + quotient - 1)) + letter;
    }
}
