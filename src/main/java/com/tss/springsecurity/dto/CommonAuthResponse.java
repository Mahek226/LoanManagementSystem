package com.tss.springsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonAuthResponse {
    
    private Long userId;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String userType; // "ADMIN", "APPLICANT", "LOAN_OFFICER", "COMPLIANCE_OFFICER"
    private String accessToken;
    private String tokenType = "Bearer";
    private String message;
    
    // Constructor for successful login
    public CommonAuthResponse(Long userId, String username, String firstName, String lastName, 
                             String email, String userType, String accessToken) {
        this.userId = userId;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.userType = userType;
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.message = "Login successful";
    }
    
    // Constructor for admin (no firstName/lastName)
    public CommonAuthResponse(Long userId, String username, String email, 
                             String userType, String accessToken) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.userType = userType;
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.message = "Login successful";
    }
}
