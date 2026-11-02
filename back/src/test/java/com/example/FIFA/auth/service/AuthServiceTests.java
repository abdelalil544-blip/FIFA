package com.example.FIFA.auth.service;

import com.example.FIFA.auth.dto.AuthResponse;
import com.example.FIFA.auth.dto.LoginRequest;
import com.example.FIFA.auth.dto.RegisterRequest;
import com.example.FIFA.common.exception.DuplicateResourceException;
import com.example.FIFA.player.entity.Player;
import com.example.FIFA.player.enums.Platform;
import com.example.FIFA.player.repository.PlayerRepository;
import com.example.FIFA.security.jwt.JwtService;
import com.example.FIFA.user.entity.User;
import com.example.FIFA.user.enums.UserRole;
import com.example.FIFA.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_shouldCreateUserAndPlayer() {
        RegisterRequest request = RegisterRequest.builder()
                .username("haaland9")
                .email("haaland@example.com")
                .password("secret123")
                .role(UserRole.PLAYER)
                .gamertag("Haaland")
                .platform(Platform.PC)
                .build();

        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .username("haaland9")
                .email("haaland@example.com")
                .role(UserRole.PLAYER)
                .password("encoded")
                .build();

        Player savedPlayer = Player.builder()
                .id(UUID.randomUUID())
                .user(savedUser)
                .gamertag("Haaland")
                .platform(Platform.PC)
                .build();

        when(userRepository.existsByUsername("haaland9")).thenReturn(false);
        when(userRepository.existsByEmail("haaland@example.com")).thenReturn(false);
        when(playerRepository.existsByGamertag("Haaland")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(playerRepository.save(any(Player.class))).thenReturn(savedPlayer);
        when(jwtService.generateToken("haaland9", savedUser.getId(), savedUser.getRole().name())).thenReturn("jwt-token");
        when(jwtService.getExpirationTime()).thenReturn(3600L);

        AuthResponse response = authService.register(request);

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getUsername()).isEqualTo("haaland9");
        assertThat(response.getGamertag()).isEqualTo("Haaland");
        assertThat(response.getRole()).isEqualTo(UserRole.PLAYER);
    }

    @Test
    void register_shouldRejectDuplicateUsername() {
        RegisterRequest request = RegisterRequest.builder()
                .username("haaland9")
                .email("haaland@example.com")
                .password("secret123")
                .gamertag("Haaland")
                .build();

        when(userRepository.existsByUsername("haaland9")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
    }

    @Test
    void login_shouldReturnUserInfoWhenCredentialsAreValid() {
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("haaland9")
                .password("secret123")
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .username("haaland9")
                .email("haaland@example.com")
                .password("encoded")
                .role(UserRole.PLAYER)
                .build();

        Player player = Player.builder()
                .id(UUID.randomUUID())
                .user(user)
                .gamertag("Haaland")
                .platform(Platform.PC)
                .build();

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByUsername("haaland9")).thenReturn(Optional.of(user));
        when(playerRepository.findByUserId(user.getId())).thenReturn(Optional.of(player));
        when(jwtService.generateToken(user.getUsername(), user.getId(), user.getRole().name())).thenReturn("jwt-token");
        when(jwtService.getExpirationTime()).thenReturn(3600L);

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getUsername()).isEqualTo("haaland9");
        assertThat(response.getGamertag()).isEqualTo("Haaland");
    }
}
