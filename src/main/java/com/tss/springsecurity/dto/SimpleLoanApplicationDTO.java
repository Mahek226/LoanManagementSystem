package com.tss.springsecurity.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimpleLoanApplicationDTO {
    
    @NotNull(message = "Applicant ID is required")
    private Long applicantId;
    
    // Basic Details
    @NotNull
    private BasicDetails basicDetails;
    
    // Applicant Details
    private ApplicantDetails applicantDetails;
    
    // Financial Details
    @NotNull
    private FinancialDetails financialDetails;
    
    // Documents
    private List<DocumentUpload> documents;
    
    // Declarations
    @NotNull
    private Declarations declarations;
    
    private String status;
    private String submittedAt;
    
    // Nested Classes
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BasicDetails {
        @NotBlank(message = "Loan type is required")
        @Pattern(regexp = "PERSONAL|HOME|VEHICLE|EDUCATION|BUSINESS", message = "Invalid loan type")
        private String loanType;
        
        @NotNull(message = "Loan amount is required")
        @DecimalMin(value = "1000.0", message = "Minimum loan amount is 1000")
        private BigDecimal loanAmount;
        
        @NotNull(message = "Tenure is required")
        @Min(value = 6, message = "Minimum tenure is 6 months")
        @Max(value = 360, message = "Maximum tenure is 360 months")
        private Integer tenure;
        
        @NotBlank(message = "Purpose is required")
        @Size(min = 10, message = "Purpose must be at least 10 characters")
        private String purpose;
        
        private Boolean hasCoApplicant;
        private String coApplicantName;
        private String coApplicantRelation;
        
        private Boolean hasCollateral;
        private String collateralType;
        private BigDecimal collateralValue;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicantDetails {
        private String firstName;
        private String middleName;
        private String lastName;
        private String dateOfBirth;
        private String gender;
        private String maritalStatus;
        private String emailAddress;
        private String mobileNumber;
        private String alternateNumber;
        private String currentAddress;
        private String currentCity;
        private String currentState;
        private String currentPincode;
        private String residenceType;
        private Integer yearsAtCurrentAddress;
        private Boolean permanentAddressSame;
        private String permanentAddress;
        private String permanentCity;
        private String permanentState;
        private String permanentPincode;
        private String panNumber;
        private String aadharNumber;
        private Boolean hasCoApplicant;
        private String coApplicantName;
        private String coApplicantRelation;
        private String coApplicantPan;
        private String coApplicantAadhar;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancialDetails {
        @NotBlank(message = "Employment type is required")
        @Pattern(regexp = "SALARIED|SELF_EMPLOYED|BUSINESS|PROFESSIONAL|RETIRED")
        private String employmentType;
        
        private String employerName;
        private String designation;
        private Integer workExperience;
        
        private BigDecimal monthlyGrossSalary;
        private BigDecimal monthlyNetSalary;
        
        private String businessName;
        private String businessType;
        private BigDecimal annualTurnover;
        
        private String otherIncomeSources;
        private BigDecimal otherIncomeAmount;
        
        private BigDecimal existingLoanEMI;
        private BigDecimal creditCardPayment;
        private BigDecimal otherObligations;
        
        @NotBlank(message = "Bank name is required")
        private String bankName;
        
        @NotBlank(message = "Account number is required")
        private String accountNumber;
        
        @NotBlank(message = "IFSC code is required")
        @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC code format")
        private String ifscCode;
        
        @NotBlank(message = "Account type is required")
        @Pattern(regexp = "SAVINGS|CURRENT")
        private String accountType;
        
        @NotNull(message = "Bank statement consent is required")
        private Boolean bankStatementConsent;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentUpload {
        private String documentType;
        private String fileName;
        private String fileUrl;
        private Long fileSize;
        private String uploadedAt;
        private String status;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Declarations {
        @NotNull(message = "KYC consent is required")
        private Boolean kycConsent;
        
        @NotNull(message = "Credit bureau consent is required")
        private Boolean creditBureauConsent;
        
        @NotNull(message = "Bank statement consent is required")
        private Boolean bankStatementConsent;
        
        @NotNull(message = "Terms acceptance is required")
        private Boolean termsAccepted;
        
        @NotNull(message = "Privacy policy acceptance is required")
        private Boolean privacyPolicyAccepted;
        
        @NotNull(message = "E-sign consent is required")
        private Boolean eSignConsent;
        
        private String declarationDate;
        private String ipAddress;
        private String eSignatureUrl;
    }
}
