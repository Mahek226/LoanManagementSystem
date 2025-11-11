package com.tss.springsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceReviewDetailsResponse {
    
    // Assignment and Loan Basic Info
    private Long assignmentId;
    private Long loanId;
    private Long applicantId;
    private String applicantName;
    private String loanType;
    private BigDecimal loanAmount;
    private String applicationStatus;
    
    // Applicant Details
    private String email;
    private String phone;
    private String panNumber;
    private String aadhaarNumber;
    private LocalDate dateOfBirth;
    
    // Documents Section
    private List<DocumentInfo> documents;
    
    // External Fraud Data Section
    private ExternalFraudDataResponse externalFraudData;
    
    // Screening Results Section
    private ScreeningResultsResponse screeningResults;
    
    // Risk Assessment
    private RiskAssessmentResponse riskAssessment;
    
    // History
    private List<ComplianceActionHistory> actionHistory;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentInfo {
        private Long documentId;
        private String documentType;
        private String fileName;
        private String fileUrl;
        private String uploadStatus;
        private LocalDateTime uploadedAt;
        private Boolean resubmissionRequested;
        private String resubmissionReason;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExternalFraudDataResponse {
        private boolean personFound;
        private Long externalPersonId;
        private List<BankRecordInfo> bankRecords;
        private List<CriminalRecordInfo> criminalRecords;
        private List<LoanHistoryInfo> loanHistory;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BankRecordInfo {
        private String bankName;
        private String accountType;
        private BigDecimal balanceAmount;
        private LocalDate lastTransactionDate;
        private Boolean isActive;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CriminalRecordInfo {
        private String caseNumber;
        private String caseType;
        private String description;
        private String courtName;
        private String status;
        private LocalDate verdictDate;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoanHistoryInfo {
        private String loanType;
        private String institutionName;
        private BigDecimal loanAmount;
        private BigDecimal outstandingBalance;
        private LocalDate startDate;
        private LocalDate endDate;
        private String status;
        private Boolean defaultFlag;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScreeningResultsResponse {
        private String riskLevel;
        private Integer totalRiskScore;
        private Double riskScorePercentage;
        private String recommendation;
        private List<RuleViolation> violations;
        private List<FraudFlag> flags;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleViolation {
        private String ruleCode;
        private String ruleName;
        private String category;
        private String severity;
        private Integer points;
        private String description;
        private String details;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FraudFlag {
        private String flagCode;
        private String flagName;
        private String category;
        private String severity;
        private Integer points;
        private String description;
        private String details;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskAssessmentResponse {
        private String overallRiskLevel;
        private Integer combinedScore;
        private Double normalizedScore;
        private String internalRiskLevel;
        private Integer internalScore;
        private String externalRiskLevel;
        private Integer externalScore;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComplianceActionHistory {
        private Long actionId;
        private String actionType;
        private String performedBy;
        private String remarks;
        private LocalDateTime timestamp;
    }
}
