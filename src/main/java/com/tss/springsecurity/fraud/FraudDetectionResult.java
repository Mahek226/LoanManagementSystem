package com.tss.springsecurity.fraud;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FraudDetectionResult {
    private Long applicantId;
    private String applicantName;
    private int totalFraudScore;
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL
    private boolean isFraudulent;
    private List<FraudRule> triggeredRules = new ArrayList<>();
    private String recommendation; // APPROVE, REVIEW, REJECT
    
    public void addTriggeredRule(FraudRule rule) {
        this.triggeredRules.add(rule);
        this.totalFraudScore += rule.getFraudPoints();
    }
    
    public void calculateRiskLevel() {
        if (totalFraudScore >= 100) {
            this.riskLevel = "CRITICAL";
            this.isFraudulent = true;
            this.recommendation = "REJECT";
        } else if (totalFraudScore >= 60) {
            this.riskLevel = "HIGH";
            this.isFraudulent = true;
            this.recommendation = "REJECT";
        } else if (totalFraudScore >= 30) {
            this.riskLevel = "MEDIUM";
            this.isFraudulent = false;
            this.recommendation = "REVIEW";
        } else if (totalFraudScore >= 10) {
            this.riskLevel = "LOW";
            this.isFraudulent = false;
            this.recommendation = "REVIEW";
        } else {
            this.riskLevel = "CLEAN";
            this.isFraudulent = false;
            this.recommendation = "APPROVE";
        }
    }
}
