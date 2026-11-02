package com.example.FIFA.tournamentgroup.service;

import com.example.FIFA.common.exception.BusinessException;
import com.example.FIFA.player.entity.Player;
import com.example.FIFA.player.enums.Platform;
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
import com.example.FIFA.user.entity.User;
import com.example.FIFA.user.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TournamentGroupServiceTests {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private TournamentGroupRepository tournamentGroupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private TournamentGroupMapper tournamentGroupMapper;

    @InjectMocks
    private TournamentGroupService service;

    @Test
    void generateGroups_shouldCreateBalancedGroupsForConfirmedPlayers() {
        UUID tournamentId = UUID.randomUUID();
        User organizer = User.builder()
                .id(UUID.randomUUID())
                .username("organizer")
                .email("organizer@example.com")
                .password("secret")
                .role(UserRole.ORGANIZER)
                .build();
        Tournament tournament = Tournament.builder()
                .id(tournamentId)
                .name("Champions League")
                .organizer(organizer)
                .format(TournamentFormat.GROUPS_AND_KNOCKOUT)
                .playersPerGroup(2)
                .status(TournamentStatus.REGISTRATION_CLOSED)
                .build();
        CustomUserDetails currentUser = new CustomUserDetails(organizer);

        Player alpha = createPlayer("Alpha");
        Player bravo = createPlayer("Bravo");
        Player charlie = createPlayer("Charlie");

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(tournamentGroupRepository.findByTournamentIdOrderByPositionAsc(tournamentId)).thenReturn(List.of());
        when(registrationRepository.findByTournamentIdAndStatus(tournamentId, RegistrationStatus.CONFIRMED)).thenReturn(List.of(
                Registration.builder().player(alpha).tournament(tournament).status(RegistrationStatus.CONFIRMED).build(),
                Registration.builder().player(bravo).tournament(tournament).status(RegistrationStatus.CONFIRMED).build(),
                Registration.builder().player(charlie).tournament(tournament).status(RegistrationStatus.CONFIRMED).build()
        ));

        TournamentGroup groupA = TournamentGroup.builder().id(UUID.randomUUID()).tournament(tournament).name("Group A").position(1).build();
        TournamentGroup groupB = TournamentGroup.builder().id(UUID.randomUUID()).tournament(tournament).name("Group B").position(2).build();
        when(tournamentGroupRepository.saveAll(any())).thenReturn(List.of(groupA, groupB));
        when(groupMemberRepository.saveAll(any())).thenReturn(List.of());
        when(tournamentGroupMapper.toResponse(groupA)).thenReturn(TournamentGroupResponse.builder().id(groupA.getId()).name("Group A").build());
        when(tournamentGroupMapper.toResponse(groupB)).thenReturn(TournamentGroupResponse.builder().id(groupB.getId()).name("Group B").build());

        List<TournamentGroupResponse> result = service.generateGroups(tournamentId, currentUser);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(TournamentGroupResponse::getName).contains("Group A", "Group B");
    }

    @Test
    void generateGroups_shouldRejectWhenNoConfirmedPlayersExist() {
        UUID tournamentId = UUID.randomUUID();
        User organizer = User.builder()
                .id(UUID.randomUUID())
                .username("organizer")
                .email("organizer@example.com")
                .password("secret")
                .role(UserRole.ORGANIZER)
                .build();
        Tournament tournament = Tournament.builder()
                .id(tournamentId)
                .name("Champions League")
                .organizer(organizer)
                .format(TournamentFormat.GROUPS_AND_KNOCKOUT)
                .playersPerGroup(2)
                .status(TournamentStatus.REGISTRATION_CLOSED)
                .build();
        CustomUserDetails currentUser = new CustomUserDetails(organizer);

        when(tournamentRepository.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(tournamentGroupRepository.findByTournamentIdOrderByPositionAsc(tournamentId)).thenReturn(List.of());
        when(registrationRepository.findByTournamentIdAndStatus(tournamentId, RegistrationStatus.CONFIRMED)).thenReturn(List.of());

        assertThrows(BusinessException.class, () -> service.generateGroups(tournamentId, currentUser));
    }

    private Player createPlayer(String gamertag) {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username(gamertag.toLowerCase())
                .email(gamertag.toLowerCase() + "@example.com")
                .password("secret")
                .role(UserRole.PLAYER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return Player.builder()
                .id(UUID.randomUUID())
                .user(user)
                .gamertag(gamertag)
                .platform(Platform.PS5)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
