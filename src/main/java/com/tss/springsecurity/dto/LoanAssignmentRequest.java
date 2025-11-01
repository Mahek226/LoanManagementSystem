package com.tss.springsecurity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanAssignmentRequest {
    
    @NotNull(message = "Loan ID is required")
    private Long loanId;
    
    // Optional: If null, system will auto-assign based on loan type and officer workload
    private Long officerId;
    
    private String priority = "MEDIUM"; // LOW, MEDIUM, HIGH
    
    private String remarks;
}
