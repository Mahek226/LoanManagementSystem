package com.tss.springsecurity.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResubmissionRequest {
    
    @NotNull(message = "Assignment ID is required")
    private Long assignmentId;
    
    @NotEmpty(message = "At least one document type must be specified")
    private List<String> documentTypes; // AADHAAR, PAN, PASSPORT, INCOME_PROOF, etc.
    
    @NotNull(message = "Reason is required")
    private String reason;
    
    private String additionalComments;
    
    private Integer priorityLevel = 1; // 1-5, where 5 is highest priority
}
