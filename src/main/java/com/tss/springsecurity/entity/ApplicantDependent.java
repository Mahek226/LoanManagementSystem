package com.tss.springsecurity.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "applicant_dependents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicantDependent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", referencedColumnName = "applicant_id")
    private Applicant applicant;
    
    @Column(name = "dependent_name", length = 150)
    private String dependentName;
    
    @Column(name = "relationship", length = 50)
    private String relationship; // spouse, child, parent, sibling
    
    @Column(name = "dob")
    private LocalDate dob;
    
    @Column(name = "age")
    private Integer age;
    
    @Column(name = "is_financially_dependent")
    private Boolean isFinanciallyDependent = true;
    
    @Column(name = "education_status", length = 100)
    private String educationStatus;
    
    @Column(name = "occupation", length = 100)
    private String occupation;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
