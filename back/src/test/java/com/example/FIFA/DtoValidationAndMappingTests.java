package com.example.FIFA;

import com.example.FIFA.match.dto.MatchCreateRequest;
import com.example.FIFA.match.dto.MatchResponse;
import com.example.FIFA.match.dto.MatchResultHistoryResponse;
import com.example.FIFA.match.dto.MatchScoreUpdateRequest;
import com.example.FIFA.match.entity.Match;
import com.example.FIFA.match.entity.MatchResultHistory;
import com.example.FIFA.match.enums.MatchStage;
import com.example.FIFA.match.enums.MatchStatus;
import com.example.FIFA.match.mapper.MatchMapper;
import com.example.FIFA.player.dto.PlayerCreateRequest;
import com.example.FIFA.player.dto.PlayerResponse;
import com.example.FIFA.player.entity.Player;
import com.example.FIFA.player.enums.Platform;
import com.example.FIFA.player.mapper.PlayerMapper;
import com.example.FIFA.registration.dto.RegistrationCreateRequest;
import com.example.FIFA.registration.dto.RegistrationResponse;
import com.example.FIFA.registration.entity.Registration;
import com.example.FIFA.registration.enums.RegistrationStatus;
import com.example.FIFA.registration.mapper.RegistrationMapper;
import com.example.FIFA.tournament.dto.TournamentCreateRequest;
import com.example.FIFA.tournament.dto.TournamentResponse;
import com.example.FIFA.tournament.entity.Tournament;
import com.example.FIFA.tournament.enums.TournamentFormat;
import com.example.FIFA.tournament.enums.TournamentStatus;
import com.example.FIFA.tournament.mapper.TournamentMapper;
import com.example.FIFA.tournamentgroup.dto.GroupMemberCreateRequest;
import com.example.FIFA.tournamentgroup.dto.GroupMemberResponse;
import com.example.FIFA.tournamentgroup.dto.TournamentGroupCreateRequest;
import com.example.FIFA.tournamentgroup.dto.TournamentGroupResponse;
import com.example.FIFA.tournamentgroup.entity.GroupMember;
import com.example.FIFA.tournamentgroup.entity.TournamentGroup;
import com.example.FIFA.tournamentgroup.mapper.TournamentGroupMapper;
import com.example.FIFA.user.dto.UserCreateRequest;
import com.example.FIFA.user.dto.UserResponse;
import com.example.FIFA.user.entity.User;
import com.example.FIFA.user.enums.UserRole;
import com.example.FIFA.user.mapper.UserMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DtoValidationAndMappingTests {

    private static Validator validator;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PlayerMapper playerMapper;

    @Autowired
    private TournamentMapper tournamentMapper;

    @Autowired
    private RegistrationMapper registrationMapper;

    @Autowired
    private TournamentGroupMapper tournamentGroupMapper;

    @Autowired
    private MatchMapper matchMapper;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Validation DTO : UserCreateRequest doit rejeter les données invalides")
    void shouldValidateUserCreateRequest() {
        UserCreateRequest invalid = UserCreateRequest.builder()
                .username("ab") // < 3 chars
                .email("not-an-email")
                .password("123") // < 6 chars
                .role(null)
                .build();

        Set<ConstraintViolation<UserCreateRequest>> violations = validator.validate(invalid);
        assertThat(violations).hasSize(4);

        UserCreateRequest valid = UserCreateRequest.builder()
                .username("cristiano7")
                .email("cr7@fifa.com")
                .password("strongpassword")
                .role(UserRole.PLAYER)
                .build();

        Set<ConstraintViolation<UserCreateRequest>> validViolations = validator.validate(valid);
        assertThat(validViolations).isEmpty();
    }

    @Test
    @DisplayName("MapStruct : UserMapper doit convertir correctement Request -> Entity et Entity -> Response")
    void shouldMapUserProperly() {
        UserCreateRequest req = UserCreateRequest.builder()
                .username("zidane5")
                .email("zizou@fifa.com")
                .password("secret123")
                .role(UserRole.ORGANIZER)
                .build();

        User entity = userMapper.toEntity(req);
        assertThat(entity.getUsername()).isEqualTo("zidane5");
        assertThat(entity.getEmail()).isEqualTo("zizou@fifa.com");
        assertThat(entity.getRole()).isEqualTo(UserRole.ORGANIZER);

        entity.setId(UUID.randomUUID());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        UserResponse res = userMapper.toResponse(entity);
        assertThat(res.getId()).isEqualTo(entity.getId());
        assertThat(res.getUsername()).isEqualTo("zidane5");
        assertThat(res.getEmail()).isEqualTo("zizou@fifa.com");
        assertThat(res.getRole()).isEqualTo(UserRole.ORGANIZER);
    }

    @Test
    @DisplayName("Validation DTO : TournamentCreateRequest doit valider les contraintes de capacité et dates")
    void shouldValidateTournamentCreateRequest() {
        TournamentCreateRequest invalid = TournamentCreateRequest.builder()
                .name("")
                .format(null)
                .maxPlayers(1) // Min is 2
                .playersPerGroup(-1)
                .qualifiersPerGroup(-2)
                .registrationStart(null)
                .registrationEnd(null)
                .startDate(null)
                .organizerId(null)
                .build();

        Set<ConstraintViolation<TournamentCreateRequest>> violations = validator.validate(invalid);
        assertThat(violations).isNotEmpty();

        TournamentCreateRequest valid = TournamentCreateRequest.builder()
                .name("Champions Trophy")
                .format(TournamentFormat.GROUPS_AND_KNOCKOUT)
                .maxPlayers(16)
                .playersPerGroup(4)
                .qualifiersPerGroup(2)
                .registrationStart(LocalDateTime.now())
                .registrationEnd(LocalDateTime.now().plusDays(2))
                .startDate(LocalDateTime.now().plusDays(5))
                .organizerId(UUID.randomUUID())
                .build();

        Set<ConstraintViolation<TournamentCreateRequest>> validViolations = validator.validate(valid);
        assertThat(validViolations).isEmpty();
    }

    @Test
    @DisplayName("MapStruct : PlayerMapper et TournamentMapper doivent mapper les relations complexes")
    void shouldMapPlayerAndTournamentRelationships() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("neymar10")
                .email("ney@fifa.com")
                .role(UserRole.PLAYER)
                .build();

        Player player = Player.builder()
                .id(UUID.randomUUID())
                .user(user)
                .gamertag("Neymar_JR")
                .platform(Platform.PC)
                .gameId("EA_NEY")
                .createdAt(LocalDateTime.now())
                .build();

        PlayerResponse playerRes = playerMapper.toResponse(player);
        assertThat(playerRes.getId()).isEqualTo(player.getId());
        assertThat(playerRes.getUserId()).isEqualTo(user.getId());
        assertThat(playerRes.getUsername()).isEqualTo("neymar10");
        assertThat(playerRes.getGamertag()).isEqualTo("Neymar_JR");

        Tournament tournament = Tournament.builder()
                .id(UUID.randomUUID())
                .name("World Cup FIFA")
                .organizer(user)
                .format(TournamentFormat.GROUPS_AND_KNOCKOUT)
                .status(TournamentStatus.REGISTRATION_OPEN)
                .maxPlayers(32)
                .build();

        TournamentResponse tourRes = tournamentMapper.toResponse(tournament);
        assertThat(tourRes.getId()).isEqualTo(tournament.getId());
        assertThat(tourRes.getOrganizerId()).isEqualTo(user.getId());
        assertThat(tourRes.getOrganizerUsername()).isEqualTo("neymar10");
    }

    @Test
    @DisplayName("MapStruct : Registration, Group et Match doivent mapper correctement leurs IDs et libellés")
    void shouldMapGroupAndMatch() {
        Tournament tournament = Tournament.builder()
                .id(UUID.randomUUID())
                .name("ESport Series")
                .build();

        TournamentGroup group = TournamentGroup.builder()
                .id(UUID.randomUUID())
                .tournament(tournament)
                .name("Groupe 1")
                .position(1)
                .build();

        TournamentGroupResponse grpRes = tournamentGroupMapper.toResponse(group);
        assertThat(grpRes.getTournamentId()).isEqualTo(tournament.getId());
        assertThat(grpRes.getTournamentName()).isEqualTo("ESport Series");

        Player p1 = Player.builder().id(UUID.randomUUID()).gamertag("P1").build();
        Player p2 = Player.builder().id(UUID.randomUUID()).gamertag("P2").build();

        Match match = Match.builder()
                .id(UUID.randomUUID())
                .tournament(tournament)
                .group(group)
                .stage(MatchStage.GROUP)
                .roundNumber(1)
                .matchNumber(1)
                .player1(p1)
                .player2(p2)
                .scorePlayer1(2)
                .scorePlayer2(0)
                .winner(p1)
                .status(MatchStatus.COMPLETED)
                .build();

        MatchResponse matchRes = matchMapper.toResponse(match);
        assertThat(matchRes.getTournamentId()).isEqualTo(tournament.getId());
        assertThat(matchRes.getGroupId()).isEqualTo(group.getId());
        assertThat(matchRes.getGroupName()).isEqualTo("Groupe 1");
        assertThat(matchRes.getPlayer1Gamertag()).isEqualTo("P1");
        assertThat(matchRes.getPlayer2Gamertag()).isEqualTo("P2");
        assertThat(matchRes.getWinnerGamertag()).isEqualTo("P1");
    }
}
