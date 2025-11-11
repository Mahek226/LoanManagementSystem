package com.tss.springsecurity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResubmissionRequestDTO {
    
    @NotNull(message = "Document ID is required")
    private Long documentId;
    
    @NotNull(message = "Loan ID is required")
    private Long loanId;
    
    @NotNull(message = "Compliance Officer ID is required")
    private Long complianceOfficerId;
    
    @NotBlank(message = "Resubmission reason is required")
    private String resubmissionReason;
    
    private String specificInstructions;
    
    // If true, sends request directly to applicant; if false, sends to loan officer first
    private Boolean directToApplicant = false;
}
