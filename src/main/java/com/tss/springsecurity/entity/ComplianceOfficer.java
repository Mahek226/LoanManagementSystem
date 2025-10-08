package com.tss.springsecurity.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "compliance_officer")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceOfficer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "officer_id")
    private Long officerId;
    
    @Column(name = "username", unique = true, nullable = false, length = 100)
    private String username;
    
    @Column(name = "email", unique = true, nullable = false, length = 150)
    private String email;
    
    @Column(name = "password_hash", nullable = false, columnDefinition = "TEXT")
    private String passwordHash;
    
    @Column(name = "loan_type", length = 100)
    private String loanType;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "complianceOfficer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ComplianceOfficerApplicationAssignment> applicationAssignments;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Convenience method for authentication
    public String getPassword() {
        return this.passwordHash;
    }
    
    public void setPassword(String password) {
        this.passwordHash = password;
    }
}
