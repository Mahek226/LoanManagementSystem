package com.tss.springsecurity.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
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
    
    @Pattern(regexp = "Single|Married|Divorced|Widowed", message = "Marital status must be Single, Married, Divorced, or Widowed")
    @Column(name = "marital_status", length = 20)
    private String maritalStatus;
    
    @Size(max = 100, message = "Education must not exceed 100 characters")
    @Column(name = "education", length = 100)
    private String education;
    
    @NotBlank(message = "Nationality is required")
    @Size(min = 2, max = 50, message = "Nationality must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Nationality can only contain letters and spaces")
    @Column(name = "nationality", length = 50)
    private String nationality;
    
    @NotBlank(message = "PAN number is required")
    @Pattern(regexp = "[A-Z]{5}[0-9]{4}[A-Z]{1}", message = "Invalid PAN format. Must be in format ABCDE1234F")
    @Column(name = "pan_number", length = 20)
    private String panNumber;
    
    @NotBlank(message = "Aadhaar number is required")
    @Pattern(regexp = "^[0-9]{12}$", message = "Aadhaar number must be exactly 12 digits")
    @Column(name = "aadhaar_number", length = 20)
    private String aadhaarNumber;
    
    @Pattern(regexp = "^[A-Z]{3}[0-9]{7}$", message = "Voter ID must be in format ABC1234567")
    @Column(name = "voter_id", length = 20)
    private String voterId;
    
    @Pattern(regexp = "^[A-Z]{1}[0-9]{7}$", message = "Passport number must be in format A1234567")
    @Column(name = "passport_number", length = 20)
    private String passportNumber;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
