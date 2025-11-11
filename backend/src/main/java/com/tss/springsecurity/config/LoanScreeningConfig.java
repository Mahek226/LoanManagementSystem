package com.tss.springsecurity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "loan.screening")
public class LoanScreeningConfig {
    
    private Integer riskScoreThreshold = 70; // Default threshold
    private String lowRiskAction = "APPROVE_REJECT_ALLOWED";
    private String highRiskAction = "ESCALATE_TO_COMPLIANCE";
    
    public Integer getRiskScoreThreshold() {
        return riskScoreThreshold;
    }
    
    public void setRiskScoreThreshold(Integer riskScoreThreshold) {
        this.riskScoreThreshold = riskScoreThreshold;
    }
    
    public String getLowRiskAction() {
        return lowRiskAction;
    }
    
    public void setLowRiskAction(String lowRiskAction) {
        this.lowRiskAction = lowRiskAction;
    }
    
    public String getHighRiskAction() {
        return highRiskAction;
    }
    
    public void setHighRiskAction(String highRiskAction) {
        this.highRiskAction = highRiskAction;
    }
}
