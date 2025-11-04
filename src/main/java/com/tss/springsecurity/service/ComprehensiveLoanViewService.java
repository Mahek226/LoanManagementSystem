package com.tss.springsecurity.service;

import com.tss.springsecurity.dto.ComprehensiveLoanViewDTO;
import com.tss.springsecurity.entity.*;
import com.tss.springsecurity.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComprehensiveLoanViewService {
    
    private final ApplicantLoanDetailsRepository loanRepository;
    private final ApplicantRepository applicantRepository;
    private final UploadedDocumentRepository documentRepository;
    private final ApplicantDependentRepository dependentRepository;
    private final LoanCollateralRepository collateralRepository;
    private final ApplicantEmploymentRepository employmentRepository;
    private final ApplicantFinancialsRepository financialsRepository;
    private final ApplicantPropertyDetailsRepository propertyRepository;
    private final ApplicantCreditHistoryRepository creditHistoryRepository;
    private final OfficerApplicationAssignmentRepository assignmentRepository;
    private final ApplicantBasicDetailsRepository basicDetailsRepository;
    
    @Transactional(readOnly = true)
    public ComprehensiveLoanViewDTO getComprehensiveLoanView(Long loanId) {
        log.info("Fetching comprehensive loan view for loanId: {}", loanId);
        
        // Fetch main loan details
        ApplicantLoanDetails loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found with ID: " + loanId));
        
        // Fetch applicant
        Applicant applicant = loan.getApplicant();
        
        // Create DTO
        ComprehensiveLoanViewDTO dto = new ComprehensiveLoanViewDTO();
        
        // Map basic loan information
        dto.setLoanId(loan.getLoanId());
        dto.setLoanType(loan.getLoanType());
        dto.setLoanAmount(loan.getLoanAmount() != null ? loan.getLoanAmount().doubleValue() : null);
        dto.setTenureMonths(loan.getTenureMonths());
        dto.setInterestRate(loan.getInterestRate() != null ? loan.getInterestRate().doubleValue() : null);
        dto.setMonthlyEmi(loan.getMonthlyEmi() != null ? loan.getMonthlyEmi().doubleValue() : null);
        dto.setLoanPurpose(loan.getLoanPurpose());
        dto.setStatus(loan.getStatus());
        dto.setAppliedDate(loan.getApplicationDate());
        dto.setApprovedDate(loan.getApprovalDate());
        
        // Map applicant information
        dto.setApplicantId(applicant.getApplicantId());
        dto.setApplicantName(applicant.getFirstName() + " " + applicant.getLastName());
        dto.setEmail(applicant.getEmail());
        dto.setPhone(applicant.getPhone());
        dto.setAddress(applicant.getAddress());
        dto.setCity(applicant.getCity());
        dto.setState(applicant.getState());
        dto.setCountry(applicant.getCountry());
        dto.setZipCode(null); // Would come from basic details if needed
        
        // Map basic details (PAN, Aadhaar, marital status)
        basicDetailsRepository.findByApplicant_ApplicantId(applicant.getApplicantId())
                .ifPresent(basic -> {
                    dto.setPanNumber(basic.getPanNumber());
                    dto.setAadhaarNumber(basic.getAadhaarNumber());
                    dto.setMaritalStatus(basic.getMaritalStatus());
                });
        
        // Map employment information
        employmentRepository.findByApplicant_ApplicantId(applicant.getApplicantId())
                .ifPresent(emp -> {
                    dto.setEmployerName(emp.getEmployerName());
                    dto.setDesignation(emp.getDesignation());
                    dto.setEmploymentType(emp.getEmploymentType());
                    dto.setMonthlyIncome(emp.getMonthlyIncome() != null ? emp.getMonthlyIncome().doubleValue() : null);
                    dto.setEmploymentStartDate(emp.getStartDate());
                    dto.setEducation(null); // Education field not in ApplicantEmployment
                });
        
        // Map financial information
        financialsRepository.findByApplicant_ApplicantId(applicant.getApplicantId())
                .ifPresent(fin -> {
                    dto.setBankName(fin.getBankName());
                    dto.setAccountNumber(fin.getAccountNumber());
                    dto.setIfscCode(fin.getIfscCode());
                    dto.setExistingDebt(fin.getExistingLoanEmi() != null ? fin.getExistingLoanEmi().doubleValue() : null);
                    dto.setTotalAssets(fin.getMonthlyIncome() != null ? fin.getMonthlyIncome().doubleValue() : null);
                    dto.setTotalLiabilities(fin.getExistingLoanEmi() != null && fin.getCreditCardOutstanding() != null ? 
                        fin.getExistingLoanEmi().add(fin.getCreditCardOutstanding()).doubleValue() : null);
                });
        
        // Map credit history
        creditHistoryRepository.findByApplicant_ApplicantId(applicant.getApplicantId())
                .ifPresent(credit -> {
                    dto.setCreditScore(credit.getCreditScore());
                });
        
        // Map property details (for home loans)
        propertyRepository.findByApplicant_ApplicantId(applicant.getApplicantId())
                .ifPresent(prop -> {
                    dto.setPropertyAddress(null); // Use applicant's address instead
                    dto.setPropertyValue(prop.getPropertyValue() != null ? prop.getPropertyValue().doubleValue() : null);
                    dto.setPropertyType(prop.getPropertyType());
                });
        
        // Map residence information from basic details
        basicDetailsRepository.findByApplicant_ApplicantId(applicant.getApplicantId())
                .ifPresent(basic -> {
                    dto.setResidenceType(null); // Field not in basic details
                    dto.setYearsAtCurrentAddress(null); // Field not in basic details
                });
        
        // Calculate financial ratios
        if (dto.getMonthlyIncome() != null && dto.getMonthlyIncome() > 0) {
            if (dto.getExistingDebt() != null) {
                dto.setDtiRatio((dto.getExistingDebt() / dto.getMonthlyIncome()) * 100);
            }
            if (dto.getMonthlyEmi() != null && dto.getInterestRate() != null) {
                double monthlyInterest = dto.getMonthlyEmi() * (dto.getInterestRate() / 12 / 100);
                dto.setInterestCoverageRatio(dto.getMonthlyIncome() / monthlyInterest);
            }
        }
        
        // Map risk assessment
        dto.setRiskLevel(loan.getRiskLevel());
        dto.setRiskScore(loan.getRiskScore());
        dto.setCanApproveReject(loan.getRiskScore() != null && loan.getRiskScore() < 70);
        
        // Map assignment information
        List<OfficerApplicationAssignment> assignments = assignmentRepository.findByLoan_LoanId(loanId);
        if (!assignments.isEmpty()) {
            OfficerApplicationAssignment assignment = assignments.get(0); // Get the first (most recent) assignment
            dto.setAssignmentId(assignment.getAssignmentId());
            if (assignment.getOfficer() != null) {
                dto.setAssignedOfficerName(assignment.getOfficer().getFirstName() + " " + assignment.getOfficer().getLastName());
                dto.setAssignedOfficerType("LOAN_OFFICER");
            }
            dto.setAssignedAt(assignment.getAssignedAt());
            dto.setOfficerRemarks(assignment.getRemarks());
        }
        
        // Map verification status from documents
        List<UploadedDocument> documents = documentRepository.findByApplicant_ApplicantId(applicant.getApplicantId());
        dto.setKycVerified(documents.stream().anyMatch(doc -> "KYC".equalsIgnoreCase(doc.getDocumentType()) && "VERIFIED".equals(doc.getVerificationStatus())));
        dto.setBankVerified(documents.stream().anyMatch(doc -> "BANK_STATEMENT".equalsIgnoreCase(doc.getDocumentType()) && "VERIFIED".equals(doc.getVerificationStatus())));
        dto.setEmploymentVerified(documents.stream().anyMatch(doc -> "EMPLOYMENT_PROOF".equalsIgnoreCase(doc.getDocumentType()) && "VERIFIED".equals(doc.getVerificationStatus())));
        dto.setIncomeVerified(documents.stream().anyMatch(doc -> "SALARY_SLIP".equalsIgnoreCase(doc.getDocumentType()) && "VERIFIED".equals(doc.getVerificationStatus())));
        dto.setAddressVerified(documents.stream().anyMatch(doc -> "ADDRESS_PROOF".equalsIgnoreCase(doc.getDocumentType()) && "VERIFIED".equals(doc.getVerificationStatus())));
        dto.setPropertyVerified(documents.stream().anyMatch(doc -> "PROPERTY_DOCUMENTS".equalsIgnoreCase(doc.getDocumentType()) && "VERIFIED".equals(doc.getVerificationStatus())));
        
        // Map documents
        dto.setDocuments(documents.stream()
                .map(this::mapToDocumentDTO)
                .collect(Collectors.toList()));
        
        // Map dependents - note: there is no reference table in the current schema
        List<ApplicantDependent> dependents = dependentRepository.findByApplicant_ApplicantId(applicant.getApplicantId());
        dto.setDependents(dependents.stream()
                .map(this::mapToDependentDTO)
                .collect(Collectors.toList()));
        
        // Map collaterals
        List<LoanCollateral> collaterals = collateralRepository.findByLoan_LoanId(loanId);
        dto.setCollaterals(collaterals.stream()
                .map(this::mapToCollateralDTO)
                .collect(Collectors.toList()));
        
        log.info("Successfully fetched comprehensive loan view for loanId: {}", loanId);
        return dto;
    }
    
    private ComprehensiveLoanViewDTO.DocumentDTO mapToDocumentDTO(UploadedDocument doc) {
        ComprehensiveLoanViewDTO.DocumentDTO dto = new ComprehensiveLoanViewDTO.DocumentDTO();
        dto.setDocumentId(doc.getDocumentId());
        dto.setDocumentType(doc.getDocumentType());
        dto.setDocumentName(doc.getDocumentName());
        dto.setDocumentUrl(doc.getCloudinaryUrl());
        dto.setVerificationStatus(doc.getVerificationStatus() != null ? doc.getVerificationStatus().name() : null);
        dto.setVerifiedBy(doc.getVerifiedBy());
        dto.setVerifiedAt(doc.getVerifiedAt());
        dto.setRemarks(doc.getVerificationNotes());
        dto.setUploadedAt(doc.getUploadedAt());
        return dto;
    }
    
    private ComprehensiveLoanViewDTO.DependentDTO mapToDependentDTO(ApplicantDependent dep) {
        ComprehensiveLoanViewDTO.DependentDTO dto = new ComprehensiveLoanViewDTO.DependentDTO();
        dto.setDependentId(dep.getId());
        dto.setName(dep.getDependentName());
        dto.setRelationship(dep.getRelationship());
        dto.setAge(dep.getAge());
        dto.setOccupation(dep.getOccupation());
        return dto;
    }
    
    private ComprehensiveLoanViewDTO.CollateralDTO mapToCollateralDTO(LoanCollateral col) {
        ComprehensiveLoanViewDTO.CollateralDTO dto = new ComprehensiveLoanViewDTO.CollateralDTO();
        dto.setCollateralId(col.getId());
        dto.setCollateralType(col.getCollateralType());
        dto.setDescription(col.getCollateralDescription());
        dto.setEstimatedValue(col.getEstimatedValue() != null ? col.getEstimatedValue().doubleValue() : null);
        dto.setOwnershipProof(col.getOwnershipProofUrl());
        dto.setVerificationStatus(col.getIsVerified() != null && col.getIsVerified() ? "VERIFIED" : "PENDING");
        return dto;
    }
}
