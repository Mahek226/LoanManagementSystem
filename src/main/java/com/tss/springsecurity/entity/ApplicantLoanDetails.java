package com.tss.springsecurity.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    @JsonIgnoreProperties({"loanDetails", "employment", "financials", "aadhaarDetails", "panDetails", "passportDetails", "otherDocuments", "basicDetails", "propertyDetails", "references", "bankStatements", "payslips", "loanCollaterals", "loanGuarantors", "loanRepayments", "loanDisbursements"})
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
    
    // Applicant Personal Details
    @Column(name = "applicant_first_name", length = 100)
    private String applicantFirstName;
    
    @Column(name = "applicant_middle_name", length = 100)
    private String applicantMiddleName;
    
    @Column(name = "applicant_last_name", length = 100)
    private String applicantLastName;
    
    @Column(name = "applicant_dob", length = 20)
    private String applicantDateOfBirth;
    
    @Column(name = "applicant_gender", length = 20)
    private String applicantGender;
    
    @Column(name = "applicant_marital_status", length = 50)
    private String applicantMaritalStatus;
    
    @Column(name = "applicant_email", length = 200)
    private String applicantEmail;
    
    @Column(name = "applicant_mobile", length = 20)
    private String applicantMobile;
    
    @Column(name = "applicant_alternate_mobile", length = 20)
    private String applicantAlternateMobile;
    
    // Address Details
    @Column(name = "current_address", length = 500)
    private String currentAddress;
    
    @Column(name = "current_city", length = 100)
    private String currentCity;
    
    @Column(name = "current_state", length = 100)
    private String currentState;
    
    @Column(name = "current_pincode", length = 10)
    private String currentPincode;
    
    @Column(name = "residence_type", length = 50)
    private String residenceType;
    
    @Column(name = "years_at_current_address")
    private Integer yearsAtCurrentAddress;
    
    @Column(name = "permanent_address_same")
    private Boolean permanentAddressSame = false;
    
    @Column(name = "permanent_address", length = 500)
    private String permanentAddress;
    
    @Column(name = "permanent_city", length = 100)
    private String permanentCity;
    
    @Column(name = "permanent_state", length = 100)
    private String permanentState;
    
    @Column(name = "permanent_pincode", length = 10)
    private String permanentPincode;
    
    // Identity Details
    @Column(name = "applicant_pan", length = 20)
    private String applicantPan;
    
    @Column(name = "applicant_aadhar", length = 20)
    private String applicantAadhar;
    
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
    
    @Column(name = "co_applicant_pan", length = 20)
    private String coApplicantPan;
    
    @Column(name = "co_applicant_aadhar", length = 20)
    private String coApplicantAadhar;
    
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
    
    @Column(name = "risk_level", length = 20)
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL
    
    @Column(name = "submitted_at", updatable = false)
    private LocalDateTime submittedAt;
    
    @Column(name = "application_date")
    private LocalDateTime applicationDate;
    
    @Column(name = "approval_date")
    private LocalDateTime approvalDate;
    
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    @Column(name = "monthly_emi", precision = 15, scale = 2)
    private BigDecimal monthlyEmi;
    
    // Approval/Rejection tracking fields
    @Column(name = "approved_by", length = 200)
    private String approvedBy;
    
    @Column(name = "rejected_by", length = 200)
    private String rejectedBy;
    
    @Column(name = "rejection_date")
    private LocalDateTime rejectionDate;
    
    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;
    
    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"loan", "applicant"})
    private List<FraudFlag> fraudFlags;
    
    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"loan", "applicant"})
    private List<VerificationLog> verificationLogs;
    
    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"loan", "applicant"})
    private List<AuditScore> auditScores;
    
    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"loan", "applicant"})
    private List<LoanCollateral> collaterals;
    
    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
    }
}
