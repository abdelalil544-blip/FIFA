package com.example.FIFA.tournament.service;

import com.example.FIFA.match.entity.Match;
import com.example.FIFA.match.enums.MatchStatus;
import com.example.FIFA.match.repository.MatchRepository;
import com.example.FIFA.player.entity.Player;
import com.example.FIFA.player.enums.Platform;
import com.example.FIFA.player.repository.PlayerRepository;
import com.example.FIFA.registration.entity.Registration;
import com.example.FIFA.registration.enums.RegistrationStatus;
import com.example.FIFA.registration.repository.RegistrationRepository;
import com.example.FIFA.tournament.dto.GroupStandingResponse;
import com.example.FIFA.tournament.dto.TournamentStatisticsResponse;
import com.example.FIFA.tournament.entity.Tournament;
import com.example.FIFA.tournament.repository.TournamentRepository;
import com.example.FIFA.tournamentgroup.entity.GroupMember;
import com.example.FIFA.tournamentgroup.entity.TournamentGroup;
import com.example.FIFA.tournamentgroup.repository.GroupMemberRepository;
import com.example.FIFA.tournamentgroup.repository.TournamentGroupRepository;
import com.example.FIFA.tournament.mapper.TournamentMapper;
import com.example.FIFA.user.entity.User;
import com.example.FIFA.user.enums.UserRole;
import com.example.FIFA.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TournamentServiceStandingsTests {

    private TournamentRepository tournamentRepository;
    private UserRepository userRepository;
    private PlayerRepository playerRepository;
    private RegistrationRepository registrationRepository;
    private TournamentGroupRepository tournamentGroupRepository;
    private GroupMemberRepository groupMemberRepository;
    private MatchRepository matchRepository;
    private TournamentMapper tournamentMapper;
    private TournamentService tournamentService;

    @BeforeEach
    void setUp() {
        tournamentRepository = mock(TournamentRepository.class);
        userRepository = mock(UserRepository.class);
        playerRepository = mock(PlayerRepository.class);
        registrationRepository = mock(RegistrationRepository.class);
        tournamentGroupRepository = mock(TournamentGroupRepository.class);
        groupMemberRepository = mock(GroupMemberRepository.class);
        matchRepository = mock(MatchRepository.class);
        tournamentMapper = mock(TournamentMapper.class);
        tournamentService = new TournamentService(
                tournamentRepository,
                userRepository,
                playerRepository,
                registrationRepository,
                tournamentGroupRepository,
                groupMemberRepository,
                matchRepository,
                tournamentMapper
        );
    }

    @Test
    void shouldComputeStandingsFromConfirmedMatchesOnly() {
        UUID tournamentId = UUID.randomUUID();
        Tournament tournament = Tournament.builder()
                .id(tournamentId)
                .name("World Cup")
                .build();
        TournamentGroup group = TournamentGroup.builder()
                .id(UUID.randomUUID())
                .tournament(tournament)
                .name("Group A")
                .position(1)
                .build();
        Player alpha = createPlayer(UUID.randomUUID(), "Alpha", "alpha@example.com");
        Player bravo = createPlayer(UUID.randomUUID(), "Bravo", "bravo@example.com");

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(tournamentGroupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupMemberRepository.findByGroupId(group.getId())).thenReturn(List.of(
                GroupMember.builder().group(group).tournament(tournament).player(alpha).build(),
                GroupMember.builder().group(group).tournament(tournament).player(bravo).build()
        ));
        when(matchRepository.findByGroupId(group.getId())).thenReturn(List.of(
                Match.builder().group(group).status(MatchStatus.COMPLETED).player1(alpha).player2(bravo).scorePlayer1(2).scorePlayer2(0).build(),
                Match.builder().group(group).status(MatchStatus.COMPLETED).player1(alpha).player2(bravo).scorePlayer1(1).scorePlayer2(1).build(),
                Match.builder().group(group).status(MatchStatus.PENDING_CONFIRMATION).player1(alpha).player2(bravo).scorePlayer1(3).scorePlayer2(2).build(),
                Match.builder().group(group).status(MatchStatus.DISPUTED).player1(alpha).player2(bravo).scorePlayer1(0).scorePlayer2(0).build()
        ));

        List<GroupStandingResponse> standings = tournamentService.getGroupStandings(tournamentId, group.getId(), null);

        assertEquals(2, standings.size());
        assertEquals("Alpha", standings.get(0).getPlayerGamertag());
        assertEquals(4, standings.get(0).getPoints());
        assertEquals(2, standings.get(0).getMatchesPlayed());
        assertEquals(1, standings.get(0).getWins());
        assertEquals("Bravo", standings.get(1).getPlayerGamertag());
        assertEquals(1, standings.get(1).getPoints());
    }

    @Test
    void shouldComputeTournamentStatisticsAcrossConfirmedMatches() {
        UUID tournamentId = UUID.randomUUID();
        Tournament tournament = Tournament.builder()
                .id(tournamentId)
                .name("Summer League")
                .build();
        Player alpha = createPlayer(UUID.randomUUID(), "Alpha", "alpha@example.com");
        Player bravo = createPlayer(UUID.randomUUID(), "Bravo", "bravo@example.com");

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(registrationRepository.findByTournamentId(tournamentId)).thenReturn(List.of(
                Registration.builder().player(alpha).tournament(tournament).status(RegistrationStatus.CONFIRMED).build(),
                Registration.builder().player(bravo).tournament(tournament).status(RegistrationStatus.PENDING).build()
        ));
        when(registrationRepository.countByTournamentIdAndStatus(tournamentId, RegistrationStatus.CONFIRMED)).thenReturn(1L);
        when(matchRepository.findByTournamentId(tournamentId)).thenReturn(List.of(
                Match.builder().tournament(tournament).player1(alpha).player2(bravo).status(MatchStatus.COMPLETED).scorePlayer1(3).scorePlayer2(1).build(),
                Match.builder().tournament(tournament).player1(alpha).player2(bravo).status(MatchStatus.FORFEIT).scorePlayer1(0).scorePlayer2(3).build(),
                Match.builder().tournament(tournament).player1(alpha).player2(bravo).status(MatchStatus.PENDING_CONFIRMATION).scorePlayer1(2).scorePlayer2(0).build()
        ));
        when(playerRepository.findById(alpha.getId())).thenReturn(Optional.of(alpha));
        when(playerRepository.findById(bravo.getId())).thenReturn(Optional.of(bravo));

        TournamentStatisticsResponse response = tournamentService.getTournamentStatistics(tournamentId, null);

        assertEquals(2, response.getCompletedMatches());
        assertEquals(1, response.getPendingMatches());
        assertEquals(7, response.getTotalGoalsScored());
        assertEquals("Bravo", response.getTopScorerGamertag());
    }

    private Player createPlayer(UUID id, String gamertag, String email) {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username(gamertag.toLowerCase())
                .email(email)
                .password("password")
                .role(UserRole.PLAYER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return Player.builder()
                .id(id)
                .user(user)
                .gamertag(gamertag)
                .platform(Platform.PC)
                .build();
    }
}
