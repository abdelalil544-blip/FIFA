package com.example.FIFA.match.service;

import com.example.FIFA.common.exception.BusinessException;
import com.example.FIFA.match.dto.MatchForfeitRequest;
import com.example.FIFA.match.dto.MatchResultDisputeRequest;
import com.example.FIFA.match.dto.MatchResultSubmissionRequest;
import com.example.FIFA.match.entity.Match;
import com.example.FIFA.match.entity.MatchResultHistory;
import com.example.FIFA.match.enums.MatchResultActionType;
import com.example.FIFA.match.enums.MatchResultSource;
import com.example.FIFA.match.enums.MatchStage;
import com.example.FIFA.match.enums.MatchStatus;
import com.example.FIFA.match.mapper.MatchMapper;
import com.example.FIFA.match.repository.MatchRepository;
import com.example.FIFA.match.repository.MatchResultHistoryRepository;
import com.example.FIFA.player.entity.Player;
import com.example.FIFA.player.enums.Platform;
import com.example.FIFA.security.user.CustomUserDetails;
import com.example.FIFA.tournament.entity.Tournament;
import com.example.FIFA.tournament.enums.TournamentStatus;
import com.example.FIFA.tournament.repository.TournamentRepository;
import com.example.FIFA.tournamentgroup.repository.GroupMemberRepository;
import com.example.FIFA.tournamentgroup.repository.TournamentGroupRepository;
import com.example.FIFA.user.entity.User;
import com.example.FIFA.user.enums.UserRole;
import com.example.FIFA.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceTests {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private TournamentGroupRepository tournamentGroupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchResultHistoryRepository matchResultHistoryRepository;

    @Mock
    private MatchMapper matchMapper;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MatchService service;

    @Test
    void submitMatchResult_shouldAcceptValidSubmissionFromParticipant() {
        User player1User = createUser("player1", "player1@example.com");
        User player2User = createUser("player2", "player2@example.com");
        Player player1 = createPlayer(player1User, "Alpha");
        Player player2 = createPlayer(player2User, "Bravo");
        Tournament tournament = createTournament(player1User);
        Match match = Match.builder()
                .id(UUID.randomUUID())
                .tournament(tournament)
                .player1(player1)
                .player2(player2)
                .status(MatchStatus.SCHEDULED)
                .stage(MatchStage.GROUP)
                .build();
        CustomUserDetails currentUser = new CustomUserDetails(player1User);
        MatchResultSubmissionRequest request = MatchResultSubmissionRequest.builder()
                .scorePlayer1(2)
                .scorePlayer2(0)
                .reason("Good result")
                .build();

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(userRepository.findById(player1User.getId())).thenReturn(Optional.of(player1User));
        when(matchRepository.save(any(Match.class))).thenReturn(match);
        when(matchResultHistoryRepository.save(any(MatchResultHistory.class))).thenReturn(MatchResultHistory.builder().build());
        when(matchMapper.toResponse(match)).thenReturn(new com.example.FIFA.match.dto.MatchResponse());

        service.submitMatchResult(tournament.getId(), match.getId(), request, currentUser);

        assertThat(match.getStatus()).isEqualTo(MatchStatus.PENDING_CONFIRMATION);
        assertThat(match.getSubmittedBy()).isEqualTo(player1User);
    }

    @Test
    void submitMatchResult_shouldRejectNonParticipant() {
        User player1User = createUser("player1", "player1@example.com");
        User player2User = createUser("player2", "player2@example.com");
        Player player1 = createPlayer(player1User, "Alpha");
        Player player2 = createPlayer(player2User, "Bravo");
        Tournament tournament = createTournament(player1User);
        Match match = Match.builder()
                .id(UUID.randomUUID())
                .tournament(tournament)
                .player1(player1)
                .player2(player2)
                .status(MatchStatus.SCHEDULED)
                .stage(MatchStage.GROUP)
                .build();
        CustomUserDetails currentUser = new CustomUserDetails(createUser("third", "third@example.com"));
        MatchResultSubmissionRequest request = MatchResultSubmissionRequest.builder().scorePlayer1(1).scorePlayer2(0).build();

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));

        assertThrows(AccessDeniedException.class, () -> service.submitMatchResult(tournament.getId(), match.getId(), request, currentUser));
    }

    @Test
    void confirmMatchResult_shouldAcceptConfirmationFromOpponent() {
        User player1User = createUser("player1", "player1@example.com");
        User player2User = createUser("player2", "player2@example.com");
        Player player1 = createPlayer(player1User, "Alpha");
        Player player2 = createPlayer(player2User, "Bravo");
        Tournament tournament = createTournament(player1User);
        Match match = Match.builder()
                .id(UUID.randomUUID())
                .tournament(tournament)
                .player1(player1)
                .player2(player2)
                .status(MatchStatus.PENDING_CONFIRMATION)
                .stage(MatchStage.GROUP)
                .scorePlayer1(2)
                .scorePlayer2(1)
                .submittedBy(player1User)
                .build();
        CustomUserDetails currentUser = new CustomUserDetails(player2User);

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(userRepository.findById(player2User.getId())).thenReturn(Optional.of(player2User));
        when(matchRepository.save(any(Match.class))).thenReturn(match);
        when(matchResultHistoryRepository.save(any(MatchResultHistory.class))).thenReturn(MatchResultHistory.builder().build());
        when(matchMapper.toResponse(match)).thenReturn(new com.example.FIFA.match.dto.MatchResponse());

        service.confirmMatchResult(tournament.getId(), match.getId(), currentUser);

        assertThat(match.getStatus()).isEqualTo(MatchStatus.COMPLETED);
        assertThat(match.getConfirmedBy()).isEqualTo(player2User);
    }

    @Test
    void confirmMatchResult_shouldRejectSubmitterConfirmation() {
        User player1User = createUser("player1", "player1@example.com");
        User player2User = createUser("player2", "player2@example.com");
        Player player1 = createPlayer(player1User, "Alpha");
        Player player2 = createPlayer(player2User, "Bravo");
        Tournament tournament = createTournament(player1User);
        Match match = Match.builder()
                .id(UUID.randomUUID())
                .tournament(tournament)
                .player1(player1)
                .player2(player2)
                .status(MatchStatus.PENDING_CONFIRMATION)
                .stage(MatchStage.GROUP)
                .scorePlayer1(2)
                .scorePlayer2(1)
                .submittedBy(player1User)
                .build();
        CustomUserDetails currentUser = new CustomUserDetails(player1User);

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(userRepository.findById(player1User.getId())).thenReturn(Optional.of(player1User));

        assertThrows(BusinessException.class, () -> service.confirmMatchResult(tournament.getId(), match.getId(), currentUser));
    }

    @Test
    void disputeMatchResult_shouldChangeStatusToDisputed() {
        User player1User = createUser("player1", "player1@example.com");
        User player2User = createUser("player2", "player2@example.com");
        Player player1 = createPlayer(player1User, "Alpha");
        Player player2 = createPlayer(player2User, "Bravo");
        Tournament tournament = createTournament(player1User);
        Match match = Match.builder()
                .id(UUID.randomUUID())
                .tournament(tournament)
                .player1(player1)
                .player2(player2)
                .status(MatchStatus.PENDING_CONFIRMATION)
                .stage(MatchStage.GROUP)
                .scorePlayer1(2)
                .scorePlayer2(1)
                .submittedBy(player1User)
                .build();
        MatchResultDisputeRequest request = MatchResultDisputeRequest.builder().reason("Score is wrong").build();
        CustomUserDetails currentUser = new CustomUserDetails(player2User);

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(userRepository.findById(player2User.getId())).thenReturn(Optional.of(player2User));
        when(matchRepository.save(any(Match.class))).thenReturn(match);
        when(matchResultHistoryRepository.save(any(MatchResultHistory.class))).thenReturn(MatchResultHistory.builder().build());
        when(matchMapper.toResponse(match)).thenReturn(new com.example.FIFA.match.dto.MatchResponse());

        service.disputeMatchResult(tournament.getId(), match.getId(), request, currentUser);

        assertThat(match.getStatus()).isEqualTo(MatchStatus.DISPUTED);
        assertThat(match.getDisputedBy()).isEqualTo(player2User);
    }

    @Test
    void submitForfeit_shouldRecordForfeitSuccessfully() {
        User player1User = createUser("player1", "player1@example.com");
        User player2User = createUser("player2", "player2@example.com");
        Player player1 = createPlayer(player1User, "Alpha");
        Player player2 = createPlayer(player2User, "Bravo");
        Tournament tournament = createTournament(player1User);
        Match match = Match.builder()
                .id(UUID.randomUUID())
                .tournament(tournament)
                .player1(player1)
                .player2(player2)
                .status(MatchStatus.SCHEDULED)
                .stage(MatchStage.GROUP)
                .build();
        MatchForfeitRequest request = MatchForfeitRequest.builder()
                .forfeitingPlayerId(player1.getId())
                .beneficiaryPlayerId(player2.getId())
                .reason("No-show")
                .build();
        CustomUserDetails currentUser = new CustomUserDetails(player1User);

        when(matchRepository.findById(match.getId())).thenReturn(Optional.of(match));
        when(userRepository.findById(player1User.getId())).thenReturn(Optional.of(player1User));
        when(matchRepository.save(any(Match.class))).thenReturn(match);
        when(matchResultHistoryRepository.save(any(MatchResultHistory.class))).thenReturn(MatchResultHistory.builder().build());
        when(matchMapper.toResponse(match)).thenReturn(new com.example.FIFA.match.dto.MatchResponse());

        service.submitForfeit(tournament.getId(), match.getId(), request, currentUser);

        assertThat(match.getStatus()).isEqualTo(MatchStatus.FORFEIT);
        assertThat(match.getWinner()).isEqualTo(player2);
    }

    private User createUser(String username, String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("secret");
        user.setRole(UserRole.PLAYER);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    private Player createPlayer(User user, String gamertag) {
        Player player = new Player();
        player.setId(UUID.randomUUID());
        player.setUser(user);
        player.setGamertag(gamertag);
        player.setPlatform(Platform.PC);
        player.setCreatedAt(LocalDateTime.now());
        return player;
    }

    private Tournament createTournament(User organizer) {
        Tournament tournament = new Tournament();
        tournament.setId(UUID.randomUUID());
        tournament.setName("FIFA Cup");
        tournament.setOrganizer(organizer);
        tournament.setStatus(TournamentStatus.REGISTRATION_CLOSED);
        return tournament;
    }
}
