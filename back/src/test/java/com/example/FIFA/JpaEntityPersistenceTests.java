package com.example.FIFA;

import com.example.FIFA.match.entity.Match;
import com.example.FIFA.match.entity.MatchResultHistory;
import com.example.FIFA.match.enums.MatchStage;
import com.example.FIFA.match.enums.MatchStatus;
import com.example.FIFA.match.enums.NextMatchSlot;
import com.example.FIFA.match.repository.MatchRepository;
import com.example.FIFA.match.repository.MatchResultHistoryRepository;
import com.example.FIFA.player.entity.Player;
import com.example.FIFA.player.enums.Platform;
import com.example.FIFA.player.repository.PlayerRepository;
import com.example.FIFA.registration.entity.Registration;
import com.example.FIFA.registration.enums.RegistrationStatus;
import com.example.FIFA.registration.repository.RegistrationRepository;
import com.example.FIFA.tournament.entity.Tournament;
import com.example.FIFA.tournament.enums.TournamentFormat;
import com.example.FIFA.tournament.enums.TournamentStatus;
import com.example.FIFA.tournament.repository.TournamentRepository;
import com.example.FIFA.tournamentgroup.entity.GroupMember;
import com.example.FIFA.tournamentgroup.entity.TournamentGroup;
import com.example.FIFA.tournamentgroup.repository.GroupMemberRepository;
import com.example.FIFA.tournamentgroup.repository.TournamentGroupRepository;
import com.example.FIFA.user.entity.User;
import com.example.FIFA.user.enums.UserRole;
import com.example.FIFA.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class JpaEntityPersistenceTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private TournamentGroupRepository tournamentGroupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private MatchResultHistoryRepository matchResultHistoryRepository;

    @Test
    @DisplayName("Devrait persister un User et son profil Player associé")
    void shouldPersistUserAndPlayer() {
        User user = User.builder()
                .username("mbappe7")
                .email("mbappe@fifa.com")
                .password("$2a$12$hashedpasswordexample")
                .role(UserRole.PLAYER)
                .build();
        User savedUser = userRepository.saveAndFlush(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();

        Player player = Player.builder()
                .user(savedUser)
                .gamertag("Kylian_07")
                .platform(Platform.PS5)
                .gameId("EA_KYLIAN")
                .build();
        Player savedPlayer = playerRepository.saveAndFlush(player);

        assertThat(savedPlayer.getId()).isNotNull();
        assertThat(savedPlayer.getUser().getId()).isEqualTo(savedUser.getId());
        assertThat(savedPlayer.getPlatform()).isEqualTo(Platform.PS5);
    }

    @Test
    @DisplayName("Devrait créer un tournoi complet avec ses groupes, inscriptions et matchs")
    void shouldPersistTournamentAndFlow() {
        // 1. Organisateur
        User organizer = userRepository.saveAndFlush(User.builder()
                .username("organizer_pro")
                .email("orga@fifa.com")
                .password("$2a$12$hashedpasswordexample")
                .role(UserRole.ORGANIZER)
                .build());

        // 2. Joueurs
        User userP1 = userRepository.saveAndFlush(User.builder()
                .username("player_one")
                .email("p1@fifa.com")
                .password("secret1")
                .role(UserRole.PLAYER)
                .build());
        Player p1 = playerRepository.saveAndFlush(Player.builder()
                .user(userP1)
                .gamertag("P1_Gamer")
                .platform(Platform.PS5)
                .build());

        User userP2 = userRepository.saveAndFlush(User.builder()
                .username("player_two")
                .email("p2@fifa.com")
                .password("secret2")
                .role(UserRole.PLAYER)
                .build());
        Player p2 = playerRepository.saveAndFlush(Player.builder()
                .user(userP2)
                .gamertag("P2_Gamer")
                .platform(Platform.PS5)
                .build());

        // 3. Tournoi
        LocalDateTime now = LocalDateTime.now();
        Tournament tournament = Tournament.builder()
                .name("Coupe d'Automne FIFA 2026")
                .description("Tournoi qualificatif")
                .gameTitle("EA Sports FC 26")
                .platform(Platform.PS5)
                .format(TournamentFormat.GROUPS_AND_KNOCKOUT)
                .status(TournamentStatus.REGISTRATION_OPEN)
                .maxPlayers(32)
                .playersPerGroup(4)
                .qualifiersPerGroup(2)
                .registrationStart(now.minusDays(5))
                .registrationEnd(now.plusDays(2))
                .startDate(now.plusDays(3))
                .organizer(organizer)
                .build();
        Tournament savedTournament = tournamentRepository.saveAndFlush(tournament);
        assertThat(savedTournament.getId()).isNotNull();

        // 4. Inscription
        Registration registration = Registration.builder()
                .tournament(savedTournament)
                .player(p1)
                .status(RegistrationStatus.CONFIRMED)
                .requestedAt(now.minusDays(1))
                .reviewedAt(now)
                .reviewedBy(organizer)
                .build();
        Registration savedReg = registrationRepository.saveAndFlush(registration);
        assertThat(savedReg.getId()).isNotNull();

        // 5. Groupe
        TournamentGroup groupA = TournamentGroup.builder()
                .tournament(savedTournament)
                .name("Groupe A")
                .position(1)
                .build();
        TournamentGroup savedGroup = tournamentGroupRepository.saveAndFlush(groupA);
        assertThat(savedGroup.getId()).isNotNull();

        // 6. Membre du groupe
        GroupMember member1 = GroupMember.builder()
                .tournament(savedTournament)
                .group(savedGroup)
                .player(p1)
                .seed(1)
                .joinedAt(now)
                .build();
        GroupMember savedMember = groupMemberRepository.saveAndFlush(member1);
        assertThat(savedMember.getId()).isNotNull();

        // 7. Match
        Match match = Match.builder()
                .tournament(savedTournament)
                .group(savedGroup)
                .stage(MatchStage.GROUP)
                .roundNumber(1)
                .matchNumber(1)
                .player1(p1)
                .player2(p2)
                .scorePlayer1(3)
                .scorePlayer2(1)
                .winner(p1)
                .status(MatchStatus.COMPLETED)
                .playedAt(now)
                .nextMatchSlot(NextMatchSlot.PLAYER1)
                .build();
        Match savedMatch = matchRepository.saveAndFlush(match);
        assertThat(savedMatch.getId()).isNotNull();
        assertThat(savedMatch.getWinner().getId()).isEqualTo(p1.getId());

        // 8. Historique de score
        MatchResultHistory history = MatchResultHistory.builder()
                .match(savedMatch)
                .previousScorePlayer1(2)
                .previousScorePlayer2(1)
                .newScorePlayer1(3)
                .newScorePlayer2(1)
                .changedBy(organizer)
                .reason("Correction suite a revision de la preuve video")
                .changedAt(now)
                .build();
        MatchResultHistory savedHistory = matchResultHistoryRepository.saveAndFlush(history);
        assertThat(savedHistory.getId()).isNotNull();
        assertThat(savedHistory.getChangedBy().getId()).isEqualTo(organizer.getId());
    }

    @Test
    @DisplayName("Devrait lever une exception en cas de doublon d'inscription pour le même joueur et tournoi")
    void shouldRejectDuplicateRegistration() {
        User organizer = userRepository.saveAndFlush(User.builder()
                .username("orga_dup")
                .email("orga_dup@fifa.com")
                .password("pwd")
                .role(UserRole.ORGANIZER)
                .build());

        User userP = userRepository.saveAndFlush(User.builder()
                .username("player_dup")
                .email("p_dup@fifa.com")
                .password("pwd")
                .role(UserRole.PLAYER)
                .build());
        Player player = playerRepository.saveAndFlush(Player.builder()
                .user(userP)
                .gamertag("GamerDup")
                .platform(Platform.PC)
                .build());

        Tournament tournament = tournamentRepository.saveAndFlush(Tournament.builder()
                .name("Tournoi Unicite")
                .format(TournamentFormat.KNOCKOUT_ONLY)
                .status(TournamentStatus.REGISTRATION_OPEN)
                .maxPlayers(16)
                .registrationStart(LocalDateTime.now())
                .registrationEnd(LocalDateTime.now().plusDays(2))
                .startDate(LocalDateTime.now().plusDays(3))
                .organizer(organizer)
                .build());

        registrationRepository.saveAndFlush(Registration.builder()
                .tournament(tournament)
                .player(player)
                .status(RegistrationStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build());

        Registration duplicate = Registration.builder()
                .tournament(tournament)
                .player(player)
                .status(RegistrationStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();

        assertThatThrownBy(() -> registrationRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Devrait interdire l'affectation d'un joueur à plusieurs groupes du même tournoi")
    void shouldEnforceSingleGroupPerTournamentConstraint() {
        User organizer = userRepository.saveAndFlush(User.builder()
                .username("orga_grp")
                .email("orga_grp@fifa.com")
                .password("pwd")
                .role(UserRole.ORGANIZER)
                .build());

        User userP = userRepository.saveAndFlush(User.builder()
                .username("player_multi_grp")
                .email("p_grp@fifa.com")
                .password("pwd")
                .role(UserRole.PLAYER)
                .build());
        Player player = playerRepository.saveAndFlush(Player.builder()
                .user(userP)
                .gamertag("GamerMultiGrp")
                .platform(Platform.XBOX)
                .build());

        Tournament tournament = tournamentRepository.saveAndFlush(Tournament.builder()
                .name("Tournoi Groupes Multi")
                .format(TournamentFormat.GROUPS_AND_KNOCKOUT)
                .status(TournamentStatus.GROUP_STAGE)
                .maxPlayers(16)
                .registrationStart(LocalDateTime.now())
                .registrationEnd(LocalDateTime.now().plusDays(2))
                .startDate(LocalDateTime.now().plusDays(3))
                .organizer(organizer)
                .build());

        TournamentGroup groupA = tournamentGroupRepository.saveAndFlush(TournamentGroup.builder()
                .tournament(tournament)
                .name("Groupe A")
                .position(1)
                .build());

        TournamentGroup groupB = tournamentGroupRepository.saveAndFlush(TournamentGroup.builder()
                .tournament(tournament)
                .name("Groupe B")
                .position(2)
                .build());

        // Affectation au groupe A
        groupMemberRepository.saveAndFlush(GroupMember.builder()
                .tournament(tournament)
                .group(groupA)
                .player(player)
                .joinedAt(LocalDateTime.now())
                .build());

        // Tentative d'affectation au groupe B dans le meme tournoi -> doit echouer au niveau SGBD
        GroupMember illegalSecondGroupMember = GroupMember.builder()
                .tournament(tournament)
                .group(groupB)
                .player(player)
                .joinedAt(LocalDateTime.now())
                .build();

        assertThatThrownBy(() -> groupMemberRepository.saveAndFlush(illegalSecondGroupMember))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
