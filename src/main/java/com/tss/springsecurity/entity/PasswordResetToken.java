package com.tss.springsecurity.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_token")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Long tokenId;
    
    @Column(name = "email", nullable = false, length = 150)
    private String email;
    
    @Column(name = "user_type", nullable = false, length = 50)
    private String userType; // ADMIN, APPLICANT, LOAN_OFFICER, COMPLIANCE_OFFICER
    
    @Column(name = "reset_token", nullable = false, columnDefinition = "TEXT")
    private String resetToken;
    
    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "used_at")
    private LocalDateTime usedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusHours(1); // 1 hour expiry
        }
    }
}
