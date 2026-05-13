package com.rheosim.domain.privacy.model;

import java.time.Instant;
import java.util.UUID;

public record DeletionRequest(
        UUID id,
        UUID userId,
        DeletionStatus status,
        Instant requestedAt,
        Instant scheduledDeletionAt,
        Instant completedAt,
        String reason
) {
    public enum DeletionStatus {
        PENDING,
        CONFIRMED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }

    public static final long GRACE_PERIOD_HOURS = 72;
    public static final long RETENTION_DAYS = 30;

    public static DeletionRequest create(UUID userId, String reason) {
        Instant now = Instant.now();
        return new DeletionRequest(
                UUID.randomUUID(),
                userId,
                DeletionStatus.PENDING,
                now,
                now.plusSeconds(GRACE_PERIOD_HOURS * 3600),
                null,
                reason
        );
    }

    public boolean canCancel() {
        return status == DeletionStatus.PENDING || status == DeletionStatus.CONFIRMED;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(scheduledDeletionAt);
    }

    public DeletionRequest confirm() {
        return new DeletionRequest(id, userId, DeletionStatus.CONFIRMED, requestedAt,
                scheduledDeletionAt, null, reason);
    }

    public DeletionRequest cancel() {
        return new DeletionRequest(id, userId, DeletionStatus.CANCELLED, requestedAt,
                scheduledDeletionAt, Instant.now(), reason);
    }

    public DeletionRequest complete() {
        return new DeletionRequest(id, userId, DeletionStatus.COMPLETED, requestedAt,
                scheduledDeletionAt, Instant.now(), reason);
    }
}
