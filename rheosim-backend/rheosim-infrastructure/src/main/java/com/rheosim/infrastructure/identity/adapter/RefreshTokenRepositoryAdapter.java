package com.rheosim.infrastructure.identity.adapter;

import com.rheosim.domain.identity.model.RefreshToken;
import com.rheosim.domain.identity.port.RefreshTokenRepository;
import com.rheosim.infrastructure.identity.entity.RefreshTokenJpaEntity;
import com.rheosim.infrastructure.identity.repository.RefreshTokenJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;

    public RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        RefreshTokenJpaEntity entity = toJpaEntity(refreshToken);
        entity = jpaRepository.save(entity);
        return toDomain(entity);
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return jpaRepository.findByToken(token).map(this::toDomain);
    }

    @Override
    @Transactional
    public void deleteByUserId(UUID userId) {
        jpaRepository.deleteByUserId(userId);
    }

    @Override
    @Transactional
    public void deleteExpiredTokens() {
        jpaRepository.deleteExpiredOrRevokedTokens(Instant.now());
    }

    private RefreshTokenJpaEntity toJpaEntity(RefreshToken token) {
        RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity();
        entity.setId(token.getId());
        entity.setToken(token.getToken());
        entity.setUserId(token.getUserId());
        entity.setExpiresAt(token.getExpiresAt());
        entity.setRevoked(token.isRevoked());
        entity.setCreatedAt(token.getCreatedAt());
        entity.setUpdatedAt(token.getUpdatedAt());
        return entity;
    }

    private RefreshToken toDomain(RefreshTokenJpaEntity entity) {
        RefreshToken token = new RefreshToken(entity.getUserId(), entity.getToken(), entity.getExpiresAt());
        token.setId(entity.getId());
        token.setRevoked(entity.isRevoked());
        token.setCreatedAt(entity.getCreatedAt());
        token.setUpdatedAt(entity.getUpdatedAt());
        return token;
    }
}
