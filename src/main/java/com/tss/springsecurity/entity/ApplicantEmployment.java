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
    
    @NotBlank(message = "Employer name is required")
    @Size(min = 2, max = 200, message = "Employer name must be between 2 and 200 characters")
    @Column(name = "employer_name", length = 200)
    private String employerName;
    
    @Size(max = 100, message = "Designation must not exceed 100 characters")
    @Column(name = "designation", length = 100)
    private String designation;
    
    @NotBlank(message = "Employment type is required")
    @Pattern(regexp = "salaried|self-employed|business|unemployed|retired", message = "Employment type must be salaried, self-employed, business, unemployed, or retired")
    @Column(name = "employment_type", length = 50)
    private String employmentType;
    
    @Past(message = "Employment start date must be in the past")
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @NotNull(message = "Monthly income is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Monthly income must be greater than 0")
    @DecimalMax(value = "10000000.0", message = "Monthly income cannot exceed 1 crore")
    @Digits(integer = 10, fraction = 2, message = "Monthly income must have at most 10 digits before decimal and 2 after")
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
