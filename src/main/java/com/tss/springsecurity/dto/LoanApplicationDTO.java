package com.tss.springsecurity.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationDTO {
    
    // Applicant Basic Info
    @Valid
    @NotNull(message = "Applicant details are required")
    private ApplicantDTO applicant;
    
    // Basic Details
    @Pattern(regexp = "Single|Married|Divorced|Widowed", message = "Invalid marital status")
    private String maritalStatus;
    
    @Size(max = 100, message = "Education must not exceed 100 characters")
    private String education;
    
    @Size(max = 50, message = "Nationality must not exceed 50 characters")
    private String nationality;
    
    @NotBlank(message = "PAN number is required")
    @Pattern(regexp = "[A-Z]{5}[0-9]{4}[A-Z]{1}", message = "Invalid PAN format")
    private String panNumber;
    
    @NotBlank(message = "Aadhaar number is required")
    @Pattern(regexp = "^[0-9]{12}$", message = "Aadhaar must be 12 digits")
    private String aadhaarNumber;
    
    // Employment Details
    @NotBlank(message = "Employer name is required")
    @Size(max = 200, message = "Employer name must not exceed 200 characters")
    private String employerName;
    
    @Size(max = 100, message = "Designation must not exceed 100 characters")
    private String designation;
    
    @NotBlank(message = "Employment type is required")
    @Pattern(regexp = "salaried|self-employed", message = "Employment type must be salaried or self-employed")
    private String employmentType;
    
    @Past(message = "Start date must be in the past")
    private LocalDate employmentStartDate;
    
    @NotNull(message = "Monthly income is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Monthly income must be greater than 0")
    private BigDecimal monthlyIncome;
    
    // Financial Details
    @NotBlank(message = "Bank name is required")
    @Size(max = 150, message = "Bank name must not exceed 150 characters")
    private String bankName;
    
    @NotBlank(message = "Account number is required")
    @Size(max = 50, message = "Account number must not exceed 50 characters")
    private String accountNumber;
    
    @Pattern(regexp = "savings|current", message = "Account type must be savings or current")
    private String accountType;
    
    @NotBlank(message = "IFSC code is required")
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC code format")
    private String ifscCode;
    
    // Property Details
    @NotBlank(message = "Residence type is required")
    @Pattern(regexp = "owned|rented|parental|company_provided", message = "Invalid residence type")
    private String residenceType;
    
    private BigDecimal monthlyRent;
    
    @Min(value = 0, message = "Years at current address cannot be negative")
    private Integer yearsAtCurrentAddress;
    
    private BigDecimal propertyValue;
    
    @Pattern(regexp = "apartment|independent_house|villa|farmhouse", message = "Invalid property type")
    private String propertyType;
    
    // Credit History
    @Min(value = 300, message = "Credit score must be at least 300")
    @Max(value = 900, message = "Credit score cannot exceed 900")
    private Integer creditScore;
    
    @Pattern(regexp = "CIBIL|Experian|Equifax|CRIF", message = "Invalid credit bureau")
    private String creditBureau;
    
    // Loan Details
    @NotBlank(message = "Loan type is required")
    @Pattern(regexp = "home|gold|personal|vehicle|education|business", message = "Invalid loan type")
    private String loanType;
    
    @NotNull(message = "Loan amount is required")
    @DecimalMin(value = "10000.0", message = "Loan amount must be at least 10,000")
    private BigDecimal loanAmount;
    
    @NotNull(message = "Tenure is required")
    @Min(value = 6, message = "Tenure must be at least 6 months")
    @Max(value = 360, message = "Tenure cannot exceed 360 months")
    private Integer tenureMonths;
}
