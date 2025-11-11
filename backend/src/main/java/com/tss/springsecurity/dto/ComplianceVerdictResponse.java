package com.tss.springsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceVerdictResponse {
    
    private Long verdictId;
    private Long assignmentId;
    private Long loanId;
    private String applicantName;
    private String verdict;
    private String verdictReason;
    private String detailedRemarks;
    private String complianceOfficerName;
    private LocalDateTime verdictTimestamp;
    private String nextAction;
    private String assignedToOfficer;
    private String status;
    private String message;
}
