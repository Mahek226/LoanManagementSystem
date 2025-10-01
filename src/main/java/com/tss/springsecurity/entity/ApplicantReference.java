package com.tss.springsecurity.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "applicant_references")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicantReference {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", referencedColumnName = "applicant_id")
    private Applicant applicant;
    
    @Column(name = "reference_name", length = 150)
    private String referenceName;
    
    @Column(name = "relationship", length = 50)
    private String relationship; // family, friend, colleague, employer
    
    @Column(name = "phone", length = 20)
    private String phone;
    
    @Column(name = "email", length = 150)
    private String email;
    
    @Column(name = "address", columnDefinition = "TEXT")
    private String address;
    
    @Column(name = "occupation", length = 100)
    private String occupation;
    
    @Column(name = "years_known")
    private Integer yearsKnown;
    
    @Column(name = "verification_status", length = 50)
    private String verificationStatus = "pending"; // pending, verified, failed
    
    @Column(name = "verification_notes", columnDefinition = "TEXT")
    private String verificationNotes;
    
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
