package com.rheosim.application.privacy.usecase;

import com.rheosim.application.privacy.dto.DataExportResponse;
import com.rheosim.domain.privacy.model.DeletionRequest;
import com.rheosim.domain.privacy.model.UserConsent;
import com.rheosim.domain.privacy.port.DataExportPort;
import com.rheosim.domain.privacy.port.PrivacyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class PrivacyUseCase {

    private final PrivacyRepository privacyRepository;
    private final DataExportPort dataExportPort;

    public PrivacyUseCase(PrivacyRepository privacyRepository, DataExportPort dataExportPort) {
        this.privacyRepository = privacyRepository;
        this.dataExportPort = dataExportPort;
    }

    // === Data Export (Article 15 & 20) ===

    @Transactional(readOnly = true)
    public DataExportResponse requestDataExport(UUID userId) {
        DataExportPort.ExportData data = dataExportPort.collectUserData(userId);
        byte[] zip = dataExportPort.generateExportZip(data);
        String downloadUrl = dataExportPort.uploadExport(zip, userId);

        return new DataExportResponse(
                userId,
                downloadUrl,
                Instant.now().plus(48, ChronoUnit.HOURS),
                zip.length
        );
    }

    // === Account Deletion (Article 17) ===

    @Transactional
    public DeletionRequest requestAccountDeletion(UUID userId, String reason) {
        if (privacyRepository.hasActiveDeletionRequest(userId)) {
            throw new IllegalStateException("A deletion request is already pending");
        }

        DeletionRequest request = DeletionRequest.create(userId, reason);
        privacyRepository.saveDeletionRequest(request);
        return request;
    }

    @Transactional
    public DeletionRequest confirmDeletion(UUID requestId) {
        DeletionRequest request = privacyRepository.findDeletionRequestById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Deletion request not found"));

        DeletionRequest confirmed = request.confirm();
        privacyRepository.saveDeletionRequest(confirmed);
        return confirmed;
    }

    @Transactional
    public DeletionRequest cancelDeletion(UUID requestId) {
        DeletionRequest request = privacyRepository.findDeletionRequestById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Deletion request not found"));

        if (!request.canCancel()) {
            throw new IllegalStateException("Deletion cannot be cancelled in current state");
        }

        DeletionRequest cancelled = request.cancel();
        privacyRepository.saveDeletionRequest(cancelled);
        return cancelled;
    }

    @Transactional
    public void processExpiredDeletions() {
        List<DeletionRequest> expired = privacyRepository.findExpiredPendingDeletions();
        for (DeletionRequest request : expired) {
            privacyRepository.anonymizeUser(request.userId());
            privacyRepository.deleteUserData(request.userId());
            privacyRepository.saveDeletionRequest(request.complete());
        }
    }

    // === Consent Management (Article 7) ===

    @Transactional(readOnly = true)
    public List<UserConsent> getConsents(UUID userId) {
        return privacyRepository.findConsentsByUserId(userId);
    }

    @Transactional
    public void updateConsent(UUID userId, UserConsent.ConsentType type, boolean granted,
                              String ipAddress, String userAgent) {
        UserConsent consent = new UserConsent(
                userId, type, granted,
                granted ? Instant.now() : null,
                granted ? null : Instant.now(),
                ipAddress, userAgent
        );
        privacyRepository.saveConsent(consent);
    }

    @Transactional
    public void revokeAllConsents(UUID userId) {
        for (UserConsent.ConsentType type : UserConsent.ConsentType.values()) {
            if (type != UserConsent.ConsentType.NECESSARY) {
                privacyRepository.revokeConsent(userId, type);
            }
        }
    }
}
