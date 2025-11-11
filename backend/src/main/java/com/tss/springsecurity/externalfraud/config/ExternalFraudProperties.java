package com.tss.springsecurity.externalfraud.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "external-fraud")
public class ExternalFraudProperties {
    
    private Screening screening = new Screening();
    private Rules rules = new Rules();
    
    @Data
    public static class Screening {
        private boolean enabled = true;
        private long timeoutMs = 10000;
        private int retryAttempts = 3;
        private boolean cacheResults = true;
        private int cacheDurationMinutes = 30;
        private boolean performDeepScreening = false;
    }
    
    @Data
    public static class Rules {
        private int criminalRecordWeight = 100;
        private int loanDefaultWeight = 80;
        private int multipleLoansThreshold = 5;
        private long highDebtThreshold = 1000000;
        private int excessiveBankAccountsThreshold = 10;
        private int documentExpiryWeight = 20;
        private int unverifiedDocumentWeight = 15;
        
        // Risk level thresholds
        private int cleanThreshold = 24;
        private int lowThreshold = 49;
        private int mediumThreshold = 79;
        private int highThreshold = 119;
        // 120+ is CRITICAL
        
        // Scoring weights for combined screening
        private double internalWeight = 0.6;
        private double externalWeight = 0.4;
    }
}
