package com.rheosim.infrastructure.privacy.adapter;

import com.rheosim.application.privacy.usecase.PrivacyUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AccountDeletionScheduler {

    private static final Logger log = LoggerFactory.getLogger(AccountDeletionScheduler.class);

    private final PrivacyUseCase privacyUseCase;

    public AccountDeletionScheduler(PrivacyUseCase privacyUseCase) {
        this.privacyUseCase = privacyUseCase;
    }

    @Scheduled(cron = "0 0 3 * * *") // Run daily at 3 AM
    public void processExpiredDeletions() {
        log.info("Starting scheduled account deletion processing");
        try {
            privacyUseCase.processExpiredDeletions();
            log.info("Account deletion processing completed");
        } catch (Exception e) {
            log.error("Error during account deletion processing", e);
        }
    }
}
