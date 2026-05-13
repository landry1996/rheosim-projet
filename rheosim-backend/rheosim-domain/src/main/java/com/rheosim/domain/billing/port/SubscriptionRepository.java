package com.rheosim.domain.billing.port;

import com.rheosim.domain.billing.model.Subscription;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository {
    Subscription save(Subscription subscription);
    Optional<Subscription> findByUserId(UUID userId);
    Optional<Subscription> findByStripeCustomerId(String stripeCustomerId);
    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);
}
