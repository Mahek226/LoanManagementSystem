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
import com.tss.springsecurity.repository.FraudRuleDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final FraudRuleDefinitionRepository fraudRuleDefinitionRepository;
    
    // External fraud rule points (hardcoded in ExternalFraudRuleEngine)
    // CRIMINAL_CONVICTION: 100, CRIMINAL_OPEN_CASE: 60, LOAN_DEFAULT_HISTORY: 80,
    // MULTIPLE_ACTIVE_LOANS: 50, HIGH_OUTSTANDING_DEBT: 45, EXCESSIVE_BANK_ACCOUNTS: 30,
    // MULTIPLE_INACTIVE_ACCOUNTS: 25, EXTERNAL_SYSTEM_ERROR: 25, EXPIRED_DOCUMENT: 20, UNVERIFIED_DOCUMENT: 15
    private static final int EXTERNAL_MAX_FRAUD_POINTS = 450;
    
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
            
            // Step 7: Update loan risk score (but NOT status - status is controlled by officers)
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
        Integer internalMaxScore = fraudRuleDefinitionRepository.getTotalFraudPointsFromActiveRules();
        ScoringConfiguration config = new ScoringConfiguration();
        config.setInternalMaxScore(internalMaxScore != null ? internalMaxScore : 0);
        config.setExternalMaxScore(EXTERNAL_MAX_FRAUD_POINTS);
        config.setInternalWeight(null); // No longer using weights
        config.setExternalWeight(null); // No longer using weights
        return config;
    }
    
    private void calculateNormalizedScores(EnhancedScoringResult result) {
        FraudDetectionResult internal = result.getInternalResult();
        ExternalFraudCheckResult external = result.getExternalResult();
        
        // Get raw scores
        int internalRaw = (internal != null) ? internal.getTotalFraudScore() : 0;
        int externalRaw = (external != null) ? external.getTotalFraudScore() : 0;
        int totalRawScore = internalRaw + externalRaw;
        
        // Get max possible score from database for internal rules
        Integer internalMaxScore = fraudRuleDefinitionRepository.getTotalFraudPointsFromActiveRules();
        if (internalMaxScore == null) {
            internalMaxScore = 0;
            log.warn("No active fraud rules found in database, using 0 as max internal score");
        }
        
        // Count violations by severity (CRITICAL, HIGH, MEDIUM, LOW)
        int criticalCount = 0, highCount = 0, mediumCount = 0, lowCount = 0;
        
        // Count internal violations by severity
        if (internal != null && internal.getTriggeredRules() != null) {
            for (FraudRule rule : internal.getTriggeredRules()) {
                String severity = rule.getSeverity();
                if (severity != null) {
                    switch (severity.toUpperCase()) {
                        case "CRITICAL": criticalCount++; break;
                        case "HIGH": highCount++; break;
                        case "MEDIUM": mediumCount++; break;
                        case "LOW": lowCount++; break;
                    }
                }
            }
        }
        
        // Count external violations by severity
        if (external != null && external.getFraudFlags() != null) {
            for (ExternalFraudFlag flag : external.getFraudFlags()) {
                String severity = flag.getSeverity();
                if (severity != null) {
                    switch (severity.toUpperCase()) {
                        case "CRITICAL": criticalCount++; break;
                        case "HIGH": highCount++; break;
                        case "MEDIUM": mediumCount++; break;
                        case "LOW": lowCount++; break;
                    }
                }
            }
        }
        
        // ===== SEVERITY + VOLUME HYBRID SCORING =====
        // Base Score: Weighted by violation count per severity (max 60% contribution)
        // Reduced weights to prevent maxing out: CRITICAL=15, HIGH=10, MEDIUM=5, LOW=2
        double baseScore = (criticalCount * 15.0) + (highCount * 10.0) + (mediumCount * 5.0) + (lowCount * 2.0);
        baseScore = Math.min(60.0, baseScore); // Cap severity contribution at 60%
        
        // Points Score: Normalized raw score (40% weight, higher denominator for better differentiation)
        double pointsScore = (totalRawScore / 800.0) * 40.0;
        pointsScore = Math.min(40.0, pointsScore); // Cap at 40%
        
        // Final Combined Score
        double normalizedScore = baseScore + pointsScore;
        normalizedScore = Math.max(0.0, Math.min(100.0, normalizedScore));
        
        // Calculate individual normalized scores for breakdown (using old method for comparison)
        double internalNormalized = 0.0;
        if (internalMaxScore > 0) {
            internalNormalized = Math.min(100.0, (double) internalRaw / internalMaxScore * 100.0);
        }
        
        double externalNormalized = 0.0;
        if (EXTERNAL_MAX_FRAUD_POINTS > 0) {
            externalNormalized = Math.min(100.0, (double) externalRaw / EXTERNAL_MAX_FRAUD_POINTS * 100.0);
        }
        
        result.setInternalNormalizedScore(internalNormalized);
        result.setExternalNormalizedScore(externalNormalized);
        result.setNormalizedScore(normalizedScore);
        
        // Store max scores and severity counts for breakdown
        result.setInternalMaxScore(internalMaxScore);
        result.setExternalMaxScore(EXTERNAL_MAX_FRAUD_POINTS);
        result.setSeverityCounts(new int[]{criticalCount, highCount, mediumCount, lowCount});
        
        // Determine risk level based on normalized score
        String riskLevel = determineRiskLevel(normalizedScore);
        result.setFinalRiskLevel(riskLevel);
        
        log.info("Raw scores - Internal: {}, External: {}, Total: {}", internalRaw, externalRaw, totalRawScore);
        log.info("Severity counts - Critical: {}, High: {}, Medium: {}, Low: {}", criticalCount, highCount, mediumCount, lowCount);
        log.info("Hybrid scoring - Severity base: {} (uncapped: {}), Points: {} (raw/800×40) → Final: {}%", 
                baseScore, 
                (criticalCount * 15.0) + (highCount * 10.0) + (mediumCount * 5.0) + (lowCount * 2.0),
                String.format("%.1f", pointsScore), 
                String.format("%.1f", normalizedScore));
        log.info("Risk Level: {} (Old pure proportional method would give: {}%)", riskLevel, 
                String.format("%.2f", ((double) totalRawScore / (internalMaxScore + EXTERNAL_MAX_FRAUD_POINTS)) * 100.0));
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
            internalScoring.setMaxPossibleScore(result.getInternalMaxScore());
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
            externalScoring.setMaxPossibleScore(result.getExternalMaxScore());
            externalScoring.setNormalizedScore(result.getExternalNormalizedScore());
            externalScoring.setRiskLevel(external.getRiskLevel());
            externalScoring.setViolatedRulesCount(external.getFraudFlags().size());
            externalScoring.setPersonFound(external.isPersonFound());
            externalScoring.setCategories(extractExternalCategories(external));
        }
        
        // Severity breakdown
        ScoringBreakdown.SeverityBreakdown severityBreakdown = new ScoringBreakdown.SeverityBreakdown();
        if (result.getSeverityCounts() != null && result.getSeverityCounts().length >= 4) {
            int[] counts = result.getSeverityCounts();
            severityBreakdown.setCriticalCount(counts[0]);
            severityBreakdown.setHighCount(counts[1]);
            severityBreakdown.setMediumCount(counts[2]);
            severityBreakdown.setLowCount(counts[3]);
            severityBreakdown.setTotalViolations(counts[0] + counts[1] + counts[2] + counts[3]);
            
            // Calculate score contributions (using new reduced weights)
            double baseSeverityScore = (counts[0] * 15.0) + (counts[1] * 10.0) + (counts[2] * 5.0) + (counts[3] * 2.0);
            baseSeverityScore = Math.min(60.0, baseSeverityScore); // Cap at 60%
            int totalRawScore = (result.getInternalResult() != null ? result.getInternalResult().getTotalFraudScore() : 0) +
                               (result.getExternalResult() != null ? result.getExternalResult().getTotalFraudScore() : 0);
            double pointsContribution = Math.min(40.0, (totalRawScore / 800.0) * 40.0);
            
            severityBreakdown.setSeverityScore(baseSeverityScore);
            severityBreakdown.setPointsScore(pointsContribution);
        }
        
        breakdown.setInternalScoring(internalScoring);
        breakdown.setExternalScoring(externalScoring);
        breakdown.setSeverityBreakdown(severityBreakdown);
        breakdown.setNormalizationMethod("Severity + Volume Hybrid Scoring");
        breakdown.setCombinationFormula("Base = min(60, Critical×15 + High×10 + Medium×5 + Low×2) + Points = min(40, RawScore/800×40)");
        
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
                // Update ONLY risk_score, do NOT update status (status is controlled by officers)
                latestLoan.setRiskScore(normalizedScore.intValue());
                loanRepository.save(latestLoan);
                log.info("Updated loan risk score to {} for applicant {} (status unchanged)", 
                        normalizedScore.intValue(), applicantId);
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
        private Integer internalMaxScore; // Max score from database
        private Integer externalMaxScore; // Max score hardcoded
        private int[] severityCounts; // [critical, high, medium, low]
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
        
        public Integer getInternalMaxScore() { return internalMaxScore; }
        public void setInternalMaxScore(Integer internalMaxScore) { this.internalMaxScore = internalMaxScore; }
        
        public Integer getExternalMaxScore() { return externalMaxScore; }
        public void setExternalMaxScore(Integer externalMaxScore) { this.externalMaxScore = externalMaxScore; }
        
        public int[] getSeverityCounts() { return severityCounts; }
        public void setSeverityCounts(int[] severityCounts) { this.severityCounts = severityCounts; }
        
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
