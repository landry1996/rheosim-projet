package com.rheosim.infrastructure.privacy.adapter;

import com.rheosim.application.privacy.dto.DataExportResponse;
import com.rheosim.application.privacy.usecase.PrivacyUseCase;
import com.rheosim.domain.privacy.model.DeletionRequest;
import com.rheosim.domain.privacy.model.UserConsent;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/privacy")
public class PrivacyController {

    private final PrivacyUseCase privacyUseCase;

    public PrivacyController(PrivacyUseCase privacyUseCase) {
        this.privacyUseCase = privacyUseCase;
    }

    // === Data Export (Article 15 & 20) ===

    @GetMapping("/export")
    public ResponseEntity<DataExportResponse> exportData(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        DataExportResponse response = privacyUseCase.requestDataExport(userId);
        return ResponseEntity.ok(response);
    }

    // === Account Deletion (Article 17) ===

    @PostMapping("/delete-account")
    public ResponseEntity<DeletionRequest> requestDeletion(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody DeletionRequestDto request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        DeletionRequest deletion = privacyUseCase.requestAccountDeletion(userId, request.reason());
        return ResponseEntity.accepted().body(deletion);
    }

    @PostMapping("/delete-account/{requestId}/confirm")
    public ResponseEntity<DeletionRequest> confirmDeletion(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal Jwt jwt) {
        DeletionRequest confirmed = privacyUseCase.confirmDeletion(requestId);
        return ResponseEntity.ok(confirmed);
    }

    @PostMapping("/delete-account/{requestId}/cancel")
    public ResponseEntity<DeletionRequest> cancelDeletion(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal Jwt jwt) {
        DeletionRequest cancelled = privacyUseCase.cancelDeletion(requestId);
        return ResponseEntity.ok(cancelled);
    }

    // === Consent Management (Article 7) ===

    @GetMapping("/consents")
    public ResponseEntity<List<UserConsent>> getConsents(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        List<UserConsent> consents = privacyUseCase.getConsents(userId);
        return ResponseEntity.ok(consents);
    }

    @PutMapping("/consents")
    public ResponseEntity<Void> updateConsent(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody ConsentUpdateDto request,
            @RequestHeader("X-Forwarded-For") String ipAddress,
            @RequestHeader("User-Agent") String userAgent) {
        UUID userId = UUID.fromString(jwt.getSubject());
        privacyUseCase.updateConsent(userId, request.type(), request.granted(), ipAddress, userAgent);
        return ResponseEntity.noContent().build();
    }

    // === DPO Contact ===

    @PostMapping("/contact")
    public ResponseEntity<Void> contactDpo(@RequestBody DpoContactDto request) {
        // Forward to DPO email/ticketing system
        return ResponseEntity.accepted().build();
    }

    // DTOs
    record DeletionRequestDto(String reason) {}
    record ConsentUpdateDto(UserConsent.ConsentType type, boolean granted) {}
    record DpoContactDto(String name, String email, String subject, String message) {}
}
