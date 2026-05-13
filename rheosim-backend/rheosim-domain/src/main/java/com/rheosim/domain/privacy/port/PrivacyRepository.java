package com.rheosim.domain.privacy.port;

import com.rheosim.domain.privacy.model.DeletionRequest;
import com.rheosim.domain.privacy.model.UserConsent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrivacyRepository {

    // Consents
    List<UserConsent> findConsentsByUserId(UUID userId);
    void saveConsent(UserConsent consent);
    void revokeConsent(UUID userId, UserConsent.ConsentType type);

    // Deletion requests
    Optional<DeletionRequest> findDeletionRequestById(UUID id);
    Optional<DeletionRequest> findPendingDeletionByUserId(UUID userId);
    List<DeletionRequest> findExpiredPendingDeletions();
    void saveDeletionRequest(DeletionRequest request);

    // Data anonymization
    void anonymizeUser(UUID userId);
    void deleteUserData(UUID userId);
    boolean hasActiveDeletionRequest(UUID userId);
}
