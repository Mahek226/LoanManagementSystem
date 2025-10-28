package com.tss.springsecurity.service;

import com.tss.springsecurity.dto.EnhancedLoanScreeningResponse;
import com.tss.springsecurity.dto.EnhancedLoanScreeningResponse.*;
import com.tss.springsecurity.entity.ApplicantLoanDetails;
import com.tss.springsecurity.externalfraud.model.ExternalFraudCheckResult;
import com.tss.springsecurity.externalfraud.model.ExternalFraudFlag;
import com.tss.springsecurity.externalfraud.service.ExternalFraudScreeningService;
import com.tss.springsecurity.fraud.FraudDetectionResult;
import com.tss.springsecurity.fraud.FraudDetectionService;
import com.tss.springsecurity.fraud.FraudRule;
import com.tss.springsecurity.repository.ApplicantLoanDetailsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedLoanScreeningService implements EnhancedLoanScreeningServiceInterface {
    
    private final FraudDetectionService internalFraudService;
    private final ExternalFraudScreeningService externalFraudService;
    private final ApplicantLoanDetailsRepository loanRepository;
    
    @Value("${loan.scoring.internal.max-score:200}")
    private Integer internalMaxScore;
    
    @Value("${loan.scoring.external.max-score:150}")
    private Integer externalMaxScore;
    
    @Value("${loan.scoring.internal.weight:0.6}")
    private Double internalWeight;
    
    @Value("${loan.scoring.external.weight:0.4}")
    private Double externalWeight;
    
    /**
     * Perform enhanced loan screening with normalized scoring
     */
    @Override
    public EnhancedScoringResult performEnhancedScreening(Long applicantId) {
        log.info("Starting enhanced loan screening for applicant ID: {}", applicantId);
        
        long startTime = System.currentTimeMillis();
        EnhancedScoringResult result = new EnhancedScoringResult();
        result.setApplicantId(applicantId);
        result.setScreeningTimestamp(LocalDateTime.now());
        
        try {
            // Step 1: Run internal fraud detection
            log.info("Running internal fraud detection for applicant ID: {}", applicantId);
            FraudDetectionResult internalResult = internalFraudService.runFraudDetection(applicantId);
            result.setInternalResult(internalResult);
            
            // Step 2: Run external fraud screening
            log.info("Running external fraud screening for applicant ID: {}", applicantId);
            ExternalFraudCheckResult externalResult = externalFraudService.screenApplicant(applicantId);
            result.setExternalResult(externalResult);
            
            // Step 3: Calculate normalized scores
            calculateNormalizedScores(result);
            
            // Step 4: Generate detailed breakdown
            generateScoringBreakdown(result);
            
            // Step 5: Compile rule violations
            compileRuleViolations(result);
            
            // Step 6: Determine final recommendation
            determineFinalRecommendation(result);
            
            // Step 7: Update loan risk score
            updateLoanRiskScore(applicantId, result.getNormalizedScore());
            
            log.info("Enhanced loan screening completed for applicant ID: {} - Final Score: {}", 
                    applicantId, result.getNormalizedScore());
            
        } catch (Exception e) {
            log.error("Error during enhanced loan screening for applicant ID: {}", applicantId, e);
            result.setHasErrors(true);
            result.setErrorMessage(e.getMessage());
            result.setNormalizedScore(75.0); // Default to medium-high risk on error
            result.setFinalRiskLevel("HIGH");
            result.setFinalRecommendation("REVIEW");
        } finally {
            result.setTotalScreeningTimeMs(System.currentTimeMillis() - startTime);
        }
        
        return result;
    }
    
    /**
     * Get scoring configuration details
     */
    @Override
    public ScoringConfiguration getScoringConfiguration() {
        ScoringConfiguration config = new ScoringConfiguration();
        config.setInternalMaxScore(internalMaxScore);
        config.setExternalMaxScore(externalMaxScore);
        config.setInternalWeight(internalWeight);
        config.setExternalWeight(externalWeight);
        return config;
    }
    
    private void calculateNormalizedScores(EnhancedScoringResult result) {
        FraudDetectionResult internal = result.getInternalResult();
        ExternalFraudCheckResult external = result.getExternalResult();
        
        // Calculate internal normalized score (0-100)
        double internalNormalized = 0.0;
        if (internal != null) {
            int internalRaw = internal.getTotalFraudScore();
            internalNormalized = Math.min(100.0, (double) internalRaw / internalMaxScore * 100.0);
        }
        
        // Calculate external normalized score (0-100)
        double externalNormalized = 0.0;
        if (external != null) {
            int externalRaw = external.getTotalFraudScore();
            externalNormalized = Math.min(100.0, (double) externalRaw / externalMaxScore * 100.0);
        }
        
        // Calculate weighted combined score
        double combinedScore = (internalNormalized * internalWeight) + (externalNormalized * externalWeight);
        
        // Ensure score is between 0 and 100
        combinedScore = Math.max(0.0, Math.min(100.0, combinedScore));
        
        result.setInternalNormalizedScore(internalNormalized);
        result.setExternalNormalizedScore(externalNormalized);
        result.setNormalizedScore(combinedScore);
        
        // Determine risk level based on normalized score
        String riskLevel = determineRiskLevel(combinedScore);
        result.setFinalRiskLevel(riskLevel);
        
        log.info("Normalized scores - Internal: {}, External: {}, Combined: {}, Risk Level: {}", 
                internalNormalized, externalNormalized, combinedScore, riskLevel);
    }
    
    private String determineRiskLevel(double score) {
        if (score >= 80) return "CRITICAL";
        if (score >= 60) return "HIGH";
        if (score >= 35) return "MEDIUM";
        if (score >= 15) return "LOW";
        return "CLEAN";
    }
    
    private void generateScoringBreakdown(EnhancedScoringResult result) {
        ScoringBreakdown breakdown = new ScoringBreakdown();
        
        // Internal scoring breakdown
        ScoringBreakdown.InternalScoring internalScoring = new ScoringBreakdown.InternalScoring();
        if (result.getInternalResult() != null) {
            FraudDetectionResult internal = result.getInternalResult();
            internalScoring.setRawScore(internal.getTotalFraudScore());
            internalScoring.setMaxPossibleScore(internalMaxScore);
            internalScoring.setNormalizedScore(result.getInternalNormalizedScore());
            internalScoring.setRiskLevel(internal.getRiskLevel());
            internalScoring.setViolatedRulesCount(internal.getTriggeredRules().size());
            internalScoring.setCategories(extractInternalCategories(internal));
        }
        
        // External scoring breakdown
        ScoringBreakdown.ExternalScoring externalScoring = new ScoringBreakdown.ExternalScoring();
        if (result.getExternalResult() != null) {
            ExternalFraudCheckResult external = result.getExternalResult();
            externalScoring.setRawScore(external.getTotalFraudScore());
            externalScoring.setMaxPossibleScore(externalMaxScore);
            externalScoring.setNormalizedScore(result.getExternalNormalizedScore());
            externalScoring.setRiskLevel(external.getRiskLevel());
            externalScoring.setViolatedRulesCount(external.getFraudFlags().size());
            externalScoring.setPersonFound(external.isPersonFound());
            externalScoring.setCategories(extractExternalCategories(external));
        }
        
        breakdown.setInternalScoring(internalScoring);
        breakdown.setExternalScoring(externalScoring);
        breakdown.setNormalizationMethod("Weighted Average with Score Capping");
        breakdown.setCombinationFormula(String.format("(Internal × %.1f) + (External × %.1f)", 
                internalWeight, externalWeight));
        
        result.setScoringBreakdown(breakdown);
    }
    
    private List<String> extractInternalCategories(FraudDetectionResult internal) {
        return internal.getTriggeredRules().stream()
                .map(FraudRule::getCategory)
                .distinct()
                .collect(Collectors.toList());
    }
    
    private List<String> extractExternalCategories(ExternalFraudCheckResult external) {
        return external.getFraudFlags().stream()
                .map(ExternalFraudFlag::getCategory)
                .distinct()
                .collect(Collectors.toList());
    }
    
    private void compileRuleViolations(EnhancedScoringResult result) {
        List<RuleViolation> violations = new ArrayList<>();
        
        // Add internal rule violations
        if (result.getInternalResult() != null) {
            for (FraudRule rule : result.getInternalResult().getTriggeredRules()) {
                RuleViolation violation = new RuleViolation();
                violation.setSource("INTERNAL");
                violation.setRuleCode(rule.getRuleName());
                violation.setRuleName(rule.getRuleName());
                violation.setCategory(rule.getCategory());
                violation.setSeverity(rule.getSeverity());
                violation.setPoints(rule.getFraudPoints());
                violation.setDescription(rule.getRuleDescription());
                violation.setDetails(rule.getFlagDetails());
                violation.setDetectedAt(LocalDateTime.now());
                violations.add(violation);
            }
        }
        
        // Add external rule violations
        if (result.getExternalResult() != null) {
            for (ExternalFraudFlag flag : result.getExternalResult().getFraudFlags()) {
                RuleViolation violation = new RuleViolation();
                violation.setSource("EXTERNAL");
                violation.setRuleCode(flag.getRuleCode());
                violation.setRuleName(flag.getRuleName());
                violation.setCategory(flag.getCategory());
                violation.setSeverity(flag.getSeverity());
                violation.setPoints(flag.getPoints());
                violation.setDescription(flag.getDescription());
                violation.setDetails(flag.getDetails());
                violation.setDetectedAt(LocalDateTime.now());
                violations.add(violation);
            }
        }
        
        result.setRuleViolations(violations);
    }
    
    private void determineFinalRecommendation(EnhancedScoringResult result) {
        double score = result.getNormalizedScore();
        String riskLevel = result.getFinalRiskLevel();
        
        // Check for critical violations
        boolean hasCriticalViolations = result.getRuleViolations().stream()
                .anyMatch(v -> "CRITICAL".equals(v.getSeverity()));
        
        // Check for external critical findings
        boolean hasCriticalExternal = result.getExternalResult() != null && 
                (result.getExternalResult().isHasCriminalRecord() || 
                 result.getExternalResult().getDefaultedLoans() > 0);
        
        if (hasCriticalViolations || hasCriticalExternal || "CRITICAL".equals(riskLevel)) {
            result.setFinalRecommendation("REJECT");
        } else if (score >= 60 || "HIGH".equals(riskLevel)) {
            result.setFinalRecommendation("REJECT");
        } else if (score >= 35 || "MEDIUM".equals(riskLevel)) {
            result.setFinalRecommendation("REVIEW");
        } else {
            result.setFinalRecommendation("APPROVE");
        }
    }
    
    private void updateLoanRiskScore(Long applicantId, Double normalizedScore) {
        try {
            List<ApplicantLoanDetails> loans = loanRepository.findByApplicant_ApplicantId(applicantId);
            if (!loans.isEmpty()) {
                ApplicantLoanDetails latestLoan = loans.get(loans.size() - 1);
                latestLoan.setRiskScore(normalizedScore.intValue());
                loanRepository.save(latestLoan);
                log.info("Updated loan risk score to {} for applicant {}", normalizedScore.intValue(), applicantId);
            }
        } catch (Exception e) {
            log.error("Error updating loan risk score for applicant {}", applicantId, e);
        }
    }
    
    // Result class for enhanced scoring
    public static class EnhancedScoringResult {
        private Long applicantId;
        private LocalDateTime screeningTimestamp;
        private long totalScreeningTimeMs;
        
        private FraudDetectionResult internalResult;
        private ExternalFraudCheckResult externalResult;
        
        private Double internalNormalizedScore;
        private Double externalNormalizedScore;
        private Double normalizedScore; // Final combined score (0-100)
        private String finalRiskLevel;
        private String finalRecommendation;
        
        private ScoringBreakdown scoringBreakdown;
        private List<RuleViolation> ruleViolations = new ArrayList<>();
        
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
        
        public Double getInternalNormalizedScore() { return internalNormalizedScore; }
        public void setInternalNormalizedScore(Double internalNormalizedScore) { this.internalNormalizedScore = internalNormalizedScore; }
        
        public Double getExternalNormalizedScore() { return externalNormalizedScore; }
        public void setExternalNormalizedScore(Double externalNormalizedScore) { this.externalNormalizedScore = externalNormalizedScore; }
        
        public Double getNormalizedScore() { return normalizedScore; }
        public void setNormalizedScore(Double normalizedScore) { this.normalizedScore = normalizedScore; }
        
        public String getFinalRiskLevel() { return finalRiskLevel; }
        public void setFinalRiskLevel(String finalRiskLevel) { this.finalRiskLevel = finalRiskLevel; }
        
        public String getFinalRecommendation() { return finalRecommendation; }
        public void setFinalRecommendation(String finalRecommendation) { this.finalRecommendation = finalRecommendation; }
        
        public ScoringBreakdown getScoringBreakdown() { return scoringBreakdown; }
        public void setScoringBreakdown(ScoringBreakdown scoringBreakdown) { this.scoringBreakdown = scoringBreakdown; }
        
        public List<RuleViolation> getRuleViolations() { return ruleViolations; }
        public void setRuleViolations(List<RuleViolation> ruleViolations) { this.ruleViolations = ruleViolations; }
        
        public boolean isHasErrors() { return hasErrors; }
        public void setHasErrors(boolean hasErrors) { this.hasErrors = hasErrors; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}
