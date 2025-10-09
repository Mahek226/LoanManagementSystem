package com.tss.springsecurity.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_collateral", indexes = {
    @Index(name = "idx_collateral_applicant_id", columnList = "applicant_id"),
    @Index(name = "idx_collateral_type", columnList = "collateral_type"),
    @Index(name = "idx_collateral_valuation_report", columnList = "valuation_report_url"),
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
    
    @Column(name = "collateral_type", length = 50)
    private String collateralType; // property, gold, vehicle, fixed_deposit, securities
    
    @Column(name = "collateral_description", columnDefinition = "TEXT")
    private String collateralDescription;
    
    @Column(name = "estimated_value", precision = 15, scale = 2)
    private BigDecimal estimatedValue;
    
    @Column(name = "valuation_date")
    private LocalDateTime valuationDate;
    
    @Column(name = "valuation_by", length = 150)
    private String valuationBy;
    
    @Column(name = "ownership_proof_url", columnDefinition = "TEXT")
    private String ownershipProofUrl;
    
    @Column(name = "valuation_report_url", columnDefinition = "TEXT")
    private String valuationReportUrl;
    
    @Column(name = "is_verified")
    private Boolean isVerified = false;
    
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
