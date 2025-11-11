package com.tss.springsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfficerResponse {
    
    private Long officerId;
    private String username;
    private String email;
    private String loanType;
    private LocalDateTime createdAt;
    private String message;
    
    public OfficerResponse(Long officerId, String username, String email, String loanType, LocalDateTime createdAt) {
        this.officerId = officerId;
        this.username = username;
        this.email = email;
        this.loanType = loanType;
        this.createdAt = createdAt;
    }
}
