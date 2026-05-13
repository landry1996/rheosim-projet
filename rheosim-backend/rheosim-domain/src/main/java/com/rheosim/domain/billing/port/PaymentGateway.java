package com.rheosim.domain.billing.port;

import com.rheosim.domain.billing.model.SubscriptionTier;

public interface PaymentGateway {
    String createCustomer(String email, String name);
    String createSubscription(String customerId, SubscriptionTier tier);
    void cancelSubscription(String subscriptionId);
    String createCheckoutSession(String customerId, SubscriptionTier tier, String successUrl, String cancelUrl);
    String createPortalSession(String customerId, String returnUrl);
}
