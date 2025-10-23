package com.tss.springsecurity.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_collateral", indexes = {
    @Index(name = "idx_collateral_applicant_id", columnList = "applicant_id"),
    @Index(name = "idx_collateral_type", columnList = "collateral_type"),
    @Index(name = "idx_collateral_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanCollateral {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", referencedColumnName = "loan_id")
    private ApplicantLoanDetails loan;
    
    @NotBlank(message = "Collateral type is required")
    @Pattern(regexp = "property|gold|vehicle|fixed_deposit|securities|jewelry|machinery|other", message = "Collateral type must be property, gold, vehicle, fixed_deposit, securities, jewelry, machinery, or other")
    @Column(name = "collateral_type", length = 50)
    private String collateralType; // property, gold, vehicle, fixed_deposit, securities
    
    @NotBlank(message = "Collateral description is required")
    @Size(min = 10, max = 1000, message = "Collateral description must be between 10 and 1000 characters")
    @Column(name = "collateral_description", columnDefinition = "TEXT")
    private String collateralDescription;
    
    @NotNull(message = "Estimated value is required")
    @DecimalMin(value = "1000.0", message = "Estimated value must be at least ₹1,000")
    @DecimalMax(value = "1000000000.0", message = "Estimated value cannot exceed ₹100 crores")
    @Digits(integer = 13, fraction = 2, message = "Estimated value must have at most 13 digits before decimal and 2 after")
    @Column(name = "estimated_value", precision = 15, scale = 2)
    private BigDecimal estimatedValue;
    
    @Column(name = "valuation_date")
    private LocalDateTime valuationDate;
    
    @Size(min = 2, max = 150, message = "Valuation by must be between 2 and 150 characters")
    @Column(name = "valuation_by", length = 150)
    private String valuationBy;
    
    @Size(max = 500, message = "Ownership proof URL must not exceed 500 characters")
    @Column(name = "ownership_proof_url", columnDefinition = "TEXT")
    private String ownershipProofUrl;
    
    @Size(max = 500, message = "Valuation report URL must not exceed 500 characters")
    @Column(name = "valuation_report_url", columnDefinition = "TEXT")
    private String valuationReportUrl;
    
    @Column(name = "is_verified")
    private Boolean isVerified = false;
    
    @Size(max = 1000, message = "Verification notes must not exceed 1000 characters")
    @Column(name = "verification_notes", columnDefinition = "TEXT")
    private String verificationNotes;
    
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
