package com.rheosim.infrastructure.identity.repository;

import com.rheosim.domain.identity.model.RoleName;
import com.rheosim.infrastructure.identity.entity.RoleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleJpaRepository extends JpaRepository<RoleJpaEntity, UUID> {

    Optional<RoleJpaEntity> findByName(RoleName name);
}
