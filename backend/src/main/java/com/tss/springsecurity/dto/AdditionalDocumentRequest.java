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
public class AdditionalDocumentRequest {
    
    @NotNull(message = "Loan ID is required")
    private Long loanId;
    
    @NotNull(message = "Applicant ID is required")
    private Long applicantId;
    
    @NotEmpty(message = "At least one document type is required")
    private List<String> documentTypes;
    
    @NotBlank(message = "Reason is required")
    private String reason;
    
    private String remarks;
}
