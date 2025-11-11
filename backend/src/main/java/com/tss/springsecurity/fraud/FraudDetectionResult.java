package com.tss.springsecurity.fraud;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FraudDetectionResult {
    private Long applicantId;
    private String applicantName;
    private int totalFraudScore;
    private String riskLevel; 
    private boolean isFraudulent;
    private List<FraudRule> triggeredRules = new ArrayList<>();
    private String recommendation; 
    
    // Enhanced scoring breakdown
    private Map<String, Object> fraudScoreBreakdown = new HashMap<>();
    private List<String> scoringExplanation = new ArrayList<>();
    
    public void addTriggeredRule(FraudRule rule) {
        this.triggeredRules.add(rule);
        this.totalFraudScore += rule.getFraudPoints();
    }
    
    public void calculateRiskLevel() {
        // Set risk level and fraud status
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
        
        // Generate detailed scoring breakdown
        generateScoreBreakdown();
        generateScoringExplanation();
    }
    
    private void generateScoreBreakdown() {
        // Calculate points by severity
        Map<String, Integer> pointsBySeverity = new HashMap<>();
        pointsBySeverity.put("CRITICAL", 0);
        pointsBySeverity.put("HIGH", 0);
        pointsBySeverity.put("MEDIUM", 0);
        pointsBySeverity.put("LOW", 0);
        
        for (FraudRule rule : triggeredRules) {
            String severity = rule.getSeverity();
            pointsBySeverity.put(severity, pointsBySeverity.get(severity) + rule.getFraudPoints());
        }
        
        // Create breakdown map
        fraudScoreBreakdown.put("totalPoints", totalFraudScore);
        fraudScoreBreakdown.put("calculationMethod", "Sum of all triggered rule points");
        fraudScoreBreakdown.put("pointsDistribution", pointsBySeverity);
        
        Map<String, String> thresholds = new HashMap<>();
        thresholds.put("CLEAN", "0-9 points");
        thresholds.put("LOW", "10-29 points");
        thresholds.put("MEDIUM", "30-59 points");
        thresholds.put("HIGH", "60-99 points");
        thresholds.put("CRITICAL", "100+ points");
        fraudScoreBreakdown.put("thresholds", thresholds);
    }
    
    private void generateScoringExplanation() {
        scoringExplanation.clear();
        
        if (triggeredRules.isEmpty()) {
            scoringExplanation.add("No fraud rules were triggered");
            scoringExplanation.add("Total Score = 0 points");
            scoringExplanation.add("Risk Level = CLEAN (no fraud indicators)");
            scoringExplanation.add("Recommendation = APPROVE");
        } else {
            scoringExplanation.add("Step 1: Identified " + triggeredRules.size() + " fraud rule violation(s)");
            
            StringBuilder ruleBreakdown = new StringBuilder("Step 2: Points breakdown - ");
            for (int i = 0; i < triggeredRules.size(); i++) {
                FraudRule rule = triggeredRules.get(i);
                ruleBreakdown.append(rule.getRuleName())
                    .append(" (").append(rule.getSeverity()).append(") = ")
                    .append(rule.getFraudPoints()).append(" points");
                if (i < triggeredRules.size() - 1) {
                    ruleBreakdown.append(", ");
                }
            }
            scoringExplanation.add(ruleBreakdown.toString());
            
            scoringExplanation.add("Step 3: Total Score = " + totalFraudScore + " points");
            scoringExplanation.add("Step 4: " + totalFraudScore + " points falls in " + riskLevel + " risk range");
            
            if (isFraudulent) {
                scoringExplanation.add("Step 5: Above fraud threshold (60+), marked as FRAUDULENT - " + recommendation);
            } else {
                scoringExplanation.add("Step 5: Below fraud threshold (60+), marked as NON-FRAUDULENT but requires " + recommendation);
            }
        }
    }
}
