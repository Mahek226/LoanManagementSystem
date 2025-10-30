package com.tss.springsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicantLoanDetailsDTO {
    private Long loanId;
    private String loanType;
    private BigDecimal loanAmount;
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private String status;
    private String loanPurpose;
    private String applicationStatus;
    private String loanStatus;
    private Integer riskScore;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    
    // Applicant basic info (not full applicant to avoid circular reference)
    private Long applicantId;
    private String applicantFirstName;
    private String applicantLastName;
    private String applicantEmail;
    private String applicantPhone;
}
