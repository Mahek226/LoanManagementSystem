package com.tss.springsecurity.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "applicant_basic_details", indexes = {
    @Index(name = "idx_basic_details_applicant_id", columnList = "applicant_id"),
    @Index(name = "idx_basic_details_pan_number", columnList = "pan_number"),
    @Index(name = "idx_basic_details_aadhaar_number", columnList = "aadhaar_number"),
    @Index(name = "idx_basic_details_passport_number", columnList = "passport_number"),
    @Index(name = "idx_basic_details_voter_id", columnList = "voter_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicantBasicDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", referencedColumnName = "applicant_id")
    private Applicant applicant;
    
    @Column(name = "marital_status", length = 20)
    private String maritalStatus;
    
    @Column(name = "education", length = 100)
    private String education;
    
    @Column(name = "nationality", length = 50)
    private String nationality;
    
    @Column(name = "pan_number", length = 20)
    private String panNumber;
    
    @Column(name = "aadhaar_number", length = 20)
    private String aadhaarNumber;
    
    @Column(name = "voter_id", length = 20)
    private String voterId;
    
    @Column(name = "passport_number", length = 20)
    private String passportNumber;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
