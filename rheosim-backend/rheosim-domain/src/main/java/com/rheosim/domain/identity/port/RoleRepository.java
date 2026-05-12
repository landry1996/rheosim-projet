package com.rheosim.domain.identity.port;

import com.rheosim.domain.identity.model.Role;
import com.rheosim.domain.identity.model.RoleName;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository {

    Role save(Role role);

    Optional<Role> findById(UUID id);

    Optional<Role> findByName(RoleName name);
}
