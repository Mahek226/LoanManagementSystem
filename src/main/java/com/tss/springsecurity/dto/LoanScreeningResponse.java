package com.tss.springsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanScreeningResponse {
    
    private Long assignmentId;
    private Long loanId;
    private Long applicantId;
    private String applicantName;
    private String loanType;
    private BigDecimal loanAmount;
    private Integer riskScore;
    private String riskLevel; // LOW, MEDIUM, HIGH
    private Boolean canApproveReject; // Based on risk score threshold
    private String status;
    private String remarks;
    private LocalDateTime assignedAt;
    private LocalDateTime processedAt;
    
    // Officer details
    private Long officerId;
    private String officerName;
    private String officerType; // LOAN_OFFICER, COMPLIANCE_OFFICER
}
