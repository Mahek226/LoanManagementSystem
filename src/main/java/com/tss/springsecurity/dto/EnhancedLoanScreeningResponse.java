package com.tss.springsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnhancedLoanScreeningResponse {
    
    // Basic loan information
    private Long assignmentId;
    private Long loanId;
    private Long applicantId;
    private String applicantName;
    private String loanType;
    private BigDecimal loanAmount;
    private String status;
    private String remarks;
    private LocalDateTime assignedAt;
    private LocalDateTime processedAt;
    
    // Officer details
    private Long officerId;
    private String officerName;
    private String officerType; // LOAN_OFFICER, COMPLIANCE_OFFICER
    
    // Enhanced scoring information
    private NormalizedRiskScore normalizedRiskScore;
    private ScoringBreakdown scoringBreakdown;
    private List<RuleViolation> ruleViolations;
    private String finalRecommendation; // APPROVE, REVIEW, REJECT
    private Boolean canApproveReject;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NormalizedRiskScore {
        private Double finalScore; // Normalized score out of 100
        private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL
        private String scoreInterpretation; // Human readable explanation
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoringBreakdown {
        private InternalScoring internalScoring;
        private ExternalScoring externalScoring;
        private String normalizationMethod;
        private String combinationFormula;
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class InternalScoring {
            private Integer rawScore;
            private Integer maxPossibleScore;
            private Double normalizedScore; // Out of 100
            private String riskLevel;
            private Integer violatedRulesCount;
            private List<String> categories; // Identity, Financial, Employment, etc.
        }
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ExternalScoring {
            private Integer rawScore;
            private Integer maxPossibleScore;
            private Double normalizedScore; // Out of 100
            private String riskLevel;
            private Integer violatedRulesCount;
            private Boolean personFound;
            private List<String> categories; // Criminal, Loan History, Bank Records, etc.
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleViolation {
        private String source; // INTERNAL, EXTERNAL
        private String ruleCode;
        private String ruleName;
        private String category;
        private String severity;
        private Integer points;
        private String description;
        private String details;
        private LocalDateTime detectedAt;
    }
}
