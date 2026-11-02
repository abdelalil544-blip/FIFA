package com.example.FIFA.security.jwt;

import com.example.FIFA.user.entity.User;
import com.example.FIFA.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTests {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        org.springframework.test.util.ReflectionTestUtils.setField(jwtService, "secretKey", "my-super-secret-key-for-fifa-tests-1234567890");
        org.springframework.test.util.ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L);
    }

    @Test
    void generateAndValidateToken_shouldWork() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("bellingham5")
                .email("bellingham@example.com")
                .password("secret")
                .role(UserRole.PLAYER)
                .build();

        String token = jwtService.generateToken("bellingham5", user.getId(), user.getRole().name());

        assertThat(jwtService.extractUsername(token)).isEqualTo("bellingham5");
        assertThat(jwtService.extractRole(token)).isEqualTo("PLAYER");
        assertThat(jwtService.extractUserId(token)).isEqualTo(user.getId());
        assertThat(jwtService.isTokenExpired(token)).isFalse();

        UserDetails details = org.springframework.security.core.userdetails.User.withUsername("bellingham5")
                .password("secret")
                .roles("PLAYER")
                .build();
        assertThat(jwtService.isTokenValid(token, details)).isTrue();
    }
}
