package com.tss.springsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanAssignmentResponse {
    
    private Long assignmentId;
    private Long loanId;
    private Long applicantId;
    private String applicantName;
    private String loanType;
    private Long officerId;
    private String officerName;
    private String officerEmail;
    private String status;
    private String priority;
    private String remarks;
    private LocalDateTime assignedAt;
    private LocalDateTime updatedAt;
}
