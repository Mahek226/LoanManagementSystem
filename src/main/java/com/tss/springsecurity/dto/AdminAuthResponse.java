package com.tss.springsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuthResponse {
    
    private Long adminId;
    private String username;
    private String email;
    private String accessToken;
    private String tokenType = "Bearer";
    
    public AdminAuthResponse(Long adminId, String username, String email, String accessToken) {
        this.adminId = adminId;
        this.username = username;
        this.email = email;
        this.accessToken = accessToken;
    }
}
