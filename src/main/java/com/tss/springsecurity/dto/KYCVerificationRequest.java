package com.tss.springsecurity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KYCVerificationRequest {
    
    @NotNull(message = "Applicant ID is required")
    private Long applicantId;
    
    @NotBlank(message = "PAN number is required")
    private String panNumber;
    
    @NotBlank(message = "Aadhaar number is required")
    private String aadhaarNumber;
    
    @NotBlank(message = "Verification type is required")
    private String verificationType; // PAN, AADHAAR, BOTH
}
