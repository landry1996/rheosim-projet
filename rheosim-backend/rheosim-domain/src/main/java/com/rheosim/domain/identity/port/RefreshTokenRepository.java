package com.rheosim.domain.identity.port;

import com.rheosim.domain.identity.model.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken refreshToken);

    Optional<RefreshToken> findByToken(String token);

    void deleteByUserId(UUID userId);

    void deleteExpiredTokens();
}
