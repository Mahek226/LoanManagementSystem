package com.tss.springsecurity.externalfraud.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalFraudCheckResult {
    
    private boolean personFound;
    private Long externalPersonId;
    private String matchedBy; // PAN, AADHAAR, PHONE, EMAIL, NAME_DOB
    
    // Overall fraud assessment
    private int totalFraudScore;
    private double riskScorePercentage; // Normalized score as percentage (0-100%)
    private String riskLevel; // CLEAN, LOW, MEDIUM, HIGH, CRITICAL
    private boolean isFraudulent;
    private String recommendation; // APPROVE, REVIEW, REJECT
    
    // Detailed findings
    @Builder.Default
    private List<ExternalFraudFlag> fraudFlags = new ArrayList<>();
    
    // Criminal record findings
    private boolean hasCriminalRecord;
    private long totalCriminalCases;
    private long convictedCases;
    private long openCases;
    @Builder.Default
    private List<String> criminalCaseTypes = new ArrayList<>();
    
    // Loan history findings
    private boolean hasLoanHistory;
    private long totalLoans;
    private long activeLoans;
    private long defaultedLoans;
    private BigDecimal totalOutstandingAmount;
    private String worstLoanStatus;
    
    // Bank record findings
    private boolean hasBankRecords;
    private long totalBankAccounts;
    private BigDecimal totalBankBalance;
    
    // Document verification findings
    private boolean hasDocumentIssues;
    @Builder.Default
    private List<String> documentIssues = new ArrayList<>();
    
    // Metadata
    private LocalDateTime screeningTimestamp;
    private long screeningDurationMs;
    private String screeningVersion;
    
    public void addFraudFlag(ExternalFraudFlag flag) {
        if (this.fraudFlags == null) {
            this.fraudFlags = new ArrayList<>();
        }
        this.fraudFlags.add(flag);
        this.totalFraudScore += flag.getPoints();
    }
    
    public void calculateRiskLevel() {
        // Calculate normalized percentage (assuming max possible score is around 500)
        // You can adjust MAX_POSSIBLE_SCORE based on your business rules
        final int MAX_POSSIBLE_SCORE = 500;
        this.riskScorePercentage = Math.min(100.0, (double) totalFraudScore / MAX_POSSIBLE_SCORE * 100);
        
        // Round to 2 decimal places
        this.riskScorePercentage = Math.round(this.riskScorePercentage * 100.0) / 100.0;
        
        if (totalFraudScore >= 200) {
            this.riskLevel = "CRITICAL";
            this.isFraudulent = true;
            this.recommendation = "REJECT";
        } else if (totalFraudScore >= 150) {
            this.riskLevel = "HIGH";
            this.isFraudulent = true;
            this.recommendation = "REJECT";
        } else if (totalFraudScore >= 100) {
            this.riskLevel = "MEDIUM";
            this.isFraudulent = false;
            this.recommendation = "REVIEW";
        } else if (totalFraudScore >= 20) {
            this.riskLevel = "LOW";
            this.isFraudulent = false;
            this.recommendation = "REVIEW";
        } else {
            this.riskLevel = "CLEAN";
            this.isFraudulent = false;
            this.recommendation = "APPROVE";
        }
    }
    
    /**
     * Get formatted risk score percentage as string
     * @return Formatted percentage string (e.g., "63.00%")
     */
    public String getFormattedRiskPercentage() {
        return String.format("%.2f%%", this.riskScorePercentage);
    }
}
