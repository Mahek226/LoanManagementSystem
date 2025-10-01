package com.tss.springsecurity.entity;

import jakarta.persistence.*;
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
    
    @Column(name = "employer_name", length = 200)
    private String employerName;
    
    @Column(name = "designation", length = 100)
    private String designation;
    
    @Column(name = "employment_type", length = 50)
    private String employmentType;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "monthly_income", precision = 12, scale = 2)
    private BigDecimal monthlyIncome;
    
    @Column(name = "verified_status", length = 50)
    private String verifiedStatus = "pending";
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
