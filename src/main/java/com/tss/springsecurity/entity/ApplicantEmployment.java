package com.tss.springsecurity.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "applicant_employment")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicantEmployment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", referencedColumnName = "applicant_id")
    private Applicant applicant;
    
    @Column(name = "employment_type", length = 50)
    private String employmentType;
    
    @Size(min = 2, max = 200, message = "Company name must be between 2 and 200 characters")
    @Column(name = "company_name", length = 200)
    private String companyName;
    
    @Size(max = 100, message = "Designation must not exceed 100 characters")
    @Column(name = "designation", length = 100)
    private String designation;
    
    @Column(name = "work_experience")
    private Integer workExperience;
    
    @Column(name = "office_address", columnDefinition = "TEXT")
    private String officeAddress;
    
    @Column(name = "office_city", length = 100)
    private String officeCity;
    
    @Column(name = "office_state", length = 100)
    private String officeState;
    
    @Column(name = "office_pincode", length = 10)
    private String officePincode;
    
    @Column(name = "monthly_income", precision = 12, scale = 2)
    private BigDecimal monthlyIncome;
    
    @Pattern(regexp = "pending|verified|rejected|in_progress", message = "Verified status must be pending, verified, rejected, or in_progress")
    @Column(name = "verified_status", length = 50)
    private String verifiedStatus = "pending";
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
