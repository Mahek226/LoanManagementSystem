package com.tss.springsecurity.externalfraud.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@ConditionalOnProperty(name = "external-fraud.enabled", havingValue = "false", matchIfMissing = false)
@Slf4j
public class ExternalFraudFallbackConfig {
    
    public ExternalFraudFallbackConfig() {
        log.warn("External fraud database is disabled. External fraud screening will not be available.");
        log.info("To enable external fraud screening, set 'external-fraud.enabled=true' in application.properties");
    }
}
