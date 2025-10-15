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
    
    private Long specificOfficerId; // Optional: if admin wants to assign to specific officer
    
    private String priority = "MEDIUM"; // LOW, MEDIUM, HIGH
    
    private String remarks;
}
