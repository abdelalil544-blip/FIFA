package com.example.FIFA.security.user;

import com.example.FIFA.user.entity.User;
import com.example.FIFA.user.enums.UserRole;
import com.example.FIFA.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTests {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void loadUserByUsername_shouldReturnUserDetailsWhenUsernameExists() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("bellingham5")
                .email("bellingham@example.com")
                .password("encoded")
                .role(UserRole.PLAYER)
                .build();

        when(userRepository.findByUsername("bellingham5")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("bellingham5");

        assertThat(details.getUsername()).isEqualTo("bellingham5");
        assertThat(details.getAuthorities()).extracting("authority").contains("ROLE_PLAYER");
    }

    @Test
    void loadUserByUsername_shouldThrowWhenUserDoesNotExist() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("missing")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("missing"));
    }
}
