package com.example.FIFA.match.service;

import com.example.FIFA.common.exception.BusinessException;
import com.example.FIFA.common.exception.ResourceNotFoundException;
import com.example.FIFA.match.dto.MatchForfeitRequest;
import com.example.FIFA.match.dto.MatchResponse;
import com.example.FIFA.match.dto.MatchResultDisputeRequest;
import com.example.FIFA.match.dto.MatchResultHistoryResponse;
import com.example.FIFA.match.dto.MatchResultResolutionRequest;
import com.example.FIFA.match.dto.MatchResultSubmissionRequest;
import com.example.FIFA.match.dto.MatchScheduleRequest;
import com.example.FIFA.match.entity.Match;
import com.example.FIFA.match.entity.MatchResultHistory;
import com.example.FIFA.match.enums.MatchResultActionType;
import com.example.FIFA.match.enums.MatchResultSource;
import com.example.FIFA.match.enums.MatchStage;
import com.example.FIFA.match.enums.MatchStatus;
import com.example.FIFA.match.enums.NextMatchSlot;
import com.example.FIFA.match.mapper.MatchMapper;
import com.example.FIFA.match.repository.MatchRepository;
import com.example.FIFA.match.repository.MatchResultHistoryRepository;
import com.example.FIFA.player.entity.Player;
import com.example.FIFA.security.user.CustomUserDetails;
import com.example.FIFA.tournament.entity.Tournament;
import com.example.FIFA.tournament.enums.RoundRobinFormat;
import com.example.FIFA.tournament.enums.TournamentStatus;
import com.example.FIFA.tournament.repository.TournamentRepository;
import com.example.FIFA.tournamentgroup.entity.GroupMember;
import com.example.FIFA.tournamentgroup.entity.TournamentGroup;
import com.example.FIFA.tournamentgroup.repository.GroupMemberRepository;
import com.example.FIFA.tournamentgroup.repository.TournamentGroupRepository;
import com.example.FIFA.user.entity.User;
import com.example.FIFA.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchService {

    private final TournamentRepository tournamentRepository;
    private final TournamentGroupRepository tournamentGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final MatchRepository matchRepository;
    private final MatchResultHistoryRepository matchResultHistoryRepository;
    private final MatchMapper matchMapper;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Match> getGroupMatchesByTournament(UUID tournamentId) {
        return matchRepository.findByTournamentIdAndStage(tournamentId, MatchStage.GROUP);
    }

    @Transactional
    public List<Match> generateGroupMatches(UUID tournamentId, CustomUserDetails currentUser) {
        Tournament tournament = findTournamentById(tournamentId);
        verifyOwnershipOrAdmin(tournament, currentUser);

        if (tournament.getStatus() != TournamentStatus.GROUP_STAGE) {
            throw new BusinessException("Group stage matches can only be generated once the tournament is in group stage.");
        }

        List<Match> existingMatches = matchRepository.findByTournamentIdAndStage(tournamentId, MatchStage.GROUP);
        if (!existingMatches.isEmpty()) {
            throw new BusinessException("Group matches for this tournament have already been generated.");
        }

        List<TournamentGroup> groups = tournamentGroupRepository.findByTournamentIdOrderByPositionAsc(tournamentId);
        if (groups.isEmpty()) {
            throw new BusinessException("Cannot generate group matches before groups are created.");
        }

        RoundRobinFormat roundRobinFormat = tournament.getRoundRobinFormat() == null
                ? RoundRobinFormat.SINGLE_ROUND_ROBIN
                : tournament.getRoundRobinFormat();

        List<Match> generatedMatches = new ArrayList<>();
        int roundCounter = 1;

        for (TournamentGroup group : groups) {
            List<GroupMember> members = groupMemberRepository.findByGroupId(group.getId());
            if (members.size() < 2) {
                continue;
            }

            List<Player> players = new ArrayList<>(members.stream()
                    .map(GroupMember::getPlayer)
                    .toList());

            int cycles = roundRobinFormat == RoundRobinFormat.DOUBLE_ROUND_ROBIN ? 2 : 1;

            for (int cycle = 0; cycle < cycles; cycle++) {
                List<Player> rotation = new ArrayList<>(players);
                if (rotation.size() % 2 != 0) {
                    rotation.add(null);
                }

                for (int round = 0; round < rotation.size() - 1; round++) {
                    Set<String> pairKeys = new HashSet<>();
                    int matchNumber = 1;
                    for (int i = 0; i < rotation.size() / 2; i++) {
                        Player player1 = rotation.get(i);
                        Player player2 = rotation.get(rotation.size() - 1 - i);

                        if (player1 == null || player2 == null || player1.getId().equals(player2.getId())) {
                            continue;
                        }

                        String pairKey = buildPairKey(player1.getId(), player2.getId());
                        if (!pairKeys.add(pairKey)) {
                            continue;
                        }

                        Match match = Match.builder()
                                .tournament(tournament)
                                .group(group)
                                .stage(MatchStage.GROUP)
                                .roundNumber(roundCounter)
                                .matchNumber(matchNumber++)
                                .player1(player1)
                                .player2(player2)
                                .status(MatchStatus.SCHEDULED)
                                .scheduledAt(resolveScheduledAt(tournament, roundCounter, matchNumber - 1))
                                .build();
                        generatedMatches.add(match);
                    }
                    roundCounter++;
                    rotatePlayers(rotation);
                }

                if (cycle == 0 && roundRobinFormat == RoundRobinFormat.DOUBLE_ROUND_ROBIN) {
                    Collections.reverse(players);
                }
            }
        }

        if (generatedMatches.isEmpty()) {
            throw new BusinessException("No valid group matches could be generated for this tournament.");
        }

        matchRepository.saveAll(generatedMatches);
        log.info("Generated {} group matches for tournament '{}' (ID: {})",
                generatedMatches.size(), tournament.getName(), tournament.getId());
        return generatedMatches;
    }

    @Transactional
    public Match scheduleMatch(UUID matchId, MatchScheduleRequest request, CustomUserDetails currentUser) {
        if (request == null || request.getScheduledAt() == null) {
            throw new BusinessException("The match schedule date and time are required.");
        }

        Match match = findMatchById(matchId);
        Tournament tournament = match.getTournament();
        if (tournament == null) {
            throw new ResourceNotFoundException("Tournament not found for the requested match.");
        }
        verifyOwnershipOrAdmin(tournament, currentUser);
        match.setScheduledAt(request.getScheduledAt());

        Match savedMatch = matchRepository.save(match);
        log.info("Match '{}' in tournament '{}' has been scheduled for {}",
                savedMatch.getId(), savedMatch.getTournament().getName(), savedMatch.getScheduledAt());
        return savedMatch;
    }

    @Transactional
    public MatchResponse submitMatchResult(UUID tournamentId, UUID matchId, MatchResultSubmissionRequest request, CustomUserDetails currentUser) {
        Match match = findMatchByIdAndTournament(matchId, tournamentId);
        validateMatchResultSubmissionRequest(request, match, currentUser);

        User actingUser = findUserById(currentUser.getId());
        MatchStatus previousStatus = match.getStatus();

        match.setScorePlayer1(request.getScorePlayer1());
        match.setScorePlayer2(request.getScorePlayer2());
        match.setWinner(determineWinner(match));
        match.setStatus(MatchStatus.PENDING_CONFIRMATION);
        match.setSubmittedBy(actingUser);
        match.setSubmittedAt(LocalDateTime.now());
        match.setResultSource(MatchResultSource.PLAYER_SUBMISSION);
        match.setPlayedAt(null);
        match.setConfirmedBy(null);
        match.setConfirmedAt(null);

        matchRepository.save(match);
        createHistory(match, actingUser, previousStatus, MatchStatus.PENDING_CONFIRMATION,
                MatchResultActionType.SUBMISSION, request.getReason(),
                match.getScorePlayer1(), match.getScorePlayer2(), request.getScorePlayer1(), request.getScorePlayer2());

        log.info("Match result submitted by user {} for match {}.", actingUser.getId(), matchId);
        return matchMapper.toResponse(match);
    }

    @Transactional
    public MatchResponse confirmMatchResult(UUID tournamentId, UUID matchId, CustomUserDetails currentUser) {
        Match match = findMatchByIdAndTournament(matchId, tournamentId);
        User actingUser = findUserById(currentUser.getId());

        if (match.getStatus() != MatchStatus.PENDING_CONFIRMATION) {
            throw new BusinessException("Only a result awaiting confirmation can be confirmed.");
        }
        if (match.getSubmittedBy() == null) {
            throw new BusinessException("The match result has no submission to confirm.");
        }
        if (actingUser.getId().equals(match.getSubmittedBy().getId())) {
            throw new BusinessException("The submitter cannot confirm their own result.");
        }
        if (!isParticipant(match, actingUser.getId()) || actingUser.getId().equals(match.getSubmittedBy().getId())) {
            throw new AccessDeniedException("Only the opponent of the submitting player can confirm the result.");
        }
        if (match.getConfirmedBy() != null) {
            throw new BusinessException("This result has already been confirmed and cannot be modified again.");
        }

        MatchStatus previousStatus = match.getStatus();
        match.setConfirmedBy(actingUser);
        match.setConfirmedAt(LocalDateTime.now());
        match.setStatus(MatchStatus.COMPLETED);
        match.setPlayedAt(LocalDateTime.now());
        match.setWinner(determineWinner(match));
        match.setResultSource(MatchResultSource.PLAYER_SUBMISSION);
        matchRepository.save(match);
        advanceWinnerToNextMatch(match);

        createHistory(match, actingUser, previousStatus, MatchStatus.COMPLETED,
                MatchResultActionType.CONFIRMATION, "Confirmed by the opposing player.",
                match.getScorePlayer1(), match.getScorePlayer2(), match.getScorePlayer1(), match.getScorePlayer2());

        return matchMapper.toResponse(match);
    }

    @Transactional
    public MatchResponse disputeMatchResult(UUID tournamentId, UUID matchId, MatchResultDisputeRequest request, CustomUserDetails currentUser) {
        Match match = findMatchByIdAndTournament(matchId, tournamentId);
        User actingUser = findUserById(currentUser.getId());

        if (request == null || request.getReason() == null || request.getReason().isBlank()) {
            throw new BusinessException("A dispute reason is required.");
        }
        if (match.getStatus() != MatchStatus.PENDING_CONFIRMATION) {
            throw new BusinessException("Only a pending result can be disputed.");
        }
        if (!isOpponentOfSubmittedPlayer(match, actingUser.getId())) {
            throw new AccessDeniedException("Only the opponent of the submitting player can dispute the result.");
        }

        MatchStatus previousStatus = match.getStatus();
        match.setStatus(MatchStatus.DISPUTED);
        match.setDisputedBy(actingUser);
        match.setDisputedAt(LocalDateTime.now());
        match.setDisputeReason(request.getReason());
        matchRepository.save(match);

        createHistory(match, actingUser, previousStatus, MatchStatus.DISPUTED,
                MatchResultActionType.DISPUTE, request.getReason(),
                match.getScorePlayer1(), match.getScorePlayer2(), match.getScorePlayer1(), match.getScorePlayer2());

        return matchMapper.toResponse(match);
    }

    @Transactional
    public MatchResponse resolveMatchDispute(UUID tournamentId, UUID matchId, MatchResultResolutionRequest request, CustomUserDetails currentUser) {
        Match match = findMatchByIdAndTournament(matchId, tournamentId);
        Tournament tournament = match.getTournament();
        verifyOwnershipOrAdmin(tournament, currentUser);

        if (request == null || request.getApproved() == null) {
            throw new BusinessException("The dispute decision is required.");
        }
        if (match.getStatus() != MatchStatus.DISPUTED) {
            throw new BusinessException("Only a disputed result can be resolved.");
        }

        MatchStatus previousStatus = match.getStatus();
        User actingUser = findUserById(currentUser.getId());

        if (Boolean.TRUE.equals(request.getApproved())) {
            Integer score1 = request.getScorePlayer1() != null ? request.getScorePlayer1() : match.getScorePlayer1();
            Integer score2 = request.getScorePlayer2() != null ? request.getScorePlayer2() : match.getScorePlayer2();
            if (score1 == null || score2 == null) {
                throw new BusinessException("A final score is required to resolve the dispute.");
            }
            if (score1 < 0 || score2 < 0) {
                throw new BusinessException("Scores cannot be negative.");
            }

            match.setScorePlayer1(score1);
            match.setScorePlayer2(score2);
            match.setWinner(determineWinner(match));
            match.setStatus(MatchStatus.COMPLETED);
            match.setPlayedAt(LocalDateTime.now());
            match.setConfirmedBy(actingUser);
            match.setConfirmedAt(LocalDateTime.now());
            match.setResultSource(MatchResultSource.ADMIN_CORRECTION);
            matchRepository.save(match);
            advanceWinnerToNextMatch(match);

            createHistory(match, actingUser, previousStatus, MatchStatus.COMPLETED,
                    MatchResultActionType.ADMIN_CORRECTION, request.getReason(),
                    match.getScorePlayer1(), match.getScorePlayer2(), score1, score2);

            return matchMapper.toResponse(match);
        }

        match.setStatus(MatchStatus.PENDING_CONFIRMATION);
        match.setResultSource(MatchResultSource.DISPUTE_RESOLUTION);
        match.setDisputeReason(request.getReason() == null ? "Dispute rejected by tournament organizer or administrator." : request.getReason());
        matchRepository.save(match);

        createHistory(match, actingUser, previousStatus, MatchStatus.PENDING_CONFIRMATION,
                MatchResultActionType.ADMIN_CORRECTION, request.getReason() == null ? "Dispute rejected." : request.getReason(),
                match.getScorePlayer1(), match.getScorePlayer2(), match.getScorePlayer1(), match.getScorePlayer2());

        return matchMapper.toResponse(match);
    }

    @Transactional
    public MatchResponse submitForfeit(UUID tournamentId, UUID matchId, MatchForfeitRequest request, CustomUserDetails currentUser) {
        Match match = findMatchByIdAndTournament(matchId, tournamentId);
        if (request == null) {
            throw new BusinessException("A forfeit request is required.");
        }
        if (request.getForfeitingPlayerId() == null || request.getBeneficiaryPlayerId() == null) {
            throw new BusinessException("Both forfeiting and beneficiary players are required.");
        }
        if (request.getForfeitingPlayerId().equals(request.getBeneficiaryPlayerId())) {
            throw new BusinessException("A player cannot forfeit against themselves.");
        }
        if (match.getStatus() == MatchStatus.COMPLETED || match.getStatus() == MatchStatus.FORFEIT || match.getStatus() == MatchStatus.CANCELLED) {
            throw new BusinessException("A completed or already processed result cannot be changed.");
        }
        if (!isMatchPlayer(match, request.getForfeitingPlayerId()) || !isMatchPlayer(match, request.getBeneficiaryPlayerId())) {
            throw new BusinessException("The forfeit must involve players from this match.");
        }

        User actingUser = findUserById(currentUser.getId());
        boolean isParticipant = isParticipant(match, actingUser.getId());
        boolean isOrganizerOrAdmin = isOrganizerOrAdmin(match.getTournament(), currentUser);
        if (!isParticipant && !isOrganizerOrAdmin) {
            throw new AccessDeniedException("Only the match participants or the tournament organizer/admin can validate a forfeit.");
        }

        Integer scorePlayer1 = request.getScorePlayer1() != null ? request.getScorePlayer1() : resolveDefaultForfeitScore(match, request.getForfeitingPlayerId(), request.getBeneficiaryPlayerId(), true);
        Integer scorePlayer2 = request.getScorePlayer2() != null ? request.getScorePlayer2() : resolveDefaultForfeitScore(match, request.getForfeitingPlayerId(), request.getBeneficiaryPlayerId(), false);

        if (scorePlayer1 < 0 || scorePlayer2 < 0) {
            throw new BusinessException("Forfeit scores cannot be negative.");
        }

        MatchStatus previousStatus = match.getStatus();
        Player beneficiary = match.getPlayer1() != null && match.getPlayer1().getId().equals(request.getBeneficiaryPlayerId())
                ? match.getPlayer1() : match.getPlayer2();
        match.setScorePlayer1(scorePlayer1);
        match.setScorePlayer2(scorePlayer2);
        match.setWinner(beneficiary);
        match.setResultSource(MatchResultSource.FORFEIT);
        match.setStatus(MatchStatus.FORFEIT);
        match.setPlayedAt(LocalDateTime.now());
        match.setSubmittedBy(actingUser);
        match.setSubmittedAt(LocalDateTime.now());
        matchRepository.save(match);
        advanceWinnerToNextMatch(match);

        createHistory(match, actingUser, previousStatus, MatchStatus.FORFEIT,
                MatchResultActionType.FORFEIT, request.getReason() == null ? "Forfeit recorded." : request.getReason(),
                scorePlayer1, scorePlayer2, scorePlayer1, scorePlayer2);

        return matchMapper.toResponse(match);
    }

    @Transactional(readOnly = true)
    public List<MatchResultHistoryResponse> getMatchResultHistory(UUID tournamentId, UUID matchId, CustomUserDetails currentUser) {
        Match match = findMatchByIdAndTournament(matchId, tournamentId);
        if (currentUser == null) {
            throw new AccessDeniedException("Authentication is required.");
        }
        if (!isParticipant(match, currentUser.getId()) && !isOrganizerOrAdmin(match.getTournament(), currentUser)) {
            throw new AccessDeniedException("You are not allowed to view this match history.");
        }

        return matchResultHistoryRepository.findByMatchIdOrderByChangedAtDesc(match.getId())
                .stream()
                .map(matchMapper::toResponse)
                .toList();
    }

    private void validateMatchResultSubmissionRequest(MatchResultSubmissionRequest request, Match match, CustomUserDetails currentUser) {
        if (request == null) {
            throw new BusinessException("A result payload is required.");
        }
        if (request.getScorePlayer1() == null || request.getScorePlayer2() == null) {
            throw new BusinessException("Both scores are required for a result submission.");
        }
        if (request.getScorePlayer1() < 0 || request.getScorePlayer2() < 0) {
            throw new BusinessException("Scores cannot be negative.");
        }
        if (match.getStatus() == MatchStatus.COMPLETED || match.getStatus() == MatchStatus.FORFEIT || match.getStatus() == MatchStatus.CANCELLED || match.getStatus() == MatchStatus.DISPUTED) {
            throw new BusinessException("This match result can no longer be updated.");
        }
        if (match.getStatus() == MatchStatus.PENDING_CONFIRMATION) {
            throw new BusinessException("A result is already awaiting confirmation for this match.");
        }
        if (!isParticipant(match, currentUser.getId())) {
            throw new AccessDeniedException("Only one of the two match participants can submit a score.");
        }
    }

    private MatchResultHistory createHistory(Match match, User changedBy, MatchStatus previousStatus, MatchStatus newStatus,
                                            MatchResultActionType actionType, String reason,
                                            Integer previousScorePlayer1, Integer previousScorePlayer2,
                                            Integer newScorePlayer1, Integer newScorePlayer2) {
        MatchResultHistory history = MatchResultHistory.builder()
                .match(match)
                .previousScorePlayer1(previousScorePlayer1)
                .previousScorePlayer2(previousScorePlayer2)
                .newScorePlayer1(newScorePlayer1)
                .newScorePlayer2(newScorePlayer2)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .actionType(actionType)
                .changedBy(changedBy)
                .reason(reason)
                .build();
        return matchResultHistoryRepository.save(history);
    }

    private Integer resolveDefaultForfeitScore(Match match, UUID forfeitingPlayerId, UUID beneficiaryPlayerId, boolean isPlayer1Score) {
        boolean beneficiaryIsPlayer1 = match.getPlayer1() != null && match.getPlayer1().getId().equals(beneficiaryPlayerId);
        if (isPlayer1Score) {
            return beneficiaryIsPlayer1 ? 3 : 0;
        }
        return beneficiaryIsPlayer1 ? 0 : 3;
    }

    private boolean isParticipant(Match match, UUID userId) {
        if (userId == null) {
            return false;
        }
        return (match.getPlayer1() != null && match.getPlayer1().getUser() != null && match.getPlayer1().getUser().getId().equals(userId))
                || (match.getPlayer2() != null && match.getPlayer2().getUser() != null && match.getPlayer2().getUser().getId().equals(userId));
    }

    private boolean isMatchPlayer(Match match, UUID playerId) {
        if (playerId == null) {
            return false;
        }
        return (match.getPlayer1() != null && match.getPlayer1().getId().equals(playerId))
                || (match.getPlayer2() != null && match.getPlayer2().getId().equals(playerId));
    }

    private boolean isOpponentOfSubmittedPlayer(Match match, UUID userId) {
        if (match.getSubmittedBy() == null || userId == null) {
            return false;
        }
        return isParticipant(match, userId) && !match.getSubmittedBy().getId().equals(userId);
    }

    private Player determineWinner(Match match) {
        if (match.getScorePlayer1() == null || match.getScorePlayer2() == null) {
            return null;
        }
        if (match.getScorePlayer1() > match.getScorePlayer2()) {
            return match.getPlayer1();
        }
        if (match.getScorePlayer2() > match.getScorePlayer1()) {
            return match.getPlayer2();
        }
        return null;
    }

    private Match findMatchById(UUID matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + matchId));
    }

    private Match findMatchByIdAndTournament(UUID matchId, UUID tournamentId) {
        Match match = findMatchById(matchId);
        if (match.getTournament() == null || !match.getTournament().getId().equals(tournamentId)) {
            throw new BusinessException("The match does not belong to the specified tournament.");
        }
        return match;
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private boolean isOrganizerOrAdmin(Tournament tournament, CustomUserDetails currentUser) {
        if (currentUser == null) {
            return false;
        }
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return true;
        }
        return tournament != null && tournament.getOrganizer() != null && tournament.getOrganizer().getId().equals(currentUser.getId());
    }

    private void verifyOwnershipOrAdmin(Tournament tournament, CustomUserDetails currentUser) {
        if (currentUser == null) {
            throw new AccessDeniedException("Authentication is required to perform this action.");
        }
        if (!isOrganizerOrAdmin(tournament, currentUser)) {
            throw new AccessDeniedException("Access denied: you must be the organizer of this tournament or an administrator.");
        }
    }

    private void advanceWinnerToNextMatch(Match match) {
        if (match.getWinner() == null) {
            return;
        }

        if (match.getNextMatch() == null) {
            if (match.getStage() == MatchStage.FINAL) {
                Tournament tournament = match.getTournament();
                if (tournament != null) {
                    tournament.setChampion(match.getWinner());
                    tournament.setStatus(TournamentStatus.COMPLETED);
                    tournamentRepository.save(tournament);
                }
            }
            return;
        }

        Match nextMatch = match.getNextMatch();
        if (match.getNextMatchSlot() == NextMatchSlot.PLAYER1) {
            nextMatch.setPlayer1(match.getWinner());
        } else if (match.getNextMatchSlot() == NextMatchSlot.PLAYER2) {
            nextMatch.setPlayer2(match.getWinner());
        }

        if (nextMatch.getPlayer1() != null && nextMatch.getPlayer2() != null) {
            nextMatch.setStatus(MatchStatus.SCHEDULED);
        }

        matchRepository.save(nextMatch);
    }

    private void rotatePlayers(List<Player> players) {
        if (players.size() <= 2) {
            return;
        }

        Player first = players.get(0);
        for (int i = 1; i < players.size(); i++) {
            players.set(i - 1, players.get(i));
        }
        players.set(players.size() - 1, first);
    }

    private String buildPairKey(UUID player1Id, UUID player2Id) {
        return player1Id.toString().compareTo(player2Id.toString()) < 0
                ? player1Id + "-" + player2Id
                : player2Id + "-" + player1Id;
    }

    private LocalDateTime resolveScheduledAt(Tournament tournament, int round, int matchNumber) {
        if (tournament.getStartDate() == null) {
            return LocalDateTime.now();
        }
        return tournament.getStartDate().plusDays(round).plusMinutes(matchNumber * 30L);
    }

    private Tournament findTournamentById(UUID id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + id));
    }
}
