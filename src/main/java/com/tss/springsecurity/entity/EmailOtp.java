package com.tss.springsecurity.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_otp")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailOtp {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "otp_id")
    private Long otpId;
    
    @Column(name = "email", nullable = false, length = 150)
    private String email;
    
    @Column(name = "otp_code", nullable = false, length = 10)
    private String otpCode;
    
    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        // OTP expires in 10 minutes
        expiresAt = LocalDateTime.now().plusMinutes(10);
    }
}
