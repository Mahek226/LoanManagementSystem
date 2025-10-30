package com.tss.springsecurity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateRequest {
    
    @Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
    private String firstName;
    
    @Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")
    private String lastName;
    
    @Email(message = "Email should be valid")
    private String email;
    
    @Size(min = 10, max = 15, message = "Phone number must be between 10 and 15 characters")
    private String phone;
    
    private String address;
    
    private String city;
    
    private String state;
    
    private String country;
}
