package com.rheosim.domain.billing.model;

import java.util.Map;
import java.util.Set;

public final class TierLimits {

    private TierLimits() {}

    public static final Map<SubscriptionTier, Integer> MAX_PROJECTS = Map.of(
            SubscriptionTier.FREE, 1,
            SubscriptionTier.PRO, Integer.MAX_VALUE,
            SubscriptionTier.ENTERPRISE, Integer.MAX_VALUE
    );

    public static final Map<SubscriptionTier, Integer> SIMULATIONS_PER_MONTH = Map.of(
            SubscriptionTier.FREE, 5,
            SubscriptionTier.PRO, Integer.MAX_VALUE,
            SubscriptionTier.ENTERPRISE, Integer.MAX_VALUE
    );

    public static final Map<SubscriptionTier, Long> STORAGE_BYTES = Map.of(
            SubscriptionTier.FREE, 100L * 1024 * 1024,        // 100 MB
            SubscriptionTier.PRO, 10L * 1024 * 1024 * 1024,   // 10 GB
            SubscriptionTier.ENTERPRISE, Long.MAX_VALUE
    );

    public static final Map<SubscriptionTier, Integer> MAX_COLLABORATORS = Map.of(
            SubscriptionTier.FREE, 2,
            SubscriptionTier.PRO, 10,
            SubscriptionTier.ENTERPRISE, Integer.MAX_VALUE
    );

    private static final Map<SubscriptionTier, Set<String>> TIER_FEATURES = Map.of(
            SubscriptionTier.FREE, Set.of("basic_simulation", "data_import"),
            SubscriptionTier.PRO, Set.of("basic_simulation", "data_import", "fem_3d", "gpu_compute",
                    "ml_calibration", "marketplace_upload", "collaboration"),
            SubscriptionTier.ENTERPRISE, Set.of("basic_simulation", "data_import", "fem_3d", "gpu_compute",
                    "ml_calibration", "marketplace_upload", "collaboration", "on_premise", "sla_99_9")
    );

    public static boolean hasFeature(SubscriptionTier tier, String feature) {
        Set<String> features = TIER_FEATURES.get(tier);
        return features != null && features.contains(feature);
    }

    public static int getMaxProjects(SubscriptionTier tier) {
        return MAX_PROJECTS.getOrDefault(tier, 1);
    }

    public static int getSimulationsPerMonth(SubscriptionTier tier) {
        return SIMULATIONS_PER_MONTH.getOrDefault(tier, 5);
    }

    public static long getStorageBytes(SubscriptionTier tier) {
        return STORAGE_BYTES.getOrDefault(tier, 100L * 1024 * 1024);
    }
}
