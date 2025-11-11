package com.tss.springsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComprehensiveLoanViewDTO {
    
    // Basic Loan Information
    private Long loanId;
    private String loanType;
    private Double loanAmount;
    private Integer tenureMonths;
    private Double interestRate;
    private Double monthlyEmi;
    private String loanPurpose;
    private String status;
    private LocalDateTime appliedDate;
    private LocalDateTime approvedDate;
    
    // Applicant Information
    private Long applicantId;
    private String applicantName;
    private String email;
    private String phone;
    private String panNumber;
    private String aadhaarNumber;
    private String maritalStatus;
    private String address;
    private String city;
    private String state;
    private String country;
    private String zipCode;
    
    // Employment Information
    private String employerName;
    private String designation;
    private String employmentType;
    private Double monthlyIncome;
    private LocalDate employmentStartDate;
    private String education;
    
    // Financial Information
    private String bankName;
    private String accountNumber;
    private String ifscCode;
    private Integer creditScore;
    private Double existingDebt;
    private Double totalAssets;
    private Double totalLiabilities;
    private Double dtiRatio;
    private Double interestCoverageRatio;
    
    // Property Information (for home loans)
    private String propertyAddress;
    private Double propertyValue;
    private String propertyType;
    
    // Residence Information
    private String residenceType;
    private Integer yearsAtCurrentAddress;
    
    // Risk Assessment
    private String riskLevel;
    private Integer riskScore;
    private Boolean canApproveReject;
    
    // Assignment Information
    private Long assignmentId;
    private String assignedOfficerName;
    private String assignedOfficerType;
    private LocalDateTime assignedAt;
    private String officerRemarks;
    
    // Verification Status
    private Boolean kycVerified;
    private Boolean bankVerified;
    private Boolean employmentVerified;
    private Boolean incomeVerified;
    private Boolean addressVerified;
    private Boolean propertyVerified;
    
    // Related Data
    private List<DocumentDTO> documents;
    private List<ReferenceDTO> references;
    private List<DependentDTO> dependents;
    private List<CollateralDTO> collaterals;
    
    // DTOs for related entities
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentDTO {
        private Long documentId;
        private String documentType;
        private String documentName;
        private String documentUrl;
        private String verificationStatus;
        private String verifiedBy;
        private LocalDateTime verifiedAt;
        private String remarks;
        private LocalDateTime uploadedAt;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReferenceDTO {
        private Long referenceId;
        private String name;
        private String relationship;
        private String phoneNumber;
        private String email;
        private String address;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DependentDTO {
        private Long dependentId;
        private String name;
        private String relationship;
        private Integer age;
        private String occupation;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollateralDTO {
        private Long collateralId;
        private String collateralType;
        private String description;
        private Double estimatedValue;
        private String ownershipProof;
        private String verificationStatus;
    }
}
