package com.example.FIFA;

import com.example.FIFA.common.response.PageResponse;
import com.example.FIFA.player.enums.Platform;
import com.example.FIFA.security.jwt.JwtService;
import com.example.FIFA.security.user.CustomUserDetails;
import com.example.FIFA.tournament.dto.TournamentCreateRequest;
import com.example.FIFA.tournament.dto.TournamentResponse;
import com.example.FIFA.tournament.dto.TournamentUpdateRequest;
import com.example.FIFA.tournament.entity.Tournament;
import com.example.FIFA.tournament.enums.TournamentFormat;
import com.example.FIFA.tournament.enums.TournamentStatus;
import com.example.FIFA.tournament.repository.TournamentRepository;
import com.example.FIFA.user.entity.User;
import com.example.FIFA.user.enums.UserRole;
import com.example.FIFA.user.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class TournamentManagementIntegrationTests {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User organizer1;
    private User organizer2;
    private User adminUser;
    private User playerUser;

    private String tokenOrganizer1;
    private String tokenOrganizer2;
    private String tokenAdmin;
    private String tokenPlayer;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        organizer1 = userRepository.save(User.builder()
                .username("organizer_alpha")
                .email("alpha@fifa.com")
                .password(passwordEncoder.encode("Secret123!"))
                .role(UserRole.ORGANIZER)
                .build());

        organizer2 = userRepository.save(User.builder()
                .username("organizer_beta")
                .email("beta@fifa.com")
                .password(passwordEncoder.encode("Secret123!"))
                .role(UserRole.ORGANIZER)
                .build());

        adminUser = userRepository.save(User.builder()
                .username("super_admin")
                .email("admin@fifa.com")
                .password(passwordEncoder.encode("Secret123!"))
                .role(UserRole.ADMIN)
                .build());

        playerUser = userRepository.save(User.builder()
                .username("gamer_player")
                .email("player@fifa.com")
                .password(passwordEncoder.encode("Secret123!"))
                .role(UserRole.PLAYER)
                .build());

        tokenOrganizer1 = jwtService.generateToken(new CustomUserDetails(organizer1));
        tokenOrganizer2 = jwtService.generateToken(new CustomUserDetails(organizer2));
        tokenAdmin = jwtService.generateToken(new CustomUserDetails(adminUser));
        tokenPlayer = jwtService.generateToken(new CustomUserDetails(playerUser));
    }

    private Tournament createSampleTournament(User organizer, TournamentStatus status, String name) {
        return tournamentRepository.save(Tournament.builder()
                .name(name)
                .description("Test tournament description")
                .gameTitle("EA Sports FC 25")
                .platform(Platform.PS5)
                .format(TournamentFormat.GROUPS_AND_KNOCKOUT)
                .status(status)
                .maxPlayers(16)
                .playersPerGroup(4)
                .qualifiersPerGroup(2)
                .registrationStart(LocalDateTime.now().plusDays(1))
                .registrationEnd(LocalDateTime.now().plusDays(3))
                .startDate(LocalDateTime.now().plusDays(5))
                .endDate(LocalDateTime.now().plusDays(7))
                .organizer(organizer)
                .build());
    }

    @Test
    @DisplayName("Création : Un ORGANIZER peut créer un tournoi valide")
    void shouldCreateTournamentSuccessfullyAsOrganizer() throws Exception {
        TournamentCreateRequest request = TournamentCreateRequest.builder()
                .name("Champions League EA FC")
                .description("Tournoi officiel PS5")
                .gameTitle("EA Sports FC 25")
                .platform(Platform.PS5)
                .format(TournamentFormat.GROUPS_AND_KNOCKOUT)
                .maxPlayers(16)
                .playersPerGroup(4)
                .qualifiersPerGroup(2)
                .registrationStart(LocalDateTime.now().plusDays(1))
                .registrationEnd(LocalDateTime.now().plusDays(3))
                .startDate(LocalDateTime.now().plusDays(5))
                .endDate(LocalDateTime.now().plusDays(7))
                .build();

        MvcResult result = mockMvc.perform(post("/api/tournaments")
                        .header("Authorization", "Bearer " + tokenOrganizer1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Champions League EA FC"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.organizerId").value(organizer1.getId().toString()))
                .andExpect(jsonPath("$.organizerUsername").value("organizer_alpha"))
                .andReturn();

        TournamentResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), TournamentResponse.class);
        assertThat(response.getId()).isNotNull();
        assertThat(tournamentRepository.findById(response.getId())).isPresent();
    }

    @Test
    @DisplayName("Création : Un PLAYER ne peut PAS créer un tournoi (403 Forbidden)")
    void shouldRejectTournamentCreationWhenCallerIsPlayer() throws Exception {
        TournamentCreateRequest request = TournamentCreateRequest.builder()
                .name("Interdit aux joueurs")
                .format(TournamentFormat.KNOCKOUT_ONLY)
                .maxPlayers(8)
                .registrationStart(LocalDateTime.now().plusDays(1))
                .registrationEnd(LocalDateTime.now().plusDays(2))
                .startDate(LocalDateTime.now().plusDays(4))
                .build();

        mockMvc.perform(post("/api/tournaments")
                        .header("Authorization", "Bearer " + tokenPlayer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Création : Rejet si non authentifié (401 Unauthorized)")
    void shouldRejectTournamentCreationWhenUnauthenticated() throws Exception {
        TournamentCreateRequest request = TournamentCreateRequest.builder()
                .name("Sans auth")
                .format(TournamentFormat.KNOCKOUT_ONLY)
                .maxPlayers(8)
                .registrationStart(LocalDateTime.now().plusDays(1))
                .registrationEnd(LocalDateTime.now().plusDays(2))
                .startDate(LocalDateTime.now().plusDays(4))
                .build();

        mockMvc.perform(post("/api/tournaments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Validation : Rejeter la création si les dates sont incohérentes (400 Bad Request)")
    void shouldRejectTournamentCreationWhenDatesAreInvalid() throws Exception {
        TournamentCreateRequest request = TournamentCreateRequest.builder()
                .name("Dates Invalides")
                .format(TournamentFormat.KNOCKOUT_ONLY)
                .maxPlayers(8)
                .registrationStart(LocalDateTime.now().plusDays(5)) // Inscription commence après la fin !
                .registrationEnd(LocalDateTime.now().plusDays(2))
                .startDate(LocalDateTime.now().plusDays(7))
                .build();

        mockMvc.perform(post("/api/tournaments")
                        .header("Authorization", "Bearer " + tokenOrganizer1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The registration start date must be before the registration end date."));
    }

    @Test
    @DisplayName("Validation : Rejeter si format poules avec qualifiersPerGroup >= playersPerGroup (400 Bad Request)")
    void shouldRejectInvalidGroupParameters() throws Exception {
        TournamentCreateRequest request = TournamentCreateRequest.builder()
                .name("Mauvais Groupe")
                .format(TournamentFormat.GROUPS_AND_KNOCKOUT)
                .maxPlayers(16)
                .playersPerGroup(4)
                .qualifiersPerGroup(4) // Doit être strictement inférieur !
                .registrationStart(LocalDateTime.now().plusDays(1))
                .registrationEnd(LocalDateTime.now().plusDays(2))
                .startDate(LocalDateTime.now().plusDays(4))
                .build();

        mockMvc.perform(post("/api/tournaments")
                        .header("Authorization", "Bearer " + tokenOrganizer1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The number of qualifiers per group must be strictly lower than the number of players per group."));
    }

    @Test
    @DisplayName("Consultation : Accès public par ID sans authentification")
    void shouldAllowPublicReadById() throws Exception {
        Tournament tournament = createSampleTournament(organizer1, TournamentStatus.REGISTRATION_OPEN, "Tournoi Public");

        mockMvc.perform(get("/api/tournaments/{id}", tournament.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tournament.getId().toString()))
                .andExpect(jsonPath("$.name").value("Tournoi Public"))
                .andExpect(jsonPath("$.organizerUsername").value("organizer_alpha"));
    }

    @Test
    @DisplayName("Filtres & Pagination : Recherche publique avec filtres et pagination")
    void shouldFilterAndPaginateTournaments() throws Exception {
        createSampleTournament(organizer1, TournamentStatus.DRAFT, "FIFA Draft Cup");
        createSampleTournament(organizer1, TournamentStatus.REGISTRATION_OPEN, "FIFA Open Cup");
        createSampleTournament(organizer2, TournamentStatus.REGISTRATION_OPEN, "PES Alternative");

        MvcResult result = mockMvc.perform(get("/api/tournaments")
                        .param("status", "REGISTRATION_OPEN")
                        .param("search", "FIFA")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("FIFA Open Cup"))
                .andReturn();

        PageResponse<TournamentResponse> page = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<PageResponse<TournamentResponse>>() {}
        );
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Modification : Le propriétaire peut modifier son tournoi")
    void shouldAllowOwnerToUpdateTournament() throws Exception {
        Tournament tournament = createSampleTournament(organizer1, TournamentStatus.DRAFT, "Ancien Nom");

        TournamentUpdateRequest updateReq = TournamentUpdateRequest.builder()
                .name("Nouveau Nom Maj")
                .description("Nouvelle description enrichie")
                .build();

        mockMvc.perform(put("/api/tournaments/{id}", tournament.getId())
                        .header("Authorization", "Bearer " + tokenOrganizer1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nouveau Nom Maj"))
                .andExpect(jsonPath("$.description").value("Nouvelle description enrichie"));
    }

    @Test
    @DisplayName("Modification : Un AUTRE ORGANIZER est rejeté (403 Forbidden)")
    void shouldRejectUpdateFromNonOwnerOrganizer() throws Exception {
        Tournament tournament = createSampleTournament(organizer1, TournamentStatus.DRAFT, "Tournoi Alpha");

        TournamentUpdateRequest updateReq = TournamentUpdateRequest.builder()
                .name("Piratage de nom")
                .build();

        mockMvc.perform(put("/api/tournaments/{id}", tournament.getId())
                        .header("Authorization", "Bearer " + tokenOrganizer2) // Organizer 2 != Organizer 1 !
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied: you must be the organizer of this tournament or an administrator."));
    }

    @Test
    @DisplayName("Modification : Un ADMIN peut modifier n'importe quel tournoi")
    void shouldAllowAdminToUpdateAnyTournament() throws Exception {
        Tournament tournament = createSampleTournament(organizer1, TournamentStatus.DRAFT, "Tournoi Alpha");

        TournamentUpdateRequest updateReq = TournamentUpdateRequest.builder()
                .name("Modifié par l'Admin")
                .build();

        mockMvc.perform(put("/api/tournaments/{id}", tournament.getId())
                        .header("Authorization", "Bearer " + tokenAdmin) // Admin user
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Modifié par l'Admin"));
    }

    @Test
    @DisplayName("Cycle de vie : Ouvrir et fermer les inscriptions")
    void shouldOpenAndCloseRegistrationsSuccessfully() throws Exception {
        Tournament tournament = createSampleTournament(organizer1, TournamentStatus.DRAFT, "Lifecycle Tournament");

        // 1. Ouvrir les inscriptions
        mockMvc.perform(patch("/api/tournaments/{id}/open-registrations", tournament.getId())
                        .header("Authorization", "Bearer " + tokenOrganizer1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REGISTRATION_OPEN"));

        // 2. Tenter de ré-ouvrir -> 400 Bad Request
        mockMvc.perform(patch("/api/tournaments/{id}/open-registrations", tournament.getId())
                        .header("Authorization", "Bearer " + tokenOrganizer1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Registrations are already open for this tournament."));

        // 3. Fermer les inscriptions
        mockMvc.perform(patch("/api/tournaments/{id}/close-registrations", tournament.getId())
                        .header("Authorization", "Bearer " + tokenOrganizer1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REGISTRATION_CLOSED"));

        // 4. Tenter de fermer à nouveau -> 400 Bad Request
        mockMvc.perform(patch("/api/tournaments/{id}/close-registrations", tournament.getId())
                        .header("Authorization", "Bearer " + tokenOrganizer1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Registrations are already closed for this tournament."));
    }

    @Test
    @DisplayName("Cycle de vie : Rejet ouverture/fermeture par un utilisateur non autorisé (403 Forbidden)")
    void shouldRejectOpenRegistrationByNonOwner() throws Exception {
        Tournament tournament = createSampleTournament(organizer1, TournamentStatus.DRAFT, "Tournoi Reserve");

        mockMvc.perform(patch("/api/tournaments/{id}/open-registrations", tournament.getId())
                        .header("Authorization", "Bearer " + tokenOrganizer2))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied: you must be the organizer of this tournament or an administrator."));
    }

    @Test
    @DisplayName("Suppression : Le propriétaire peut supprimer son tournoi en DRAFT")
    void shouldAllowOwnerToDeleteDraftTournament() throws Exception {
        Tournament tournament = createSampleTournament(organizer1, TournamentStatus.DRAFT, "A Supprimer");

        mockMvc.perform(delete("/api/tournaments/{id}", tournament.getId())
                        .header("Authorization", "Bearer " + tokenOrganizer1))
                .andExpect(status().isNoContent());

        assertThat(tournamentRepository.findById(tournament.getId())).isEmpty();
    }
}
