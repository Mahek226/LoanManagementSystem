package com.tss.springsecurity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanScreeningDecision {
    
    @NotNull(message = "Decision is required")
    @NotBlank(message = "Decision cannot be blank")
    private String decision; // APPROVE, REJECT, ESCALATE, NEED_MORE_INFO
    
    private String remarks;
    
    private String rejectionReason;
    
    private Integer riskAssessment; // 1-100 scale
    
    private String recommendedAction;
    
    private Boolean requiresManagerApproval;
    
    // Additional screening criteria
    private Boolean incomeVerified;
    private Boolean creditCheckPassed;
    private Boolean collateralVerified;
    private Boolean employmentVerified;
    private Boolean identityVerified;
    
    // Officer notes
    private String internalNotes;
    private String nextSteps;
}
