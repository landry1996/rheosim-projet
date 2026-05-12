package com.rheosim.application.identity.usecase;

import com.rheosim.application.identity.dto.*;
import com.rheosim.application.identity.mapper.UserMapper;
import com.rheosim.domain.identity.event.UserRegisteredEvent;
import com.rheosim.domain.identity.model.RefreshToken;
import com.rheosim.domain.identity.model.Role;
import com.rheosim.domain.identity.model.RoleName;
import com.rheosim.domain.identity.model.User;
import com.rheosim.domain.identity.port.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuthenticationUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final UserMapper userMapper;

    private static final long REFRESH_TOKEN_VALIDITY_DAYS = 7;

    public AuthenticationUseCase(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            TokenProvider tokenProvider,
            UserMapper userMapper
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.userMapper = userMapper;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered: " + request.email());
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.builder()
                .email(request.email())
                .passwordHash(encodedPassword)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .organization(request.organization())
                .enabled(true)
                .build();

        Role defaultRole = roleRepository.findByName(RoleName.ENGINEER)
                .orElseThrow(() -> new IllegalStateException("Default role ENGINEER not found"));
        user.assignRole(defaultRole);

        user = userRepository.save(user);

        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!user.isActive()) {
            throw new IllegalStateException("Account is disabled or locked");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        user.recordLogin();
        userRepository.save(user);

        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (!storedToken.isValid()) {
            refreshTokenRepository.deleteByUserId(storedToken.getUserId());
            throw new IllegalArgumentException("Refresh token expired or revoked");
        }

        storedToken.revoke();
        refreshTokenRepository.save(storedToken);

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new IllegalStateException("User not found for refresh token"));

        return generateAuthResponse(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(token -> {
                    token.revoke();
                    refreshTokenRepository.save(token);
                });
    }

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = tokenProvider.generateAccessToken(user);
        String refreshTokenValue = tokenProvider.generateRefreshToken();

        RefreshToken refreshToken = new RefreshToken(
                user.getId(),
                refreshTokenValue,
                Instant.now().plus(REFRESH_TOKEN_VALIDITY_DAYS, ChronoUnit.DAYS)
        );
        refreshTokenRepository.save(refreshToken);

        UserResponse userResponse = userMapper.toResponse(user);

        return new AuthResponse(accessToken, refreshTokenValue, 900L, userResponse);
    }
}
