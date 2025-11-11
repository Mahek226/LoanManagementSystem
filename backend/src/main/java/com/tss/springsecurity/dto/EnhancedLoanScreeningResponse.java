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
    
    // Compliance verdict details (when available)
    private String complianceVerdict; // APPROVED, REJECTED, FLAGGED, CONDITIONAL_APPROVAL
    private String complianceVerdictReason;
    private String complianceRemarks;
    private String complianceOfficerName;
    private LocalDateTime complianceVerdictTimestamp;
    private String nextAction; // What the loan officer should do next
    private Boolean hasComplianceVerdict; // Flag to indicate if compliance review is complete
    
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
        private SeverityBreakdown severityBreakdown;
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
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class SeverityBreakdown {
            private Integer criticalCount;
            private Integer highCount;
            private Integer mediumCount;
            private Integer lowCount;
            private Integer totalViolations;
            private Double severityScore; // Score contribution from severity (0-100)
            private Double pointsScore; // Score contribution from fraud points (0-40)
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
