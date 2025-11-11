package com.tss.springsecurity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonLoginRequest {
    
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Password is required")
    private String password;
    
    private String userType; // LOAN_OFFICER, COMPLIANCE_OFFICER, ADMIN, APPLICANT
    
    @NotBlank(message = "CAPTCHA token is required")
    private String captchaToken;
}
