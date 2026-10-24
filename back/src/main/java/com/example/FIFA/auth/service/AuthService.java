package com.example.FIFA.auth.service;

import com.example.FIFA.auth.dto.AuthResponse;
import com.example.FIFA.auth.dto.LoginRequest;
import com.example.FIFA.auth.dto.RegisterRequest;
import com.example.FIFA.common.exception.DuplicateResourceException;
import com.example.FIFA.common.exception.ResourceNotFoundException;
import com.example.FIFA.player.entity.Player;
import com.example.FIFA.player.enums.Platform;
import com.example.FIFA.player.repository.PlayerRepository;
import com.example.FIFA.security.jwt.JwtService;
import com.example.FIFA.user.dto.UserResponse;
import com.example.FIFA.user.entity.User;
import com.example.FIFA.user.enums.UserRole;
import com.example.FIFA.user.mapper.UserMapper;
import com.example.FIFA.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Ce nom d'utilisateur est déjà utilisé : " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Cette adresse email est déjà utilisée : " + request.getEmail());
        }
        if (playerRepository.existsByGamertag(request.getGamertag())) {
            throw new DuplicateResourceException("Ce gamertag est déjà utilisé : " + request.getGamertag());
        }

        UserRole role = request.getRole() != null ? request.getRole() : UserRole.PLAYER;
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();
        User savedUser = userRepository.save(user);

        Platform platform = request.getPlatform() != null ? request.getPlatform() : Platform.PS5;
        Player player = Player.builder()
                .user(savedUser)
                .gamertag(request.getGamertag())
                .platform(platform)
                .build();
        Player savedPlayer = playerRepository.save(player);

        String token = jwtService.generateToken(savedUser.getUsername(), savedUser.getId(), savedUser.getRole().name());

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime())
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .gamertag(savedPlayer.getGamertag())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsernameOrEmail())
                .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + request.getUsernameOrEmail()));

        String gamertag = playerRepository.findByUserId(user.getId())
                .map(Player::getGamertag)
                .orElse(null);

        String token = jwtService.generateToken(user.getUsername(), user.getId(), user.getRole().name());

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime())
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .gamertag(gamertag)
                .build();
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + username));
        return userMapper.toResponse(user);
    }
}
