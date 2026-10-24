package com.example.FIFA;

import com.example.FIFA.auth.dto.LoginRequest;
import com.example.FIFA.auth.dto.RegisterRequest;
import com.example.FIFA.player.enums.Platform;
import com.example.FIFA.player.repository.PlayerRepository;
import com.example.FIFA.security.jwt.JwtService;
import com.example.FIFA.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.example.FIFA.user.enums.UserRole;
import com.example.FIFA.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class AuthAndSecurityIntegrationTests {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @RestController
    static class SecurityTestEndpointsController {
        @GetMapping("/api/admin/dashboard")
        public String adminDashboard() {
            return "admin-only-secret";
        }

        @PostMapping("/api/tournaments")
        public String createTournament() {
            return "tournament-created";
        }

        @GetMapping("/api/tournaments")
        public String listTournaments() {
            return "public-tournament-list";
        }
    }

    @Test
    @DisplayName("Auth : Inscription d'un nouvel utilisateur et création de son profil joueur")
    void shouldRegisterNewUserSuccessfully() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("haaland9")
                .email("haaland@fifa.com")
                .password("supersecret123")
                .role(UserRole.PLAYER)
                .gamertag("Erling_Cyborg")
                .platform(Platform.PC)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value("haaland9"))
                .andExpect(jsonPath("$.email").value("haaland@fifa.com"))
                .andExpect(jsonPath("$.role").value("PLAYER"))
                .andExpect(jsonPath("$.gamertag").value("Erling_Cyborg"));

        assertThat(userRepository.existsByUsername("haaland9")).isTrue();
        assertThat(playerRepository.existsByGamertag("Erling_Cyborg")).isTrue();
    }

    @Test
    @DisplayName("Auth : Rejet en cas de doublon sur nom d'utilisateur (409 Conflict)")
    void shouldRejectDuplicateRegistration() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("modric10")
                .email("modric@fifa.com")
                .password("pwd12345")
                .gamertag("Luka_Magic")
                .platform(Platform.PS5)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Tentative d'inscription avec le même username
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @DisplayName("Auth : Connexion réussie avec mot de passe et génération de JWT")
    void shouldLoginSuccessfully() throws Exception {
        RegisterRequest reg = RegisterRequest.builder()
                .username("kroos8")
                .email("kroos@fifa.com")
                .password("tonipassword")
                .gamertag("ToniKroos")
                .platform(Platform.XBOX)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        LoginRequest login = LoginRequest.builder()
                .usernameOrEmail("kroos8")
                .password("tonipassword")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.username").value("kroos8"))
                .andExpect(jsonPath("$.gamertag").value("ToniKroos"));
    }

    @Test
    @DisplayName("Auth : Rejet de connexion avec mauvais mot de passe (401 Unauthorized)")
    void shouldRejectInvalidCredentials() throws Exception {
        LoginRequest login = LoginRequest.builder()
                .usernameOrEmail("nonexistent_user")
                .password("wrongpassword")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Sécurité : 401 Unauthorized personnalisé si jeton manquant sur endpoint protégé")
    void shouldReturn401WhenNoTokenProvided() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Accès non autorisé : jeton JWT manquant, invalide ou expiré."));
    }

    @Test
    @DisplayName("Sécurité : Accès autorisé à /api/auth/me avec jeton JWT Bearer valide")
    void shouldAccessMeEndpointWithValidToken() throws Exception {
        RegisterRequest reg = RegisterRequest.builder()
                .username("debruyne17")
                .email("kdb@fifa.com")
                .password("pass12345")
                .gamertag("KDB_17")
                .platform(Platform.PC)
                .build();

        String resStr = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(resStr).get("accessToken").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("debruyne17"))
                .andExpect(jsonPath("$.email").value("kdb@fifa.com"));
    }

    @Test
    @DisplayName("Autorisation : 403 Forbidden personnalisé quand PLAYER tente d'accéder à l'admin")
    void shouldReturn403WhenPlayerAccessesAdmin() throws Exception {
        RegisterRequest reg = RegisterRequest.builder()
                .username("bellingham5")
                .email("jude@fifa.com")
                .password("bellingham55")
                .role(UserRole.PLAYER)
                .gamertag("Jude_B")
                .build();

        String resStr = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String playerToken = objectMapper.readTree(resStr).get("accessToken").asText();

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + playerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Accès refusé : vous ne disposez pas des privilèges nécessaires pour accéder à cette ressource."));
    }

    @Test
    @DisplayName("OAuth2 Google : Auto-provisioning utilisateur et redirection frontend avec token JWT")
    void shouldHandleGoogleOAuth2Success() throws Exception {
        OAuth2User mockOAuth2User = mock(OAuth2User.class);
        when(mockOAuth2User.getAttribute("email")).thenReturn("google_player@gmail.com");
        when(mockOAuth2User.getAttribute("name")).thenReturn("Google Player");

        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(mockOAuth2User);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, mockAuth);

        String redirectedUrl = response.getRedirectedUrl();
        assertThat(redirectedUrl).isNotNull();
        assertThat(redirectedUrl).contains("http://localhost:3000/oauth2/redirect?token=");

        // Vérification de la création de l'utilisateur et du joueur en base
        assertThat(userRepository.existsByEmail("google_player@gmail.com")).isTrue();
        assertThat(playerRepository.existsByGamertag("Google Player")).isTrue();
    }
}
