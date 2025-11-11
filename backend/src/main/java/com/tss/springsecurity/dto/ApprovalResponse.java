package com.tss.springsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalResponse {
    
    private Long applicantId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String approvalStatus;
    private Boolean isApproved;
    private Boolean isEmailVerified;
    private LocalDateTime createdAt;
}
