package com.tss.springsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComprehensiveDashboardDTO {
    
    // ==================== Application Volume ====================
    private Integer totalApplications;
    private Integer totalApproved;
    private Integer totalRejected;
    private Integer totalPending;
    private Integer totalEscalated;
    
    // ==================== Financial Performance ====================
    private Double totalFundedAmount;
    private Double averageInterestRate;
    private Double averageDTI;
    private Double averageLoanAmount;
    private Double totalPendingAmount;
    private Double totalRejectedAmount;
    
    // ==================== Approval and Performance ====================
    private Double approvalRate;          // (Approved / Total) * 100
    private Double rejectionRate;         // (Rejected / Total) * 100
    private Double pullThroughRate;       // Approved loans that were funded
    private Double defaultRate;           // (Defaulted / Total Approved) * 100
    private Double escalationRate;        // (Escalated / Total) * 100
    
    // ==================== Loan Status ====================
    private Integer goodLoans;            // Low risk, approved
    private Integer badLoans;             // High risk, rejected, defaulted
    private Integer underReviewLoans;     // Currently being reviewed
    
    // ==================== Loan Quality ====================
    private Double underwritingAccuracy;  // Correctly assessed loans
    private Double portfolioYield;        // Average return on approved loans
    private Double loanQualityIndex;      // Composite score
    
    // ==================== Risk Distribution ====================
    private Integer lowRiskCount;
    private Integer mediumRiskCount;
    private Integer highRiskCount;
    private Integer criticalRiskCount;
    
    // ==================== Monthly Trends ====================
    private List<MonthlyTrend> monthlyApplications;
    private List<MonthlyTrend> monthlyApprovals;
    private List<MonthlyTrend> monthlyDefaults;
    private List<MonthlyTrend> monthlyRejections;
    
    // ==================== Loan Breakdown ====================
    private Map<String, Integer> loansByPurpose;      // Purpose -> Count
    private Map<String, Double> amountsByPurpose;     // Purpose -> Total Amount
    private Map<String, Integer> loansByType;         // Type -> Count
    private Map<String, Integer> loansByTerm;         // Term range -> Count
    
    // ==================== Geographic Data ====================
    private List<GeographicData> loansByState;
    private List<GeographicData> loansByCity;
    
    // ==================== Performance by Branch (if applicable) ====================
    private List<BranchPerformance> branchPerformance;
    
    // ==================== Employee Performance ====================
    private List<EmployeePerformance> officerPerformance;
    
    // ==================== Alerts and Risky Loans ====================
    private List<RiskyLoan> riskyLoans;
    private Integer highRiskLoansCount;
    private Integer overdueLoansCount;
    private Integer fraudAlertsCount;
    
    // ==================== Time Period Info ====================
    private LocalDate startDate;
    private LocalDate endDate;
    private String filterType;  // YEAR, MONTH, QUARTER, CUSTOM
    
    // ==================== Nested DTOs ====================
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyTrend {
        private String month;           // "Jan 2025"
        private Integer year;
        private Integer count;
        private Double amount;
        private Double averageAmount;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeographicData {
        private String location;        // State or City name
        private String code;            // State/City code
        private Integer applicationCount;
        private Integer approvedCount;
        private Integer rejectedCount;
        private Double totalAmount;
        private Double averageAmount;
        private Double approvalRate;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BranchPerformance {
        private String branchName;
        private String branchCode;
        private Integer totalApplications;
        private Integer approved;
        private Integer rejected;
        private Double totalAmount;
        private Double approvalRate;
        private Double defaultRate;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeePerformance {
        private Long officerId;
        private String officerName;
        private Integer yearsExperience;
        private Integer totalAssigned;
        private Integer processed;
        private Integer approved;
        private Integer rejected;
        private Integer escalated;
        private Double approvalRate;
        private Double averageProcessingDays;
        private Double underwritingAccuracy;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskyLoan {
        private Long loanId;
        private Long applicantId;
        private String applicantName;
        private String loanType;
        private Double loanAmount;
        private Integer riskScore;
        private String riskLevel;
        private String riskReason;
        private List<String> fraudIndicators;
        private Integer daysPending;
        private String assignedOfficer;
        private String alert;           // HIGH_RISK, FRAUD_DETECTED, OVERDUE, etc.
    }
}
