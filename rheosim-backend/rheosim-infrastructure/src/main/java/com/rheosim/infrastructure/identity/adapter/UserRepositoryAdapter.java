package com.rheosim.infrastructure.identity.adapter;

import com.rheosim.domain.identity.model.Role;
import com.rheosim.domain.identity.model.User;
import com.rheosim.domain.identity.port.UserRepository;
import com.rheosim.infrastructure.identity.entity.RoleJpaEntity;
import com.rheosim.infrastructure.identity.entity.UserJpaEntity;
import com.rheosim.infrastructure.identity.repository.UserJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;

    public UserRepositoryAdapter(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = toJpaEntity(user);
        entity = jpaRepository.save(entity);
        return toDomain(entity);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    private UserJpaEntity toJpaEntity(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.getId());
        entity.setEmail(user.getEmail());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setFirstName(user.getFirstName());
        entity.setLastName(user.getLastName());
        entity.setOrganization(user.getOrganization());
        entity.setEnabled(user.isEnabled());
        entity.setLocked(user.isLocked());
        entity.setLastLoginAt(user.getLastLoginAt());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setUpdatedAt(user.getUpdatedAt());

        Set<RoleJpaEntity> roleEntities = user.getRoles().stream()
                .map(this::toRoleJpaEntity)
                .collect(Collectors.toSet());
        entity.setRoles(roleEntities);

        return entity;
    }

    private User toDomain(UserJpaEntity entity) {
        User user = User.builder()
                .email(entity.getEmail())
                .passwordHash(entity.getPasswordHash())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .organization(entity.getOrganization())
                .enabled(entity.isEnabled())
                .build();

        user.setId(entity.getId());
        user.setLocked(entity.isLocked());
        user.setLastLoginAt(entity.getLastLoginAt());
        user.setCreatedAt(entity.getCreatedAt());
        user.setUpdatedAt(entity.getUpdatedAt());

        Set<Role> roles = entity.getRoles().stream()
                .map(this::toRoleDomain)
                .collect(Collectors.toSet());
        user.setRoles(roles);

        return user;
    }

    private RoleJpaEntity toRoleJpaEntity(Role role) {
        RoleJpaEntity entity = new RoleJpaEntity();
        entity.setId(role.getId());
        entity.setName(role.getName());
        entity.setDescription(role.getDescription());
        return entity;
    }

    private Role toRoleDomain(RoleJpaEntity entity) {
        return new Role(entity.getId(), entity.getName(), entity.getDescription());
    }
}
