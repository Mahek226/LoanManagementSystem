package com.tss.springsecurity.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "co_applicants")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoApplicant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", referencedColumnName = "applicant_id")
    private Applicant applicant;
    
    @Column(name = "first_name", length = 100)
    private String firstName;
    
    @Column(name = "last_name", length = 100)
    private String lastName;
    
    @Column(name = "relationship", length = 50)
    private String relationship; // spouse, parent, sibling, business_partner
    
    @Column(name = "dob")
    private LocalDate dob;
    
    @Column(name = "gender", length = 10)
    private String gender;
    
    @Column(name = "email", length = 150)
    private String email;
    
    @Column(name = "phone", length = 20)
    private String phone;
    
    @Column(name = "pan_number", length = 20)
    private String panNumber;
    
    @Column(name = "aadhaar_number", length = 20)
    private String aadhaarNumber;
    
    @Column(name = "occupation", length = 100)
    private String occupation;
    
    @Column(name = "employer_name", length = 200)
    private String employerName;
    
    @Column(name = "monthly_income", precision = 12, scale = 2)
    private BigDecimal monthlyIncome;
    
    @Column(name = "credit_score")
    private Integer creditScore;
    
    @Column(name = "consent_given")
    private Boolean consentGiven = false;
    
    @Column(name = "consent_date")
    private LocalDateTime consentDate;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
