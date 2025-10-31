package com.tss.springsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceAuditLogResponse {
    
    private Long logId;
    private Long assignmentId;
    private Long loanId;
    private Long applicantId;
    private Long officerId;
    private String officerName;
    private String action;
    private String decision;
    private String remarks;
    private LocalDateTime timestamp;
    private String ipAddress;
    private List<String> checksPerformed;
}
