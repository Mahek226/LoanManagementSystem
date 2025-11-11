package com.tss.springsecurity.externalfraud.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalFraudCheckRequest {
    
    private String panNumber;
    private String aadhaarNumber;
    private String phoneNumber;
    private String email;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    
    // Additional context for screening
    private String requestedLoanType;
    private Double requestedLoanAmount;
    private Long internalApplicantId;
    
    // Screening options
    private boolean checkCriminalRecords = true;
    private boolean checkLoanHistory = true;
    private boolean checkBankRecords = true;
    private boolean checkDocumentVerification = true;
    private boolean performDeepScreening = false;
}
