package com.example.FIFA.security.oauth2;

import com.example.FIFA.player.entity.Player;
import com.example.FIFA.player.enums.Platform;
import com.example.FIFA.player.repository.PlayerRepository;
import com.example.FIFA.security.jwt.JwtService;
import com.example.FIFA.user.entity.User;
import com.example.FIFA.user.enums.UserRole;
import com.example.FIFA.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        if (email == null) {
            log.error("Email non fourni par le fournisseur OAuth2 Google");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email introuvable depuis Google OAuth2");
            return;
        }

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            log.info("Nouvel utilisateur Google détecté, création du compte pour : {}", email);

            String baseUsername = (name != null && !name.isBlank())
                    ? name.replaceAll("\\s+", "_").toLowerCase()
                    : email.split("@")[0].toLowerCase();

            String finalUsername = baseUsername;
            int counter = 1;
            while (userRepository.existsByUsername(finalUsername)) {
                finalUsername = baseUsername + counter++;
            }

            User newUser = User.builder()
                    .username(finalUsername)
                    .email(email)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .role(UserRole.PLAYER)
                    .build();
            User savedUser = userRepository.save(newUser);

            // Création automatique du profil joueur associé
            String baseGamertag = (name != null && !name.isBlank()) ? name : finalUsername;
            String finalGamertag = baseGamertag;
            int tagCounter = 1;
            while (playerRepository.existsByGamertag(finalGamertag)) {
                finalGamertag = baseGamertag + "_" + tagCounter++;
            }

            Player player = Player.builder()
                    .user(savedUser)
                    .gamertag(finalGamertag)
                    .platform(Platform.PS5)
                    .build();
            playerRepository.save(player);

            return savedUser;
        });

        String token = jwtService.generateToken(user.getUsername(), user.getId(), user.getRole().name());

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", token)
                .build().toUriString();

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
