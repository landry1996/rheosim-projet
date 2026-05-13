package com.rheosim.application.billing.usecase;

import com.rheosim.domain.billing.model.Subscription;
import com.rheosim.domain.billing.model.SubscriptionStatus;
import com.rheosim.domain.billing.model.SubscriptionTier;
import com.rheosim.domain.billing.model.TierLimits;
import com.rheosim.domain.billing.port.PaymentGateway;
import com.rheosim.domain.billing.port.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class BillingUseCase {

    private static final Logger log = LoggerFactory.getLogger(BillingUseCase.class);

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentGateway paymentGateway;

    public BillingUseCase(SubscriptionRepository subscriptionRepository, PaymentGateway paymentGateway) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentGateway = paymentGateway;
    }

    public Subscription getOrCreateSubscription(UUID userId) {
        return subscriptionRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Subscription sub = new Subscription(UUID.randomUUID(), userId, SubscriptionTier.FREE);
                    return subscriptionRepository.save(sub);
                });
    }

    public String createCheckoutSession(UUID userId, SubscriptionTier tier, String successUrl, String cancelUrl) {
        Subscription sub = getOrCreateSubscription(userId);
        if (sub.getStripeCustomerId() == null) {
            throw new IllegalStateException("Stripe customer not set up. Call setupCustomer first.");
        }
        return paymentGateway.createCheckoutSession(sub.getStripeCustomerId(), tier, successUrl, cancelUrl);
    }

    public String setupCustomer(UUID userId, String email, String name) {
        Subscription sub = getOrCreateSubscription(userId);
        if (sub.getStripeCustomerId() != null) {
            return sub.getStripeCustomerId();
        }
        String customerId = paymentGateway.createCustomer(email, name);
        sub.setStripeCustomerId(customerId);
        subscriptionRepository.save(sub);
        return customerId;
    }

    public void handleSubscriptionCreated(String stripeSubscriptionId, String stripeCustomerId, String tier) {
        subscriptionRepository.findByStripeCustomerId(stripeCustomerId).ifPresent(sub -> {
            sub.setStripeSubscriptionId(stripeSubscriptionId);
            sub.setTier(SubscriptionTier.valueOf(tier.toUpperCase()));
            sub.setStatus(SubscriptionStatus.ACTIVE);
            subscriptionRepository.save(sub);
        });
    }

    public void handleSubscriptionCancelled(String stripeSubscriptionId) {
        subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId).ifPresent(sub -> {
            sub.setStatus(SubscriptionStatus.CANCELLED);
            sub.setTier(SubscriptionTier.FREE);
            subscriptionRepository.save(sub);
        });
    }

    public String createPortalSession(UUID userId, String returnUrl) {
        Subscription sub = getOrCreateSubscription(userId);
        if (sub.getStripeCustomerId() == null) {
            throw new IllegalStateException("No Stripe customer found");
        }
        return paymentGateway.createPortalSession(sub.getStripeCustomerId(), returnUrl);
    }

    public void enforceQuota(UUID userId, String feature) {
        Subscription sub = getOrCreateSubscription(userId);
        if (!sub.isActive()) {
            throw new IllegalStateException("Subscription is not active");
        }
        if (!TierLimits.hasFeature(sub.getTier(), feature)) {
            throw new IllegalStateException("Feature '" + feature + "' not available on " + sub.getTier() + " plan. Please upgrade.");
        }
    }

    public void processWebhookEvent(String payload) {
        if (payload == null || payload.isBlank()) {
            log.warn("Empty webhook payload received");
            return;
        }

        if (payload.contains("\"type\":\"customer.subscription.created\"") ||
                payload.contains("\"type\": \"customer.subscription.created\"")) {
            log.info("Processing subscription created event");
            extractAndHandleSubscription(payload, true);
        } else if (payload.contains("\"type\":\"customer.subscription.deleted\"") ||
                payload.contains("\"type\": \"customer.subscription.deleted\"")) {
            log.info("Processing subscription deleted event");
            extractAndHandleSubscription(payload, false);
        } else {
            log.debug("Ignoring unhandled webhook event type");
        }
    }

    private void extractAndHandleSubscription(String payload, boolean created) {
        String subscriptionId = extractJsonValue(payload, "\"id\"");
        String customerId = extractJsonValue(payload, "\"customer\"");

        if (subscriptionId == null || customerId == null) {
            log.warn("Could not extract subscription/customer ID from webhook payload");
            return;
        }

        if (created) {
            String plan = extractJsonValue(payload, "\"plan\"");
            String tier = (plan != null && plan.contains("pro")) ? "PRO" : "ENTERPRISE";
            handleSubscriptionCreated(subscriptionId, customerId, tier);
        } else {
            handleSubscriptionCancelled(subscriptionId);
        }
    }

    private String extractJsonValue(String json, String key) {
        int keyIndex = json.indexOf(key);
        if (keyIndex == -1) return null;
        int colonIndex = json.indexOf(':', keyIndex + key.length());
        if (colonIndex == -1) return null;
        int startQuote = json.indexOf('"', colonIndex + 1);
        if (startQuote == -1) return null;
        int endQuote = json.indexOf('"', startQuote + 1);
        if (endQuote == -1) return null;
        return json.substring(startQuote + 1, endQuote);
    }
}
