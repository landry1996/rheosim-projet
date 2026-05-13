package com.rheosim.infrastructure.marketplace.repository;

import com.rheosim.infrastructure.marketplace.entity.PluginJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PluginJpaRepository extends JpaRepository<PluginJpaEntity, UUID> {

    Optional<PluginJpaEntity> findBySlug(String slug);

    List<PluginJpaEntity> findByAuthorId(UUID authorId);

    Page<PluginJpaEntity> findByStatus(String status, Pageable pageable);

    @Query(value = "SELECT p FROM PluginJpaEntity p WHERE p.status = 'ACTIVE' " +
            "AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<PluginJpaEntity> searchByQuery(@Param("query") String query, Pageable pageable);
}
