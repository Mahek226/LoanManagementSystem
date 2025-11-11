package com.tss.springsecurity.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CompleteLoanApplicationRequest {
    
    // Loan Type Information
    @NotBlank(message = "Loan type is required")
    private String loanType;
    
    // Personal Details
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;
    
    private String middleName;
    
    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;
    
    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;
    
    @NotBlank(message = "Gender is required")
    private String gender;
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be 10 digits")
    private String phone;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;
    
    // Address Details
    @NotBlank(message = "Current address is required")
    private String currentAddress;
    
    @NotBlank(message = "Current city is required")
    private String currentCity;
    
    @NotBlank(message = "Current state is required")
    private String currentState;
    
    @NotBlank(message = "Current pincode is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be 6 digits")
    private String currentPincode;
    
    private Boolean sameAsCurrent = false;
    private String permanentAddress;
    private String permanentCity;
    private String permanentState;
    private String permanentPincode;
    
    // Loan Details
    @NotNull(message = "Loan amount is required")
    @DecimalMin(value = "100000", message = "Minimum loan amount is ₹1,00,000")
    @DecimalMax(value = "100000000", message = "Maximum loan amount is ₹10,00,00,000")
    private BigDecimal loanAmount;
    
    @NotNull(message = "Loan tenure is required")
    @Min(value = 12, message = "Minimum tenure is 12 months")
    @Max(value = 360, message = "Maximum tenure is 360 months")
    private Integer loanTenure;
    
    @NotBlank(message = "Loan purpose is required")
    private String loanPurpose;
    
    // Property Details (for home loans)
    private String propertyType;
    private String propertyAddress;
    private String propertyCity;
    private String propertyState;
    private String propertyPincode;
    private BigDecimal propertyValue;
    private String constructionStatus;
    
    // Employment Details
    @NotBlank(message = "Employment type is required")
    private String employmentType;
    
    private String companyName;
    private String designation;
    private Integer workExperience;
    private String officeAddress;
    private String officeCity;
    private String officeState;
    private String officePincode;
    
    // Financial Details
    @NotNull(message = "Monthly income is required")
    @DecimalMin(value = "10000", message = "Minimum monthly income is ₹10,000")
    private BigDecimal monthlyIncome;
    
    private BigDecimal otherIncome;
    private BigDecimal monthlyExpenses;
    private BigDecimal existingLoanEmi;
    private BigDecimal creditCardOutstanding;
    
    // Bank Details
    @NotBlank(message = "Bank name is required")
    private String bankName;
    
    @NotBlank(message = "Account number is required")
    private String accountNumber;
    
    @NotBlank(message = "IFSC code is required")
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Please provide a valid IFSC code")
    private String ifscCode;
    
    @NotBlank(message = "Account type is required")
    private String accountType;
    
    // Co-applicant Details (optional)
    private Boolean hasCoApplicant = false;
    private String coApplicantFirstName;
    private String coApplicantLastName;
    private LocalDate coApplicantDateOfBirth;
    private String coApplicantPhone;
    private String coApplicantEmail;
    private String coApplicantRelation;
    private BigDecimal coApplicantIncome;
    
    // Document Information (file names will be stored after upload)
    private String aadhaarNumber;
    private String panNumber;
    private String passportNumber;
    
    // Additional Information
    private String remarks;
    private Boolean agreeToTerms = false;
    
    // Document Files (for upload)
    private List<MultipartFile> documents;
    private List<String> documentTypes; // Corresponding document types
}
