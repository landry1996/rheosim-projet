package com.rheosim.application.billing.usecase;

import com.rheosim.domain.billing.model.Subscription;
import com.rheosim.domain.billing.model.SubscriptionStatus;
import com.rheosim.domain.billing.model.SubscriptionTier;
import com.rheosim.domain.billing.model.TierLimits;
import com.rheosim.domain.billing.port.PaymentGateway;
import com.rheosim.domain.billing.port.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class BillingUseCase {

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
}
