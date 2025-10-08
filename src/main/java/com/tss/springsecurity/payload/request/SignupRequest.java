package com.tss.springsecurity.payload.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.Set;

@Data
public class SignupRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Size(max = 50)
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$",
        message = "Password must contain at least one digit, one lowercase, one uppercase letter, one special character, and no whitespace"
    )
    private String password;

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "ROLE_(APPLICANT|LOAN_OFFICER|COMPLIANCE_OFFICER|ADMIN)", 
             message = "Role must be one of: ROLE_APPLICANT, ROLE_LOAN_OFFICER, ROLE_COMPLIANCE_OFFICER, ROLE_ADMIN")
    private String role;

    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must be less than 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must be less than 50 characters")
    private String lastName;
    private String phone;
    // Add other fields specific to different user types as needed
}
