package com.tss.springsecurity.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "applicant_loan_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicantLoanDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loan_id")
    private Long loanId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", referencedColumnName = "applicant_id")
    private Applicant applicant;
    
    @NotBlank(message = "Loan type is required")
    @Pattern(regexp = "(?i)(home|gold|personal|vehicle|education|business|agriculture|medical|HOME|GOLD|PERSONAL|VEHICLE|EDUCATION|BUSINESS|AGRICULTURE|MEDICAL)", message = "Loan type must be home, gold, personal, vehicle, education, business, agriculture, or medical")
    @Column(name = "loan_type", length = 50)
    private String loanType;
    
    @NotNull(message = "Loan amount is required")
    @DecimalMin(value = "10000.0", message = "Loan amount must be at least ₹10,000")
    @DecimalMax(value = "100000000.0", message = "Loan amount cannot exceed ₹10 crores")
    @Digits(integer = 13, fraction = 2, message = "Loan amount must have at most 13 digits before decimal and 2 after")
    @Column(name = "loan_amount", precision = 15, scale = 2)
    private BigDecimal loanAmount;
    
    @DecimalMin(value = "1.0", message = "Interest rate must be at least 1%")
    @DecimalMax(value = "50.0", message = "Interest rate cannot exceed 50%")
    @Digits(integer = 3, fraction = 2, message = "Interest rate must have at most 3 digits before decimal and 2 after")
    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate;
    
    @NotNull(message = "Tenure is required")
    @Min(value = 6, message = "Tenure must be at least 6 months")
    @Max(value = 360, message = "Tenure cannot exceed 360 months (30 years)")
    @Column(name = "tenure_months")
    private Integer tenureMonths;
    
    @Pattern(regexp = "(?i)(pending|approved|rejected|under_review|disbursed|closed|PENDING|APPROVED|REJECTED|UNDER_REVIEW|DISBURSED|CLOSED)", message = "Status must be pending, approved, rejected, under_review, disbursed, or closed")
    @Column(name = "status", length = 50)
    private String status = "pending";
    
    @Column(name = "loan_purpose", length = 500)
    private String loanPurpose;
    
    // Employment Details
    @Column(name = "employment_type", length = 50)
    private String employmentType;
    
    @Column(name = "employer_name", length = 200)
    private String employerName;
    
    @Column(name = "designation", length = 100)
    private String designation;
    
    @Column(name = "monthly_income", precision = 15, scale = 2)
    private BigDecimal monthlyIncome;
    
    // Bank Details
    @Column(name = "bank_name", length = 200)
    private String bankName;
    
    @Column(name = "account_number", length = 50)
    private String accountNumber;
    
    @Column(name = "ifsc_code", length = 20)
    private String ifscCode;
    
    @Column(name = "account_type", length = 50)
    private String accountType;
    
    // Financial Obligations
    @Column(name = "existing_obligations", precision = 15, scale = 2)
    private BigDecimal existingObligations;
    
    // Co-Applicant Details
    @Column(name = "has_co_applicant")
    private Boolean hasCoApplicant = false;
    
    @Column(name = "co_applicant_name", length = 200)
    private String coApplicantName;
    
    @Column(name = "co_applicant_relation", length = 50)
    private String coApplicantRelation;
    
    // Collateral Details
    @Column(name = "has_collateral")
    private Boolean hasCollateral = false;
    
    @Column(name = "collateral_type", length = 100)
    private String collateralType;
    
    @Column(name = "collateral_value", precision = 15, scale = 2)
    private BigDecimal collateralValue;
    
    // Application Status
    @Column(name = "application_status", length = 50)
    private String applicationStatus = "DRAFT";
    
    @Column(name = "loan_status", length = 50)
    private String loanStatus = "PENDING";
    
    @Min(value = 0, message = "Risk score cannot be negative")
    @Max(value = 1000, message = "Risk score cannot exceed 1000")
    @Column(name = "risk_score")
    private Integer riskScore = 0;
    
    @Column(name = "submitted_at", updatable = false)
    private LocalDateTime submittedAt;
    
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FraudFlag> fraudFlags;
    
    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<VerificationLog> verificationLogs;
    
    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AuditScore> auditScores;
    
    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LoanCollateral> collaterals;
    
    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
    }
}
