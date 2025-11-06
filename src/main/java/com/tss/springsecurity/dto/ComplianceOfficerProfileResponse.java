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
public class ComplianceOfficerProfileResponse {
    
    private Long officerId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String loanType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Statistics (optional)
    private Long totalAssignments;
    private Long pendingAssignments;
    private Long completedAssignments;
}
