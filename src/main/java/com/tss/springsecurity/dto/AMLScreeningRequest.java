package com.tss.springsecurity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AMLScreeningRequest {
    
    @NotNull(message = "Applicant ID is required")
    private Long applicantId;
    
    @NotBlank(message = "Applicant name is required")
    private String applicantName;
    
    @NotBlank(message = "PAN number is required")
    private String panNumber;
    
    @NotEmpty(message = "At least one check type is required")
    private List<String> checkTypes; // RBI_DEFAULTERS, FATF_SANCTIONS, OFAC, INTERNAL_BLACKLIST
}
