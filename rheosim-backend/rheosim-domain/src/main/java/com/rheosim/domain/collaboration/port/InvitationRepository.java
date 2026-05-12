package com.rheosim.domain.collaboration.port;

import com.rheosim.domain.collaboration.model.Invitation;
import com.rheosim.domain.collaboration.model.InvitationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository {
    Invitation save(Invitation invitation);
    Optional<Invitation> findById(UUID id);
    List<Invitation> findByOrganizationId(UUID organizationId);
    List<Invitation> findByEmail(String email);
    List<Invitation> findByEmailAndStatus(String email, InvitationStatus status);
}
