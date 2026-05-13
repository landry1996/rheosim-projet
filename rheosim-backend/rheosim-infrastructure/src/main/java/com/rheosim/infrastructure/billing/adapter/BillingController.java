package com.rheosim.infrastructure.billing.adapter;

import com.rheosim.application.billing.usecase.BillingUseCase;
import com.rheosim.domain.billing.model.Subscription;
import com.rheosim.domain.billing.model.SubscriptionTier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    private final BillingUseCase billingUseCase;

    public BillingController(BillingUseCase billingUseCase) {
        this.billingUseCase = billingUseCase;
    }

    @GetMapping("/subscription")
    public ResponseEntity<SubscriptionResponse> getSubscription(@AuthenticationPrincipal UserDetails user) {
        UUID userId = UUID.fromString(user.getUsername());
        Subscription sub = billingUseCase.getOrCreateSubscription(userId);
        return ResponseEntity.ok(new SubscriptionResponse(
                sub.getTier().name(),
                sub.getStatus().name(),
                sub.getCurrentPeriodEnd() != null ? sub.getCurrentPeriodEnd().toString() : null
        ));
    }

    @PostMapping("/checkout")
    public ResponseEntity<Map<String, String>> createCheckout(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody CheckoutRequest request) {
        UUID userId = UUID.fromString(user.getUsername());
        String sessionUrl = billingUseCase.createCheckoutSession(
                userId,
                SubscriptionTier.valueOf(request.tier().toUpperCase()),
                request.successUrl(),
                request.cancelUrl()
        );
        return ResponseEntity.ok(Map.of("url", sessionUrl));
    }

    @PostMapping("/portal")
    public ResponseEntity<Map<String, String>> createPortal(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody PortalRequest request) {
        UUID userId = UUID.fromString(user.getUsername());
        String url = billingUseCase.createPortalSession(userId, request.returnUrl());
        return ResponseEntity.ok(Map.of("url", url));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(@RequestBody String payload,
                                               @RequestHeader("Stripe-Signature") String signature) {
        // Webhook handling: verify signature and process events
        // In production, verify with Stripe SDK
        return ResponseEntity.ok().build();
    }

    record SubscriptionResponse(String tier, String status, String periodEnd) {}
    record CheckoutRequest(String tier, String successUrl, String cancelUrl) {}
    record PortalRequest(String returnUrl) {}
}
