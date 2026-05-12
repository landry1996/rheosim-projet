package com.rheosim.application.identity.usecase;

import com.rheosim.application.identity.dto.*;
import com.rheosim.application.identity.mapper.UserMapper;
import com.rheosim.domain.identity.model.RefreshToken;
import com.rheosim.domain.identity.model.Role;
import com.rheosim.domain.identity.model.RoleName;
import com.rheosim.domain.identity.model.User;
import com.rheosim.domain.identity.port.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationUseCaseTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenProvider tokenProvider;
    @Mock private UserMapper userMapper;

    private AuthenticationUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AuthenticationUseCase(
                userRepository, roleRepository, refreshTokenRepository,
                passwordEncoder, tokenProvider, userMapper
        );
    }

    @Test
    @DisplayName("register - success with valid request")
    void register_success() {
        RegisterRequest request = new RegisterRequest(
                "elise@acme.com", "SecurePass123!", "Elise", "Durand", "ACME R&D"
        );

        Role engineerRole = new Role(RoleName.ENGINEER, "R&D Engineer");
        User savedUser = User.builder()
                .email("elise@acme.com")
                .passwordHash("$2a$12$hash")
                .firstName("Elise")
                .lastName("Durand")
                .organization("ACME R&D")
                .build();

        when(userRepository.existsByEmail("elise@acme.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("$2a$12$hash");
        when(roleRepository.findByName(RoleName.ENGINEER)).thenReturn(Optional.of(engineerRole));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(tokenProvider.generateAccessToken(any())).thenReturn("access-token");
        when(tokenProvider.generateRefreshToken()).thenReturn("refresh-token");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any())).thenReturn(
                new UserResponse(UUID.randomUUID(), "elise@acme.com", "Elise", "Durand", "ACME R&D", Set.of("ENGINEER"), Instant.now())
        );

        AuthResponse response = useCase.register(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().email()).isEqualTo("elise@acme.com");

        verify(userRepository).save(any(User.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("register - fails when email already exists")
    void register_emailAlreadyExists() {
        RegisterRequest request = new RegisterRequest(
                "existing@acme.com", "Password123!", "Test", "User", null
        );

        when(userRepository.existsByEmail("existing@acme.com")).thenReturn(true);

        assertThatThrownBy(() -> useCase.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login - success with valid credentials")
    void login_success() {
        LoginRequest request = new LoginRequest("elise@acme.com", "SecurePass123!");

        User user = User.builder()
                .email("elise@acme.com")
                .passwordHash("$2a$12$hash")
                .firstName("Elise")
                .lastName("Durand")
                .enabled(true)
                .build();

        when(userRepository.findByEmail("elise@acme.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SecurePass123!", "$2a$12$hash")).thenReturn(true);
        when(userRepository.save(any())).thenReturn(user);
        when(tokenProvider.generateAccessToken(any())).thenReturn("access-token");
        when(tokenProvider.generateRefreshToken()).thenReturn("refresh-token");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any())).thenReturn(
                new UserResponse(UUID.randomUUID(), "elise@acme.com", "Elise", "Durand", null, Set.of(), Instant.now())
        );

        AuthResponse response = useCase.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        verify(userRepository).save(any());
    }

    @Test
    @DisplayName("login - fails with wrong password")
    void login_wrongPassword() {
        LoginRequest request = new LoginRequest("elise@acme.com", "WrongPass!");

        User user = User.builder()
                .email("elise@acme.com")
                .passwordHash("$2a$12$hash")
                .firstName("Elise")
                .lastName("Durand")
                .enabled(true)
                .build();

        when(userRepository.findByEmail("elise@acme.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass!", "$2a$12$hash")).thenReturn(false);

        assertThatThrownBy(() -> useCase.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("login - fails when account is locked")
    void login_accountLocked() {
        LoginRequest request = new LoginRequest("locked@acme.com", "Password!");

        User user = User.builder()
                .email("locked@acme.com")
                .passwordHash("$2a$12$hash")
                .firstName("Locked")
                .lastName("User")
                .enabled(true)
                .build();
        user.lock();

        when(userRepository.findByEmail("locked@acme.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> useCase.login(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disabled or locked");
    }

    @Test
    @DisplayName("refreshToken - success with valid token")
    void refreshToken_success() {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");

        RefreshToken storedToken = new RefreshToken(
                UUID.randomUUID(), "valid-refresh-token", Instant.now().plus(7, ChronoUnit.DAYS)
        );

        User user = User.builder()
                .email("elise@acme.com")
                .passwordHash("hash")
                .firstName("Elise")
                .lastName("Durand")
                .enabled(true)
                .build();

        when(refreshTokenRepository.findByToken("valid-refresh-token")).thenReturn(Optional.of(storedToken));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(storedToken.getUserId())).thenReturn(Optional.of(user));
        when(tokenProvider.generateAccessToken(any())).thenReturn("new-access-token");
        when(tokenProvider.generateRefreshToken()).thenReturn("new-refresh-token");
        when(userMapper.toResponse(any())).thenReturn(
                new UserResponse(UUID.randomUUID(), "elise@acme.com", "Elise", "Durand", null, Set.of(), Instant.now())
        );

        AuthResponse response = useCase.refreshToken(request);

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    @DisplayName("refreshToken - fails with expired token")
    void refreshToken_expired() {
        RefreshTokenRequest request = new RefreshTokenRequest("expired-token");

        RefreshToken expiredToken = new RefreshToken(
                UUID.randomUUID(), "expired-token", Instant.now().minus(1, ChronoUnit.DAYS)
        );

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> useCase.refreshToken(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired or revoked");
    }
}
