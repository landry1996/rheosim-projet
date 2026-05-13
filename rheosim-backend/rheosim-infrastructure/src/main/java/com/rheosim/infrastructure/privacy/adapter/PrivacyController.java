package com.rheosim.infrastructure.privacy.adapter;

import com.rheosim.application.privacy.dto.DataExportResponse;
import com.rheosim.application.privacy.usecase.PrivacyUseCase;
import com.rheosim.domain.identity.model.User;
import com.rheosim.domain.privacy.model.DeletionRequest;
import com.rheosim.domain.privacy.model.UserConsent;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @GetMapping("/export")
    public ResponseEntity<DataExportResponse> exportData(@AuthenticationPrincipal User currentUser) {
        DataExportResponse response = privacyUseCase.requestDataExport(currentUser.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/delete-account")
    public ResponseEntity<DeletionRequest> requestDeletion(
            @AuthenticationPrincipal User currentUser,
            @RequestBody DeletionRequestDto request) {
        DeletionRequest deletion = privacyUseCase.requestAccountDeletion(currentUser.getId(), request.reason());
        return ResponseEntity.accepted().body(deletion);
    }

    @PostMapping("/delete-account/{requestId}/confirm")
    public ResponseEntity<DeletionRequest> confirmDeletion(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal User currentUser) {
        DeletionRequest confirmed = privacyUseCase.confirmDeletion(requestId);
        return ResponseEntity.ok(confirmed);
    }

    @PostMapping("/delete-account/{requestId}/cancel")
    public ResponseEntity<DeletionRequest> cancelDeletion(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal User currentUser) {
        DeletionRequest cancelled = privacyUseCase.cancelDeletion(requestId);
        return ResponseEntity.ok(cancelled);
    }

    @GetMapping("/consents")
    public ResponseEntity<List<UserConsent>> getConsents(@AuthenticationPrincipal User currentUser) {
        List<UserConsent> consents = privacyUseCase.getConsents(currentUser.getId());
        return ResponseEntity.ok(consents);
    }

    @PutMapping("/consents")
    public ResponseEntity<Void> updateConsent(
            @AuthenticationPrincipal User currentUser,
            @RequestBody ConsentUpdateDto request,
            @RequestHeader(value = "X-Forwarded-For", required = false, defaultValue = "unknown") String ipAddress,
            @RequestHeader("User-Agent") String userAgent) {
        privacyUseCase.updateConsent(currentUser.getId(), request.type(), request.granted(), ipAddress, userAgent);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/contact")
    public ResponseEntity<Void> contactDpo(@RequestBody DpoContactDto request) {
        return ResponseEntity.accepted().build();
    }

    record DeletionRequestDto(String reason) {}
    record ConsentUpdateDto(UserConsent.ConsentType type, boolean granted) {}
    record DpoContactDto(String name, String email, String subject, String message) {}
}
