package com.tss.springsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicantAuthResponse {
    
    private Long applicantId;
    private String firstName;
    private String lastName;
    private String email;
    private String accessToken;
    private String tokenType = "Bearer";
    private Boolean isApproved;
    private String approvalStatus;
    private String message;
    
    public ApplicantAuthResponse(Long applicantId, String firstName, String lastName, String email, 
                                 String accessToken, Boolean isApproved, String approvalStatus) {
        this.applicantId = applicantId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.accessToken = accessToken;
        this.isApproved = isApproved;
        this.approvalStatus = approvalStatus;
    }
}
