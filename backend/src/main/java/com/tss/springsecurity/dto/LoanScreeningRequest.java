package com.tss.springsecurity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanScreeningRequest {
    
    @NotNull(message = "Assignment ID is required")
    private Long assignmentId;
    
    @NotNull(message = "Action is required")
    private String action; // APPROVE, REJECT, ESCALATE_TO_COMPLIANCE
    
    private String remarks;
    
    private String rejectionReason;
}
