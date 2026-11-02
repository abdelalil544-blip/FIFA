package com.example.FIFA.tournament.service;

import com.example.FIFA.common.exception.BusinessException;
import com.example.FIFA.match.repository.MatchRepository;
import com.example.FIFA.player.entity.Player;
import com.example.FIFA.player.enums.Platform;
import com.example.FIFA.player.repository.PlayerRepository;
import com.example.FIFA.registration.repository.RegistrationRepository;
import com.example.FIFA.security.user.CustomUserDetails;
import com.example.FIFA.tournament.dto.TournamentCreateRequest;
import com.example.FIFA.tournament.dto.TournamentResponse;
import com.example.FIFA.tournament.entity.Tournament;
import com.example.FIFA.tournament.enums.TournamentFormat;
import com.example.FIFA.tournament.enums.TournamentStatus;
import com.example.FIFA.tournament.mapper.TournamentMapper;
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

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TournamentServiceBusinessTests {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private TournamentGroupRepository tournamentGroupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private TournamentMapper tournamentMapper;

    @InjectMocks
    private TournamentService tournamentService;

    @Test
    void createTournament_shouldCreateTournamentUsingAuthenticatedOrganizer() {
        User organizer = User.builder()
                .id(UUID.randomUUID())
                .username("organizer")
                .email("organizer@example.com")
                .role(UserRole.ORGANIZER)
                .password("secret")
                .build();
        CustomUserDetails currentUser = new CustomUserDetails(organizer);
        TournamentCreateRequest request = TournamentCreateRequest.builder()
                .name("Champions League")
                .description("Official league")
                .gameTitle("EA FC 25")
                .platform(Platform.PS5)
                .format(TournamentFormat.GROUPS_AND_KNOCKOUT)
                .maxPlayers(16)
                .playersPerGroup(4)
                .qualifiersPerGroup(2)
                .registrationStart(LocalDateTime.now())
                .registrationEnd(LocalDateTime.now().plusDays(5))
                .startDate(LocalDateTime.now().plusDays(10))
                .endDate(LocalDateTime.now().plusDays(12))
                .build();

        Tournament mappedTournament = Tournament.builder()
                .name("Champions League")
                .description("Official league")
                .platform(Platform.PS5)
                .format(TournamentFormat.GROUPS_AND_KNOCKOUT)
                .maxPlayers(16)
                .playersPerGroup(4)
                .qualifiersPerGroup(2)
                .build();

        Tournament savedTournament = Tournament.builder()
                .id(UUID.randomUUID())
                .name("Champions League")
                .description("Official league")
                .platform(Platform.PS5)
                .format(TournamentFormat.GROUPS_AND_KNOCKOUT)
                .maxPlayers(16)
                .playersPerGroup(4)
                .qualifiersPerGroup(2)
                .organizer(organizer)
                .status(TournamentStatus.DRAFT)
                .build();

        TournamentResponse expectedResponse = TournamentResponse.builder()
                .id(savedTournament.getId())
                .name("Champions League")
                .status(TournamentStatus.DRAFT)
                .organizerId(organizer.getId())
                .build();

        when(userRepository.findById(organizer.getId())).thenReturn(Optional.of(organizer));
        when(tournamentMapper.toEntity(request)).thenReturn(mappedTournament);
        when(tournamentRepository.save(any(Tournament.class))).thenReturn(savedTournament);
        when(tournamentMapper.toResponse(savedTournament)).thenReturn(expectedResponse);

        TournamentResponse response = tournamentService.createTournament(request, currentUser);

        assertThat(response.getId()).isEqualTo(savedTournament.getId());
        assertThat(response.getStatus()).isEqualTo(TournamentStatus.DRAFT);
        assertThat(savedTournament.getOrganizer()).isEqualTo(organizer);
    }

    @Test
    void createTournament_shouldRejectInvalidDateRanges() {
        User organizer = User.builder()
                .id(UUID.randomUUID())
                .username("organizer")
                .email("organizer@example.com")
                .role(UserRole.ORGANIZER)
                .password("secret")
                .build();
        CustomUserDetails currentUser = new CustomUserDetails(organizer);

        TournamentCreateRequest request = TournamentCreateRequest.builder()
                .name("Invalid dates")
                .format(TournamentFormat.GROUPS_ONLY)
                .maxPlayers(8)
                .playersPerGroup(4)
                .qualifiersPerGroup(2)
                .registrationStart(LocalDateTime.now().plusDays(10))
                .registrationEnd(LocalDateTime.now().plusDays(2))
                .startDate(LocalDateTime.now().plusDays(5))
                .endDate(LocalDateTime.now().plusDays(7))
                .build();

        assertThrows(BusinessException.class, () -> tournamentService.createTournament(request, currentUser));
    }
}
