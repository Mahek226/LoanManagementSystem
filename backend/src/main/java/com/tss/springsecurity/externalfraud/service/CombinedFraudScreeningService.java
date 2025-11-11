package com.tss.springsecurity.externalfraud.service;

import com.tss.springsecurity.externalfraud.model.ExternalFraudCheckResult;
import com.tss.springsecurity.externalfraud.model.ExternalFraudFlag;
import com.tss.springsecurity.fraud.FraudDetectionResult;
import com.tss.springsecurity.fraud.FraudRule;
import com.tss.springsecurity.fraud.FraudDetectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "external-fraud.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class CombinedFraudScreeningService {
    
    @Autowired
    private FraudDetectionService internalFraudService;
    
    @Autowired
    private ExternalFraudScreeningService externalFraudService;
    
    /**
     * Perform combined fraud screening (Internal + External)
     */
    public CombinedFraudResult performCombinedScreening(Long applicantId) {
        log.info("Starting combined fraud screening for applicant ID: {}", applicantId);
        
        long startTime = System.currentTimeMillis();
        CombinedFraudResult combinedResult = new CombinedFraudResult();
        combinedResult.setApplicantId(applicantId);
        combinedResult.setScreeningTimestamp(LocalDateTime.now());
        
        try {
            // Step 1: Run internal fraud detection
            log.info("Running internal fraud detection for applicant ID: {}", applicantId);
            FraudDetectionResult internalResult = internalFraudService.runFraudDetection(applicantId);
            combinedResult.setInternalResult(internalResult);
            
            // Step 2: Run external fraud screening
            log.info("Running external fraud screening for applicant ID: {}", applicantId);
            ExternalFraudCheckResult externalResult = externalFraudService.screenApplicant(applicantId);
            combinedResult.setExternalResult(externalResult);
            
            // Step 3: Combine and analyze results
            combineResults(combinedResult);
            
            log.info("Combined fraud screening completed for applicant ID: {} - Final Risk: {}", 
                    applicantId, combinedResult.getFinalRiskLevel());
            
        } catch (Exception e) {
            log.error("Error during combined fraud screening for applicant ID: {}", applicantId, e);
            combinedResult.setHasErrors(true);
            combinedResult.setErrorMessage(e.getMessage());
            combinedResult.setFinalRiskLevel("HIGH");
            combinedResult.setFinalRecommendation("REVIEW");
        } finally {
            combinedResult.setTotalScreeningTimeMs(System.currentTimeMillis() - startTime);
        }
        
        return combinedResult;
    }
    
    private void combineResults(CombinedFraudResult combinedResult) {
        FraudDetectionResult internal = combinedResult.getInternalResult();
        ExternalFraudCheckResult external = combinedResult.getExternalResult();
        
        // Calculate combined fraud score
        int internalScore = internal != null ? internal.getTotalFraudScore() : 0;
        int externalScore = external != null ? external.getTotalFraudScore() : 0;
        
        // Weight the scores (Internal: 60%, External: 40%)
        int combinedScore = (int) (internalScore * 0.6 + externalScore * 0.4);
        combinedResult.setCombinedFraudScore(combinedScore);
        
        // Combine fraud flags
        List<CombinedFraudFlag> allFlags = new ArrayList<>();
        
        // Add internal flags
        if (internal != null && internal.getTriggeredRules() != null) {
            for (FraudRule rule : internal.getTriggeredRules()) {
                allFlags.add(CombinedFraudFlag.fromInternalRule(rule));
            }
        }
        
        // Add external flags
        if (external != null && external.getFraudFlags() != null) {
            for (ExternalFraudFlag flag : external.getFraudFlags()) {
                allFlags.add(CombinedFraudFlag.fromExternalFlag(flag));
            }
        }
        
        combinedResult.setAllFraudFlags(allFlags);
        
        // Determine final risk level and recommendation
        determineFinalRiskAssessment(combinedResult);
        
        // Generate insights
        generateInsights(combinedResult);
    }
    
    private void determineFinalRiskAssessment(CombinedFraudResult result) {
        int combinedScore = result.getCombinedFraudScore();
        
        // Check for critical external findings
        boolean hasCriticalExternal = result.getExternalResult() != null && 
                (result.getExternalResult().isHasCriminalRecord() || 
                 result.getExternalResult().getDefaultedLoans() > 0);
        
        // Check for critical internal findings
        boolean hasCriticalInternal = result.getInternalResult() != null && 
                "CRITICAL".equals(result.getInternalResult().getRiskLevel());
        
        // Final risk assessment logic
        if (hasCriticalExternal || hasCriticalInternal || combinedScore >= 120) {
            result.setFinalRiskLevel("CRITICAL");
            result.setFinalRecommendation("REJECT");
        } else if (combinedScore >= 80) {
            result.setFinalRiskLevel("HIGH");
            result.setFinalRecommendation("REJECT");
        } else if (combinedScore >= 50) {
            result.setFinalRiskLevel("MEDIUM");
            result.setFinalRecommendation("REVIEW");
        } else if (combinedScore >= 25) {
            result.setFinalRiskLevel("LOW");
            result.setFinalRecommendation("REVIEW");
        } else {
            result.setFinalRiskLevel("CLEAN");
            result.setFinalRecommendation("APPROVE");
        }
        
        // Override logic for specific scenarios
        if (result.getExternalResult() != null && result.getExternalResult().getConvictedCases() > 0) {
            result.setFinalRiskLevel("CRITICAL");
            result.setFinalRecommendation("REJECT");
        }
    }
    
    private void generateInsights(CombinedFraudResult result) {
        List<String> insights = new ArrayList<>();
        
        // Internal vs External comparison
        if (result.getInternalResult() != null && result.getExternalResult() != null) {
            int internalScore = result.getInternalResult().getTotalFraudScore();
            int externalScore = result.getExternalResult().getTotalFraudScore();
            
            if (internalScore > externalScore * 2) {
                insights.add("Internal fraud indicators significantly higher than external - possible data inconsistency");
            } else if (externalScore > internalScore * 2) {
                insights.add("External fraud indicators significantly higher than internal - applicant may have hidden negative history");
            }
        }
        
        // External database insights
        if (result.getExternalResult() != null) {
            if (result.getExternalResult().isPersonFound()) {
                insights.add("Applicant found in external database - comprehensive screening performed");
                
                if (result.getExternalResult().isHasCriminalRecord()) {
                    insights.add("Criminal record found - high risk applicant");
                }
                
                if (result.getExternalResult().getDefaultedLoans() > 0) {
                    insights.add("Previous loan defaults detected - credit risk concern");
                }
                
                if (result.getExternalResult().getActiveLoans() >= 3) {
                    insights.add("Multiple active loans detected - potential over-leveraging");
                }
            } else {
                insights.add("Applicant not found in external database - limited external verification available");
            }
        }
        
        // Risk pattern insights
        long highRiskFlags = result.getAllFraudFlags().stream()
                .mapToLong(flag -> "HIGH".equals(flag.getSeverity()) || "CRITICAL".equals(flag.getSeverity()) ? 1 : 0)
                .sum();
        
        if (highRiskFlags >= 3) {
            insights.add("Multiple high-risk fraud indicators detected across internal and external sources");
        }
        
        result.setInsights(insights);
    }
    
    // Inner classes for combined result structure
    public static class CombinedFraudResult {
        private Long applicantId;
        private LocalDateTime screeningTimestamp;
        private long totalScreeningTimeMs;
        
        private FraudDetectionResult internalResult;
        private ExternalFraudCheckResult externalResult;
        
        private int combinedFraudScore;
        private String finalRiskLevel;
        private String finalRecommendation;
        private List<CombinedFraudFlag> allFraudFlags = new ArrayList<>();
        private List<String> insights = new ArrayList<>();
        
        private boolean hasErrors;
        private String errorMessage;
        
        // Getters and setters
        public Long getApplicantId() { return applicantId; }
        public void setApplicantId(Long applicantId) { this.applicantId = applicantId; }
        
        public LocalDateTime getScreeningTimestamp() { return screeningTimestamp; }
        public void setScreeningTimestamp(LocalDateTime screeningTimestamp) { this.screeningTimestamp = screeningTimestamp; }
        
        public long getTotalScreeningTimeMs() { return totalScreeningTimeMs; }
        public void setTotalScreeningTimeMs(long totalScreeningTimeMs) { this.totalScreeningTimeMs = totalScreeningTimeMs; }
        
        public FraudDetectionResult getInternalResult() { return internalResult; }
        public void setInternalResult(FraudDetectionResult internalResult) { this.internalResult = internalResult; }
        
        public ExternalFraudCheckResult getExternalResult() { return externalResult; }
        public void setExternalResult(ExternalFraudCheckResult externalResult) { this.externalResult = externalResult; }
        
        public int getCombinedFraudScore() { return combinedFraudScore; }
        public void setCombinedFraudScore(int combinedFraudScore) { this.combinedFraudScore = combinedFraudScore; }
        
        public String getFinalRiskLevel() { return finalRiskLevel; }
        public void setFinalRiskLevel(String finalRiskLevel) { this.finalRiskLevel = finalRiskLevel; }
        
        public String getFinalRecommendation() { return finalRecommendation; }
        public void setFinalRecommendation(String finalRecommendation) { this.finalRecommendation = finalRecommendation; }
        
        public List<CombinedFraudFlag> getAllFraudFlags() { return allFraudFlags; }
        public void setAllFraudFlags(List<CombinedFraudFlag> allFraudFlags) { this.allFraudFlags = allFraudFlags; }
        
        public List<String> getInsights() { return insights; }
        public void setInsights(List<String> insights) { this.insights = insights; }
        
        public boolean isHasErrors() { return hasErrors; }
        public void setHasErrors(boolean hasErrors) { this.hasErrors = hasErrors; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
    
    public static class CombinedFraudFlag {
        private String source; // INTERNAL, EXTERNAL
        private String ruleCode;
        private String ruleName;
        private String category;
        private String severity;
        private int points;
        private String description;
        private String details;
        
        public static CombinedFraudFlag fromInternalRule(FraudRule rule) {
            CombinedFraudFlag flag = new CombinedFraudFlag();
            flag.setSource("INTERNAL");
            flag.setRuleCode(rule.getRuleName()); // Using ruleName as code for internal
            flag.setRuleName(rule.getRuleName());
            flag.setCategory(rule.getCategory());
            flag.setSeverity(rule.getSeverity());
            flag.setPoints(rule.getFraudPoints());
            flag.setDescription(rule.getRuleDescription());
            flag.setDetails(rule.getFlagDetails());
            return flag;
        }
        
        public static CombinedFraudFlag fromExternalFlag(ExternalFraudFlag flag) {
            CombinedFraudFlag combinedFlag = new CombinedFraudFlag();
            combinedFlag.setSource("EXTERNAL");
            combinedFlag.setRuleCode(flag.getRuleCode());
            combinedFlag.setRuleName(flag.getRuleName());
            combinedFlag.setCategory(flag.getCategory());
            combinedFlag.setSeverity(flag.getSeverity());
            combinedFlag.setPoints(flag.getPoints());
            combinedFlag.setDescription(flag.getDescription());
            combinedFlag.setDetails(flag.getDetails());
            return combinedFlag;
        }
        
        // Getters and setters
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        
        public String getRuleCode() { return ruleCode; }
        public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
        
        public String getRuleName() { return ruleName; }
        public void setRuleName(String ruleName) { this.ruleName = ruleName; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        
        public int getPoints() { return points; }
        public void setPoints(int points) { this.points = points; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
    }
}
