package com.tss.springsecurity.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
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
    
    @NotBlank(message = "Dependent name is required")
    @Size(min = 2, max = 150, message = "Dependent name must be between 2 and 150 characters")
    @Pattern(regexp = "^[a-zA-Z\\s.]+$", message = "Dependent name can only contain letters, spaces, and dots")
    @Column(name = "dependent_name", length = 150)
    private String dependentName;
    
    @NotBlank(message = "Relationship is required")
    @Pattern(regexp = "spouse|child|parent|sibling|other", message = "Relationship must be spouse, child, parent, sibling, or other")
    @Column(name = "relationship", length = 50)
    private String relationship; // spouse, child, parent, sibling
    
    @Past(message = "Date of birth must be in the past")
    @Column(name = "dob")
    private LocalDate dob;
    
    @Min(value = 0, message = "Age cannot be negative")
    @Max(value = 120, message = "Age cannot exceed 120 years")
    @Column(name = "age")
    private Integer age;
    
    @Column(name = "is_financially_dependent")
    private Boolean isFinanciallyDependent = true;
    
    @Size(max = 100, message = "Education status must not exceed 100 characters")
    @Column(name = "education_status", length = 100)
    private String educationStatus;
    
    @Size(max = 100, message = "Occupation must not exceed 100 characters")
    @Column(name = "occupation", length = 100)
    private String occupation;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
