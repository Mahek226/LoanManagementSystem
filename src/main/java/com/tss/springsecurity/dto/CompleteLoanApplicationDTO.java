package com.tss.springsecurity.dto;

import com.tss.springsecurity.validation.ValidAge;
import com.tss.springsecurity.validation.ValidCreditScore;
import com.tss.springsecurity.validation.ValidLoanAmount;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ValidLoanAmount
@ValidCreditScore
public class CompleteLoanApplicationDTO {
    
    // ========== APPLICANT BASIC INFO ==========
    @Valid
    @NotNull(message = "Applicant details are required")
    private ApplicantDTO applicant;
    
    // ========== BASIC DETAILS ==========
    @Pattern(regexp = "Single|Married|Divorced|Widowed", message = "Invalid marital status")
    private String maritalStatus;
    
    @Size(max = 100)
    private String education;
    
    @Size(max = 50)
    private String nationality;
    
    @NotBlank(message = "PAN number is required")
    @Pattern(regexp = "[A-Z]{5}[0-9]{4}[A-Z]{1}", message = "Invalid PAN format")
    private String panNumber;
    
    @NotBlank(message = "Aadhaar number is required")
    @Pattern(regexp = "^[0-9]{12}$", message = "Aadhaar must be 12 digits")
    private String aadhaarNumber;
    
    private String voterId;
    private String passportNumber;
    
    // ========== EMPLOYMENT DETAILS ==========
    @NotBlank(message = "Employer name is required")
    private String employerName;
    
    private String designation;
    
    @NotBlank
    @Pattern(regexp = "salaried|self-employed")
    private String employmentType;
    
    @Past
    private LocalDate employmentStartDate;
    
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal monthlyIncome;
    
    // ========== FINANCIAL DETAILS ==========
    @NotBlank
    private String bankName;
    
    @NotBlank
    private String accountNumber;
    
    @Pattern(regexp = "savings|current")
    private String accountType;
    
    @NotBlank
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$")
    private String ifscCode;
    
    private BigDecimal totalCreditLastMonth;
    private BigDecimal totalDebitLastMonth;
    private String anomalies;
    
    // ========== PROPERTY DETAILS ==========
    @NotBlank
    @Pattern(regexp = "owned|rented|parental|company_provided")
    private String residenceType;
    
    private String propertyOwnership;
    private BigDecimal monthlyRent;
    
    @Min(0)
    private Integer yearsAtCurrentAddress;
    
    private BigDecimal propertyValue;
    
    @Pattern(regexp = "apartment|independent_house|villa|farmhouse")
    private String propertyType;
    
    private Integer totalAreaSqft;
    private Boolean hasHomeLoan = false;
    private BigDecimal outstandingHomeLoan;
    private BigDecimal homeLoanEmi;
    
    // ========== CREDIT HISTORY ==========
    @Min(300)
    @Max(900)
    private Integer creditScore;
    
    @Pattern(regexp = "CIBIL|Experian|Equifax|CRIF")
    private String creditBureau;
    
    private Integer totalActiveLoans = 0;
    private BigDecimal totalOutstandingDebt;
    private BigDecimal totalMonthlyEmi;
    private Integer creditCardCount = 0;
    private BigDecimal totalCreditLimit;
    private BigDecimal creditUtilizationRatio;
    
    @Pattern(regexp = "excellent|good|fair|poor")
    private String paymentHistory;
    
    private Integer defaultsCount = 0;
    private Boolean bankruptcyFiled = false;
    private String creditReportUrl;
    
    // ========== LOAN DETAILS ==========
    @NotBlank
    @Pattern(regexp = "(?i)(home|gold|personal|vehicle|education|business|HOME|GOLD|PERSONAL|VEHICLE|EDUCATION|BUSINESS)", 
             message = "Invalid loan type. Must be one of: HOME, GOLD, PERSONAL, VEHICLE, EDUCATION, BUSINESS")
    private String loanType;
    
    @NotNull
    @DecimalMin(value = "10000.0")
    private BigDecimal loanAmount;
    
    @NotNull
    @Min(6)
    @Max(360)
    private Integer tenureMonths;
    
    // ========== DOCUMENTS ==========
    private List<DocumentDTO> documents;
    
    // ========== REFERENCES ==========
    private List<ReferenceDTO> references;
    
    // ========== DEPENDENTS ==========
    private List<DependentDTO> dependents;
    
    // ========== CO-APPLICANTS ==========
    private List<CoApplicantDTO> coApplicants;
    
    // ========== COLLATERAL ==========
    private List<CollateralDTO> collaterals;
    
    // ========== NESTED DTOs ==========
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentDTO {
        @NotBlank
        @Pattern(regexp = "aadhaar|pan|passport|voter_id|payslip|bank_statement|itr|other")
        private String docType;
        
        private String docNumber;
        private String cloudinaryUrl;
        private String ocrText;
        private Boolean isTampered = false;
        
        // For Aadhaar
        private String name;
        private LocalDate dob;
        private String gender;
        private String address;
        private String qrCodeData;
        
        // For PAN
        private String fatherName;
        
        // For Passport
        private String nationality;
        private LocalDate expiryDate;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReferenceDTO {
        @NotBlank
        private String referenceName;
        
        @NotBlank
        @Pattern(regexp = "family|friend|colleague|employer")
        private String relationship;
        
        @NotBlank
        @Pattern(regexp = "^[0-9]{10}$")
        private String phone;
        
        @Email
        private String email;
        
        private String address;
        private String occupation;
        
        @Min(0)
        private Integer yearsKnown;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DependentDTO {
        @NotBlank
        private String dependentName;
        
        @NotBlank
        @Pattern(regexp = "spouse|child|parent|sibling")
        private String relationship;
        
        private LocalDate dob;
        private Integer age;
        private Boolean isFinanciallyDependent = true;
        private String educationStatus;
        private String occupation;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoApplicantDTO {
        @NotBlank
        private String firstName;
        
        private String lastName;
        
        @NotBlank
        @Pattern(regexp = "spouse|parent|sibling|business_partner")
        private String relationship;
        
        @Past
        private LocalDate dob;
        
        private String gender;
        
        @Email
        private String email;
        
        @Pattern(regexp = "^[0-9]{10}$")
        private String phone;
        
        @Pattern(regexp = "[A-Z]{5}[0-9]{4}[A-Z]{1}")
        private String panNumber;
        
        @Pattern(regexp = "^[0-9]{12}$")
        private String aadhaarNumber;
        
        private String occupation;
        private String employerName;
        
        @DecimalMin(value = "0.0", inclusive = false)
        private BigDecimal monthlyIncome;
        
        @Min(300)
        @Max(900)
        private Integer creditScore;
        
        private Boolean consentGiven = false;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollateralDTO {
        @NotBlank
        @Pattern(regexp = "property|gold|vehicle|fixed_deposit|securities")
        private String collateralType;
        
        @NotBlank
        private String collateralDescription;
        
        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        private BigDecimal estimatedValue;
        
        private String valuationBy;
        private String ownershipProofUrl;
        private String valuationReportUrl;
    }
}
