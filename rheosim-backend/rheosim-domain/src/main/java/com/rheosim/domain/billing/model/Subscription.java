package com.rheosim.domain.billing.model;

import com.rheosim.domain.shared.BaseEntity;

import java.time.Instant;
import java.util.UUID;

public class Subscription extends BaseEntity {
    private UUID userId;
    private SubscriptionTier tier;
    private String stripeCustomerId;
    private String stripeSubscriptionId;
    private SubscriptionStatus status;
    private Instant currentPeriodStart;
    private Instant currentPeriodEnd;
    private Instant createdAt;

    public Subscription(UUID id, UUID userId, SubscriptionTier tier) {
        super(id);
        this.userId = userId;
        this.tier = tier;
        this.status = SubscriptionStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE;
    }

    public boolean hasFeature(String feature) {
        return TierLimits.hasFeature(tier, feature);
    }

    public UUID getUserId() { return userId; }
    public SubscriptionTier getTier() { return tier; }
    public void setTier(SubscriptionTier tier) { this.tier = tier; }
    public String getStripeCustomerId() { return stripeCustomerId; }
    public void setStripeCustomerId(String stripeCustomerId) { this.stripeCustomerId = stripeCustomerId; }
    public String getStripeSubscriptionId() { return stripeSubscriptionId; }
    public void setStripeSubscriptionId(String stripeSubscriptionId) { this.stripeSubscriptionId = stripeSubscriptionId; }
    public SubscriptionStatus getStatus() { return status; }
    public void setStatus(SubscriptionStatus status) { this.status = status; }
    public Instant getCurrentPeriodStart() { return currentPeriodStart; }
    public void setCurrentPeriodStart(Instant currentPeriodStart) { this.currentPeriodStart = currentPeriodStart; }
    public Instant getCurrentPeriodEnd() { return currentPeriodEnd; }
    public void setCurrentPeriodEnd(Instant currentPeriodEnd) { this.currentPeriodEnd = currentPeriodEnd; }
    public Instant getCreatedAt() { return createdAt; }
}
