package com.example.FIFA.tournament.service;

import com.example.FIFA.common.exception.BusinessException;
import com.example.FIFA.common.exception.ResourceNotFoundException;
import com.example.FIFA.common.response.PageResponse;
import com.example.FIFA.match.entity.Match;
import com.example.FIFA.match.enums.MatchStage;
import com.example.FIFA.match.enums.MatchStatus;
import com.example.FIFA.match.enums.NextMatchSlot;
import com.example.FIFA.match.repository.MatchRepository;
import com.example.FIFA.player.entity.Player;
import com.example.FIFA.player.enums.Platform;
import com.example.FIFA.player.repository.PlayerRepository;
import com.example.FIFA.registration.entity.Registration;
import com.example.FIFA.registration.enums.RegistrationStatus;
import com.example.FIFA.registration.repository.RegistrationRepository;
import com.example.FIFA.security.user.CustomUserDetails;
import com.example.FIFA.tournament.dto.GroupStandingResponse;
import com.example.FIFA.tournament.dto.PlayerTournamentStatisticsResponse;
import com.example.FIFA.tournament.dto.TournamentCreateRequest;
import com.example.FIFA.tournament.dto.TournamentResponse;
import com.example.FIFA.tournament.dto.TournamentStatisticsResponse;
import com.example.FIFA.tournament.dto.TournamentUpdateRequest;
import com.example.FIFA.tournament.entity.Tournament;
import com.example.FIFA.tournament.enums.TournamentFormat;
import com.example.FIFA.tournament.enums.TournamentStatus;
import com.example.FIFA.tournament.mapper.TournamentMapper;
import com.example.FIFA.tournament.repository.TournamentRepository;
import com.example.FIFA.tournamentgroup.entity.GroupMember;
import com.example.FIFA.tournamentgroup.entity.TournamentGroup;
import com.example.FIFA.tournamentgroup.repository.GroupMemberRepository;
import com.example.FIFA.tournamentgroup.repository.TournamentGroupRepository;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final UserRepository userRepository;
    private final PlayerRepository playerRepository;
    private final RegistrationRepository registrationRepository;
    private final TournamentGroupRepository tournamentGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final MatchRepository matchRepository;
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

    @Transactional(readOnly = true)
    public List<GroupStandingResponse> getGroupStandings(UUID tournamentId, UUID groupId, CustomUserDetails currentUser) {
        Tournament tournament = findTournamentById(tournamentId);
        validateTournamentAccess(tournament, currentUser);

        TournamentGroup group = tournamentGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found with id: " + groupId));
        if (!group.getTournament().getId().equals(tournamentId)) {
            throw new BusinessException("The selected group does not belong to the specified tournament.");
        }

        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);
        if (members.isEmpty()) {
            return List.of();
        }

        Map<UUID, StandingsAccumulator> standings = new HashMap<>();
        for (GroupMember member : members) {
            Player player = member.getPlayer();
            if (player == null) {
                continue;
            }
            standings.put(player.getId(), new StandingsAccumulator(player.getId(), player.getGamertag()));
        }

        List<Match> groupMatches = matchRepository.findByGroupId(groupId).stream()
                .filter(match -> match.getStatus() == MatchStatus.COMPLETED || match.getStatus() == MatchStatus.FORFEIT)
                .toList();

        for (Match match : groupMatches) {
            if (match.getPlayer1() == null || match.getPlayer2() == null) {
                continue;
            }
            UUID player1Id = match.getPlayer1().getId();
            UUID player2Id = match.getPlayer2().getId();
            if (!standings.containsKey(player1Id) || !standings.containsKey(player2Id)) {
                continue;
            }

            StandingsAccumulator player1 = standings.get(player1Id);
            StandingsAccumulator player2 = standings.get(player2Id);
            int scorePlayer1 = match.getScorePlayer1() == null ? 0 : match.getScorePlayer1();
            int scorePlayer2 = match.getScorePlayer2() == null ? 0 : match.getScorePlayer2();

            player1.matchesPlayed += 1;
            player2.matchesPlayed += 1;
            player1.goalsFor += scorePlayer1;
            player1.goalsAgainst += scorePlayer2;
            player2.goalsFor += scorePlayer2;
            player2.goalsAgainst += scorePlayer1;

            if (scorePlayer1 > scorePlayer2) {
                player1.wins += 1;
                player1.points += 3;
                player2.losses += 1;
            } else if (scorePlayer2 > scorePlayer1) {
                player2.wins += 1;
                player2.points += 3;
                player1.losses += 1;
            } else {
                player1.draws += 1;
                player2.draws += 1;
                player1.points += 1;
                player2.points += 1;
            }
        }

        List<StandingsAccumulator> sortedStandings = new ArrayList<>(standings.values());
        Map<UUID, Map<UUID, Integer>> directPoints = buildDirectHeadToHeadPoints(groupMatches, standings.keySet());
        sortedStandings.sort((left, right) -> {
            int pointsComparison = Integer.compare(right.points, left.points);
            if (pointsComparison != 0) {
                return pointsComparison;
            }

            int goalDifferenceComparison = Integer.compare(right.goalDifference(), left.goalDifference());
            if (goalDifferenceComparison != 0) {
                return goalDifferenceComparison;
            }

            int goalsForComparison = Integer.compare(right.goalsFor, left.goalsFor);
            if (goalsForComparison != 0) {
                return goalsForComparison;
            }

            int headToHeadComparison = Integer.compare(
                    directPoints.getOrDefault(right.playerId, Map.of()).getOrDefault(left.playerId, 0),
                    directPoints.getOrDefault(left.playerId, Map.of()).getOrDefault(right.playerId, 0));
            if (headToHeadComparison != 0) {
                return headToHeadComparison;
            }

            return left.playerGamertag.compareToIgnoreCase(right.playerGamertag);
        });

        List<GroupStandingResponse> result = new ArrayList<>();
        for (int i = 0; i < sortedStandings.size(); i++) {
            StandingsAccumulator standing = sortedStandings.get(i);
            GroupStandingResponse response = GroupStandingResponse.builder()
                    .rank(i + 1)
                    .playerId(standing.playerId)
                    .playerGamertag(standing.playerGamertag)
                    .matchesPlayed(standing.matchesPlayed)
                    .wins(standing.wins)
                    .draws(standing.draws)
                    .losses(standing.losses)
                    .goalsFor(standing.goalsFor)
                    .goalsAgainst(standing.goalsAgainst)
                    .goalDifference(standing.goalDifference())
                    .points(standing.points)
                    .build();
            result.add(response);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public PlayerTournamentStatisticsResponse getPlayerTournamentStatistics(UUID tournamentId, UUID playerId, CustomUserDetails currentUser) {
        Tournament tournament = findTournamentById(tournamentId);
        validateTournamentAccess(tournament, currentUser);

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + playerId));

        if (!registrationRepository.existsByPlayerIdAndTournamentId(playerId, tournamentId)
                && !groupMemberRepository.existsByTournamentIdAndPlayerId(tournamentId, playerId)) {
            throw new BusinessException("The selected player is not registered in the specified tournament.");
        }

        List<Match> matches = matchRepository.findByTournamentId(tournamentId).stream()
                .filter(match -> isPlayerInMatch(match, playerId))
                .toList();

        int wins = 0;
        int draws = 0;
        int losses = 0;
        int goalsFor = 0;
        int goalsAgainst = 0;
        int pendingResults = 0;
        int disputedResults = 0;
        int matchesPlayed = 0;

        for (Match match : matches) {
            if (match.getStatus() == MatchStatus.PENDING_CONFIRMATION) {
                pendingResults += 1;
                continue;
            }
            if (match.getStatus() == MatchStatus.DISPUTED) {
                disputedResults += 1;
                continue;
            }
            if (match.getStatus() != MatchStatus.COMPLETED && match.getStatus() != MatchStatus.FORFEIT) {
                continue;
            }
            if (match.getScorePlayer1() == null || match.getScorePlayer2() == null) {
                continue;
            }

            matchesPlayed += 1;
            int playerScore = isPlayer1(player, match) ? match.getScorePlayer1() : match.getScorePlayer2();
            int opponentScore = isPlayer1(player, match) ? match.getScorePlayer2() : match.getScorePlayer1();
            goalsFor += playerScore;
            goalsAgainst += opponentScore;

            if (playerScore > opponentScore) {
                wins += 1;
            } else if (playerScore < opponentScore) {
                losses += 1;
            } else {
                draws += 1;
            }
        }

        int points = wins * 3 + draws;
        double winRate = matchesPlayed == 0 ? 0.0 : (wins * 100.0) / matchesPlayed;

        return PlayerTournamentStatisticsResponse.builder()
                .playerId(player.getId())
                .playerGamertag(player.getGamertag())
                .matchesPlayed(matchesPlayed)
                .wins(wins)
                .draws(draws)
                .losses(losses)
                .points(points)
                .goalsFor(goalsFor)
                .goalsAgainst(goalsAgainst)
                .goalDifference(goalsFor - goalsAgainst)
                .winRate(winRate)
                .pendingResults(pendingResults)
                .disputedResults(disputedResults)
                .build();
    }

    @Transactional(readOnly = true)
    public TournamentStatisticsResponse getTournamentStatistics(UUID tournamentId, CustomUserDetails currentUser) {
        Tournament tournament = findTournamentById(tournamentId);
        validateTournamentAccess(tournament, currentUser);

        List<Registration> registrations = registrationRepository.findByTournamentId(tournamentId);
        List<Match> matches = matchRepository.findByTournamentId(tournamentId);

        long registeredPlayers = registrations.size();
        long confirmedPlayers = registrationRepository.countByTournamentIdAndStatus(tournamentId, RegistrationStatus.CONFIRMED);
        long totalMatches = matches.size();
        long completedMatches = matches.stream()
                .filter(match -> match.getStatus() == MatchStatus.COMPLETED || match.getStatus() == MatchStatus.FORFEIT)
                .count();
        long pendingMatches = matches.stream()
                .filter(match -> match.getStatus() == MatchStatus.PENDING_CONFIRMATION)
                .count();
        long disputedMatches = matches.stream()
                .filter(match -> match.getStatus() == MatchStatus.DISPUTED)
                .count();

        int totalGoalsScored = 0;
        Map<UUID, Integer> goalsByPlayer = new HashMap<>();
        Map<UUID, Integer> winsByPlayer = new HashMap<>();

        for (Match match : matches) {
            if (match.getStatus() != MatchStatus.COMPLETED && match.getStatus() != MatchStatus.FORFEIT) {
                continue;
            }
            if (match.getScorePlayer1() == null || match.getScorePlayer2() == null) {
                continue;
            }
            totalGoalsScored += match.getScorePlayer1() + match.getScorePlayer2();

            if (match.getPlayer1() != null) {
                goalsByPlayer.merge(match.getPlayer1().getId(), match.getScorePlayer1(), Integer::sum);
            }
            if (match.getPlayer2() != null) {
                goalsByPlayer.merge(match.getPlayer2().getId(), match.getScorePlayer2(), Integer::sum);
            }

            if (match.getScorePlayer1() > match.getScorePlayer2()) {
                if (match.getPlayer1() != null) {
                    winsByPlayer.merge(match.getPlayer1().getId(), 1, Integer::sum);
                }
            } else if (match.getScorePlayer2() > match.getScorePlayer1()) {
                if (match.getPlayer2() != null) {
                    winsByPlayer.merge(match.getPlayer2().getId(), 1, Integer::sum);
                }
            }
        }

        double averageGoalsPerCompletedMatch = completedMatches == 0 ? 0.0 : totalGoalsScored / (double) completedMatches;

        Player topScorer = null;
        Integer topScorerGoals = 0;
        for (Map.Entry<UUID, Integer> entry : goalsByPlayer.entrySet()) {
            Player player = playerRepository.findById(entry.getKey()).orElse(null);
            if (player == null) {
                continue;
            }
            if (topScorer == null || entry.getValue() > topScorerGoals
                    || (entry.getValue().equals(topScorerGoals)
                    && player.getGamertag().compareToIgnoreCase(topScorer.getGamertag()) < 0)) {
                topScorer = player;
                topScorerGoals = entry.getValue();
            }
        }

        Player topWinner = null;
        Integer topWinnerWins = 0;
        for (Map.Entry<UUID, Integer> entry : winsByPlayer.entrySet()) {
            Player player = playerRepository.findById(entry.getKey()).orElse(null);
            if (player == null) {
                continue;
            }
            if (topWinner == null || entry.getValue() > topWinnerWins
                    || (entry.getValue().equals(topWinnerWins)
                    && player.getGamertag().compareToIgnoreCase(topWinner.getGamertag()) < 0)) {
                topWinner = player;
                topWinnerWins = entry.getValue();
            }
        }

        return TournamentStatisticsResponse.builder()
                .tournamentId(tournament.getId())
                .tournamentName(tournament.getName())
                .registeredPlayers(registeredPlayers)
                .confirmedPlayers(confirmedPlayers)
                .totalMatches(totalMatches)
                .completedMatches(completedMatches)
                .pendingMatches(pendingMatches)
                .disputedMatches(disputedMatches)
                .totalGoalsScored(totalGoalsScored)
                .averageGoalsPerCompletedMatch(averageGoalsPerCompletedMatch)
                .topScorerPlayerId(topScorer != null ? topScorer.getId() : null)
                .topScorerGamertag(topScorer != null ? topScorer.getGamertag() : null)
                .topScorerGoals(topScorerGoals)
                .topWinnerPlayerId(topWinner != null ? topWinner.getId() : null)
                .topWinnerGamertag(topWinner != null ? topWinner.getGamertag() : null)
                .topWinnerWins(topWinnerWins)
                .build();
    }

    @Transactional
    public List<Match> generatePlayoffs(UUID tournamentId, CustomUserDetails currentUser) {
        Tournament tournament = findTournamentById(tournamentId);
        verifyOwnershipOrAdmin(tournament, currentUser);

        List<TournamentGroup> groups = tournamentGroupRepository.findByTournamentIdOrderByPositionAsc(tournamentId);
        if (groups.isEmpty()) {
            throw new BusinessException("Cannot generate playoffs before the groups are created.");
        }

        if (!matchRepository.findByTournamentIdAndStage(tournamentId, MatchStage.GROUP).isEmpty()) {
            for (Match match : matchRepository.findByTournamentId(tournamentId)) {
                if (match.getStage() != MatchStage.GROUP && match.getStatus() != MatchStatus.CANCELLED) {
                    throw new BusinessException("The playoff bracket has already been generated for this tournament.");
                }
            }
        }

        for (TournamentGroup group : groups) {
            List<Match> groupMatches = matchRepository.findByGroupId(group.getId());
            boolean hasPendingOrDisputed = groupMatches.stream().anyMatch(match ->
                    match.getStatus() == MatchStatus.PENDING_CONFIRMATION || match.getStatus() == MatchStatus.DISPUTED);
            if (hasPendingOrDisputed) {
                throw new BusinessException("Playoffs cannot start while group results are still pending or disputed.");
            }
        }

        List<Player> qualifiers = determineQualifiers(tournament, groups);
        if (qualifiers.size() < 2) {
            throw new BusinessException("At least two players must qualify for the playoff bracket.");
        }

        List<Match> bracket = buildPlayoffBracket(tournament, qualifiers);
        tournament.setStatus(TournamentStatus.PLAYOFFS);
        tournamentRepository.save(tournament);
        return bracket;
    }

    @Transactional(readOnly = true)
    public List<Match> getPlayoffBracket(UUID tournamentId, CustomUserDetails currentUser) {
        Tournament tournament = findTournamentById(tournamentId);
        validateTournamentAccess(tournament, currentUser);

        return matchRepository.findByTournamentId(tournamentId).stream()
                .filter(match -> match.getStage() != MatchStage.GROUP)
                .sorted(Comparator.comparing(Match::getRoundNumber, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(Match::getMatchNumber, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    @Transactional(readOnly = true)
    public TournamentResponse getChampion(UUID tournamentId, CustomUserDetails currentUser) {
        Tournament tournament = findTournamentById(tournamentId);
        validateTournamentAccess(tournament, currentUser);
        return tournamentMapper.toResponse(tournament);
    }

    private List<Player> determineQualifiers(Tournament tournament, List<TournamentGroup> groups) {
        int qualifiersPerGroup = tournament.getQualifiersPerGroup() == null ? 1 : Math.max(1, tournament.getQualifiersPerGroup());
        List<Player> qualifiers = new ArrayList<>();

        for (TournamentGroup group : groups) {
            List<GroupMember> members = groupMemberRepository.findByGroupId(group.getId());
            if (members.isEmpty()) {
                continue;
            }

            Map<UUID, StandingsAccumulator> standings = new HashMap<>();
            for (GroupMember member : members) {
                Player player = member.getPlayer();
                if (player != null) {
                    standings.put(player.getId(), new StandingsAccumulator(player.getId(), player.getGamertag()));
                }
            }

            List<Match> groupMatches = matchRepository.findByGroupId(group.getId()).stream()
                    .filter(match -> match.getStatus() == MatchStatus.COMPLETED || match.getStatus() == MatchStatus.FORFEIT)
                    .toList();

            for (Match match : groupMatches) {
                if (match.getPlayer1() == null || match.getPlayer2() == null) {
                    continue;
                }
                UUID player1Id = match.getPlayer1().getId();
                UUID player2Id = match.getPlayer2().getId();
                if (!standings.containsKey(player1Id) || !standings.containsKey(player2Id)) {
                    continue;
                }

                StandingsAccumulator player1 = standings.get(player1Id);
                StandingsAccumulator player2 = standings.get(player2Id);
                int scorePlayer1 = match.getScorePlayer1() == null ? 0 : match.getScorePlayer1();
                int scorePlayer2 = match.getScorePlayer2() == null ? 0 : match.getScorePlayer2();

                player1.matchesPlayed += 1;
                player2.matchesPlayed += 1;
                player1.goalsFor += scorePlayer1;
                player1.goalsAgainst += scorePlayer2;
                player2.goalsFor += scorePlayer2;
                player2.goalsAgainst += scorePlayer1;

                if (scorePlayer1 > scorePlayer2) {
                    player1.wins += 1;
                    player1.points += 3;
                    player2.losses += 1;
                } else if (scorePlayer2 > scorePlayer1) {
                    player2.wins += 1;
                    player2.points += 3;
                    player1.losses += 1;
                } else {
                    player1.draws += 1;
                    player2.draws += 1;
                    player1.points += 1;
                    player2.points += 1;
                }
            }

            List<StandingsAccumulator> ordered = new ArrayList<>(standings.values());
            ordered.sort((left, right) -> {
                int byPoints = Integer.compare(right.points, left.points);
                if (byPoints != 0) {
                    return byPoints;
                }
                int byGoalDifference = Integer.compare(right.goalDifference(), left.goalDifference());
                if (byGoalDifference != 0) {
                    return byGoalDifference;
                }
                int byGoalsFor = Integer.compare(right.goalsFor, left.goalsFor);
                if (byGoalsFor != 0) {
                    return byGoalsFor;
                }

                Map<UUID, Map<UUID, Integer>> directPoints = buildDirectHeadToHeadPoints(groupMatches, standings.keySet());
                Integer leftDirect = directPoints.getOrDefault(left.playerId, Map.of()).values().stream().mapToInt(Integer::intValue).sum();
                Integer rightDirect = directPoints.getOrDefault(right.playerId, Map.of()).values().stream().mapToInt(Integer::intValue).sum();
                int byDirect = Integer.compare(rightDirect, leftDirect);
                if (byDirect != 0) {
                    return byDirect;
                }
                return left.playerGamertag.compareToIgnoreCase(right.playerGamertag);
            });

            for (int i = 0; i < Math.min(qualifiersPerGroup, ordered.size()); i++) {
                Player player = playerRepository.findById(ordered.get(i).playerId).orElse(null);
                if (player != null) {
                    qualifiers.add(player);
                }
            }
        }

        return qualifiers.stream().distinct().toList();
    }

    private List<Match> buildPlayoffBracket(Tournament tournament, List<Player> qualifiers) {
        if (qualifiers == null || qualifiers.isEmpty()) {
            throw new BusinessException("No qualified players were found for the playoff bracket.");
        }

        List<Player> bracketPlayers = new ArrayList<>(qualifiers);
        int bracketSize = nextPowerOfTwo(bracketPlayers.size());
        while (bracketPlayers.size() < bracketSize) {
            bracketPlayers.add(null);
        }

        List<List<Match>> rounds = new ArrayList<>();
        List<Player> currentRoundPlayers = new ArrayList<>(bracketPlayers);
        int roundNumber = 1;

        while (currentRoundPlayers.size() > 1) {
            MatchStage stage = resolvePlayoffStage(currentRoundPlayers.size());
            List<Match> roundMatches = new ArrayList<>();
            for (int i = 0; i < currentRoundPlayers.size(); i += 2) {
                Match match = Match.builder()
                        .tournament(tournament)
                        .stage(stage)
                        .roundNumber(roundNumber)
                        .matchNumber((i / 2) + 1)
                        .player1(currentRoundPlayers.get(i))
                        .player2(currentRoundPlayers.get(i + 1))
                        .status(MatchStatus.SCHEDULED)
                        .scheduledAt(tournament.getStartDate() != null ? tournament.getStartDate().plusDays(roundNumber) : LocalDateTime.now())
                        .build();
                roundMatches.add(match);
            }
            rounds.add(roundMatches);
            currentRoundPlayers = new ArrayList<>(Collections.nCopies(roundMatches.size() / 2 == 0 ? 1 : roundMatches.size() / 2, null));
            roundNumber++;
        }

        for (int roundIndex = 0; roundIndex < rounds.size() - 1; roundIndex++) {
            List<Match> currentRound = rounds.get(roundIndex);
            List<Match> nextRound = rounds.get(roundIndex + 1);
            for (int i = 0; i < currentRound.size(); i++) {
                Match currentMatch = currentRound.get(i);
                Match nextMatch = nextRound.get(i / 2);
                currentMatch.setNextMatch(nextMatch);
                currentMatch.setNextMatchSlot(i % 2 == 0 ? com.example.FIFA.match.enums.NextMatchSlot.PLAYER1 : com.example.FIFA.match.enums.NextMatchSlot.PLAYER2);
            }
        }

        List<Match> allMatches = new ArrayList<>();
        for (List<Match> roundMatches : rounds) {
            allMatches.addAll(roundMatches);
        }
        return matchRepository.saveAll(allMatches);
    }

    private MatchStage resolvePlayoffStage(int participantCount) {
        if (participantCount <= 2) {
            return MatchStage.FINAL;
        }
        if (participantCount <= 4) {
            return MatchStage.SEMI_FINAL;
        }
        if (participantCount <= 8) {
            return MatchStage.QUARTER_FINAL;
        }
        if (participantCount <= 16) {
            return MatchStage.ROUND_OF_16;
        }
        return MatchStage.ROUND_OF_32;
    }

    private int nextPowerOfTwo(int value) {
        int size = 1;
        while (size < value) {
            size *= 2;
        }
        return size;
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

    private void validateTournamentAccess(Tournament tournament, CustomUserDetails currentUser) {
        if (currentUser == null) {
            return;
        }

        if (isAdmin(currentUser) || isOrganizerOrAdmin(tournament, currentUser) || isPlayerInTournament(tournament, currentUser.getId())) {
            return;
        }

        throw new AccessDeniedException("Access denied: you are not authorized to view this tournament data.");
    }

    private boolean isPlayerInTournament(Tournament tournament, UUID userId) {
        if (userId == null) {
            return false;
        }

        Optional<Player> player = playerRepository.findByUserId(userId);
        if (player.isEmpty()) {
            return false;
        }

        UUID playerId = player.get().getId();
        return registrationRepository.existsByPlayerIdAndTournamentId(playerId, tournament.getId())
                || groupMemberRepository.existsByTournamentIdAndPlayerId(tournament.getId(), playerId);
    }

    private boolean isPlayerInMatch(Match match, UUID playerId) {
        return (match.getPlayer1() != null && match.getPlayer1().getId().equals(playerId))
                || (match.getPlayer2() != null && match.getPlayer2().getId().equals(playerId));
    }

    private boolean isPlayer1(Player player, Match match) {
        return match.getPlayer1() != null && match.getPlayer1().getId().equals(player.getId());
    }

    private Map<UUID, Map<UUID, Integer>> buildDirectHeadToHeadPoints(List<Match> groupMatches, java.util.Set<UUID> participantIds) {
        Map<UUID, Map<UUID, Integer>> directPoints = new HashMap<>();
        for (Match match : groupMatches) {
            if (match.getPlayer1() == null || match.getPlayer2() == null) {
                continue;
            }
            UUID player1Id = match.getPlayer1().getId();
            UUID player2Id = match.getPlayer2().getId();
            if (!participantIds.contains(player1Id) || !participantIds.contains(player2Id)) {
                continue;
            }

            int player1Score = match.getScorePlayer1() == null ? 0 : match.getScorePlayer1();
            int player2Score = match.getScorePlayer2() == null ? 0 : match.getScorePlayer2();
            int player1Points = player1Score > player2Score ? 3 : (player1Score == player2Score ? 1 : 0);
            int player2Points = player2Score > player1Score ? 3 : (player1Score == player2Score ? 1 : 0);

            directPoints.computeIfAbsent(player1Id, key -> new HashMap<>()).merge(player2Id, player1Points, Integer::sum);
            directPoints.computeIfAbsent(player2Id, key -> new HashMap<>()).merge(player1Id, player2Points, Integer::sum);
        }
        return directPoints;
    }

    private Tournament findTournamentById(UUID id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + id));
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

    private boolean isOrganizerOrAdmin(Tournament tournament, CustomUserDetails currentUser) {
        return isAdmin(currentUser)
                || (tournament.getOrganizer() != null && tournament.getOrganizer().getId().equals(currentUser.getId()));
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

    private static class StandingsAccumulator {
        private final UUID playerId;
        private final String playerGamertag;
        private int matchesPlayed;
        private int wins;
        private int draws;
        private int losses;
        private int goalsFor;
        private int goalsAgainst;
        private int points;

        private StandingsAccumulator(UUID playerId, String playerGamertag) {
            this.playerId = playerId;
            this.playerGamertag = playerGamertag;
        }

        private int goalDifference() {
            return goalsFor - goalsAgainst;
        }
    }
}
