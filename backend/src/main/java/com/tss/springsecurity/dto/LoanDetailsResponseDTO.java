package com.tss.springsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanDetailsResponseDTO {
    // Basic Loan Information
    private Long loanId;
    private String loanType;
    private BigDecimal loanAmount;
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private String loanPurpose;
    private String loanStatus;
    private String applicationStatus;
    
    // Calculated Fields
    private BigDecimal emi;
    private BigDecimal totalPayable;
    
    // Applicant Information
    private String applicantFirstName;
    private String applicantMiddleName;
    private String applicantLastName;
    private String applicantEmail;
    private String applicantMobile;
    private String applicantPan;
    private String applicantAadhar;
    private String applicantDateOfBirth;
    private String applicantGender;
    private String applicantMaritalStatus;
    
    // Employment Details
    private String employmentType;
    private String employerName;
    private String designation;
    private BigDecimal monthlyIncome;
    
    // Bank Details
    private String bankName;
    private String accountNumber;
    private String ifscCode;
    private String accountType;
    
    // Address Information
    private String currentAddress;
    private String currentCity;
    private String currentState;
    private String currentPincode;
    
    // Risk Assessment
    private Integer riskScore;
    private Integer fraudScore;
    private String fraudStatus;
    
    // Assignment Information
    private String assignedOfficerName;
    private String assignedOfficerEmail;
    
    // Timeline
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime disbursedAt;
    
    // Remarks
    private String remarks;
}
