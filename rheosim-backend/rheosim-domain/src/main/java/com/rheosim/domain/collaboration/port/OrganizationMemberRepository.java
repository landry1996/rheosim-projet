package com.rheosim.domain.collaboration.port;

import com.rheosim.domain.collaboration.model.OrganizationMember;
import com.rheosim.domain.collaboration.model.OrganizationRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationMemberRepository {
    OrganizationMember save(OrganizationMember member);
    List<OrganizationMember> findByOrganizationId(UUID organizationId);
    Optional<OrganizationMember> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);
    void deleteByOrganizationIdAndUserId(UUID organizationId, UUID userId);
    boolean existsByOrganizationIdAndUserId(UUID organizationId, UUID userId);
}
