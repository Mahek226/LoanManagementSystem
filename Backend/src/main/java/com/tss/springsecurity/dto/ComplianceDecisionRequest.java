package com.tss.springsecurity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceDecisionRequest {
    
    @NotNull(message = "Assignment ID is required")
    private Long assignmentId;
    
    @NotNull(message = "Decision is required")
    private String decision; // APPROVE, REJECT, REQUEST_DOCUMENTS
    
    private String remarks;
    
    private String rejectionReason;
    
    // For document resubmission
    private DocumentResubmissionRequest documentResubmission;
}
