package com.rheosim.infrastructure.identity.adapter;

import com.rheosim.domain.identity.model.Role;
import com.rheosim.domain.identity.model.RoleName;
import com.rheosim.domain.identity.port.RoleRepository;
import com.rheosim.infrastructure.identity.entity.RoleJpaEntity;
import com.rheosim.infrastructure.identity.repository.RoleJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class RoleRepositoryAdapter implements RoleRepository {

    private final RoleJpaRepository jpaRepository;

    public RoleRepositoryAdapter(RoleJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Role save(Role role) {
        RoleJpaEntity entity = toJpaEntity(role);
        entity = jpaRepository.save(entity);
        return toDomain(entity);
    }

    @Override
    public Optional<Role> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Role> findByName(RoleName name) {
        return jpaRepository.findByName(name).map(this::toDomain);
    }

    private RoleJpaEntity toJpaEntity(Role role) {
        RoleJpaEntity entity = new RoleJpaEntity();
        entity.setId(role.getId());
        entity.setName(role.getName());
        entity.setDescription(role.getDescription());
        return entity;
    }

    private Role toDomain(RoleJpaEntity entity) {
        return new Role(entity.getId(), entity.getName(), entity.getDescription());
    }
}
