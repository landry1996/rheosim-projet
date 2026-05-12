package com.rheosim.domain.collaboration.port;

import com.rheosim.domain.collaboration.model.Organization;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository {
    Organization save(Organization organization);
    Optional<Organization> findById(UUID id);
    Optional<Organization> findBySlug(String slug);
    List<Organization> findByMemberUserId(UUID userId);
    void deleteById(UUID id);
}
