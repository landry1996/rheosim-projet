package com.rheosim.infrastructure.billing.adapter;

import com.rheosim.application.billing.usecase.BillingUseCase;
import com.rheosim.domain.billing.model.Subscription;
import com.rheosim.domain.billing.model.SubscriptionTier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    private static final Logger log = LoggerFactory.getLogger(BillingController.class);

    private final BillingUseCase billingUseCase;

    @Value("${rheosim.stripe.webhook-secret:}")
    private String stripeWebhookSecret;

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
        if (stripeWebhookSecret == null || stripeWebhookSecret.isBlank()) {
            log.error("Stripe webhook secret not configured");
            return ResponseEntity.status(500).build();
        }

        if (!verifyStripeSignature(payload, signature, stripeWebhookSecret)) {
            log.warn("Invalid Stripe webhook signature");
            return ResponseEntity.status(400).build();
        }

        billingUseCase.processWebhookEvent(payload);
        return ResponseEntity.ok().build();
    }

    private boolean verifyStripeSignature(String payload, String sigHeader, String secret) {
        try {
            String[] elements = sigHeader.split(",");
            String timestamp = null;
            String expectedSig = null;
            for (String element : elements) {
                String[] kv = element.split("=", 2);
                if (kv.length == 2) {
                    if ("t".equals(kv[0])) timestamp = kv[1];
                    if ("v1".equals(kv[0])) expectedSig = kv[1];
                }
            }
            if (timestamp == null || expectedSig == null) return false;

            long ts = Long.parseLong(timestamp);
            long tolerance = 300;
            if (Math.abs(System.currentTimeMillis() / 1000 - ts) > tolerance) return false;

            String signedPayload = timestamp + "." + payload;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
            String computedSig = HexFormat.of().formatHex(hash);

            return MessageDigest.isEqual(
                    computedSig.getBytes(StandardCharsets.UTF_8),
                    expectedSig.getBytes(StandardCharsets.UTF_8)
            );
        } catch (NoSuchAlgorithmException | InvalidKeyException | NumberFormatException e) {
            log.error("Stripe signature verification failed", e);
            return false;
        }
    }

    record SubscriptionResponse(String tier, String status, String periodEnd) {}
    record CheckoutRequest(String tier, String successUrl, String cancelUrl) {}
    record PortalRequest(String returnUrl) {}
}
