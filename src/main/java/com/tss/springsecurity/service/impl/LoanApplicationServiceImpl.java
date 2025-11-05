package com.tss.springsecurity.service.impl;

import com.tss.springsecurity.dto.ApplicantLoanDetailsDTO;
import com.tss.springsecurity.dto.CompleteLoanApplicationDTO;
import com.tss.springsecurity.dto.LoanApplicationDTO;
import com.tss.springsecurity.dto.LoanApplicationForExistingApplicantDTO;
import com.tss.springsecurity.entity.*;
import com.tss.springsecurity.repository.*;
import com.tss.springsecurity.service.LoanApplicationService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LoanApplicationServiceImpl implements LoanApplicationService {
    
    private final ApplicantRepository applicantRepository;
    private final ApplicantBasicDetailsRepository basicDetailsRepository;
    private final ApplicantEmploymentRepository employmentRepository;
    private final ApplicantFinancialsRepository financialsRepository;
    private final ApplicantPropertyDetailsRepository propertyDetailsRepository;
    private final ApplicantCreditHistoryRepository creditHistoryRepository;
    private final ApplicantLoanDetailsRepository loanDetailsRepository;
    private final CompleteLoanApplicationServiceImpl completeLoanApplicationService;
    private final com.tss.springsecurity.service.DocumentExtractionService documentExtractionService;
    private final UploadedDocumentRepository uploadedDocumentRepository;
    
    public LoanApplicationServiceImpl(
            ApplicantRepository applicantRepository,
            ApplicantBasicDetailsRepository basicDetailsRepository,
            ApplicantEmploymentRepository employmentRepository,
            ApplicantFinancialsRepository financialsRepository,
            ApplicantPropertyDetailsRepository propertyDetailsRepository,
            ApplicantCreditHistoryRepository creditHistoryRepository,
            ApplicantLoanDetailsRepository loanDetailsRepository,
            CompleteLoanApplicationServiceImpl completeLoanApplicationService,
            com.tss.springsecurity.service.DocumentExtractionService documentExtractionService,
            UploadedDocumentRepository uploadedDocumentRepository) {
        this.applicantRepository = applicantRepository;
        this.basicDetailsRepository = basicDetailsRepository;
        this.employmentRepository = employmentRepository;
        this.financialsRepository = financialsRepository;
        this.propertyDetailsRepository = propertyDetailsRepository;
        this.creditHistoryRepository = creditHistoryRepository;
        this.loanDetailsRepository = loanDetailsRepository;
        this.completeLoanApplicationService = completeLoanApplicationService;
        this.documentExtractionService = documentExtractionService;
        this.uploadedDocumentRepository = uploadedDocumentRepository;
    }
    
    @Override
    @Transactional
    public Applicant submitLoanApplication(LoanApplicationDTO dto) {
        // Check if applicant already exists
        if (applicantRepository.existsByEmail(dto.getApplicant().getEmail())) {
            throw new RuntimeException("Applicant with email " + dto.getApplicant().getEmail() + " already exists");
        }
        if (applicantRepository.existsByPhone(dto.getApplicant().getPhone())) {
            throw new RuntimeException("Applicant with phone " + dto.getApplicant().getPhone() + " already exists");
        }
        
        // Create Applicant
        Applicant applicant = new Applicant();
        applicant.setFirstName(dto.getApplicant().getFirstName());
        applicant.setLastName(dto.getApplicant().getLastName());
        applicant.setDob(dto.getApplicant().getDob());
        applicant.setGender(dto.getApplicant().getGender());
        applicant.setEmail(dto.getApplicant().getEmail());
        applicant.setPhone(dto.getApplicant().getPhone());
        applicant.setAddress(dto.getApplicant().getAddress());
        applicant.setCity(dto.getApplicant().getCity());
        applicant.setState(dto.getApplicant().getState());
        applicant.setCountry(dto.getApplicant().getCountry());
        applicant = applicantRepository.save(applicant);
        
        // Create Basic Details
        ApplicantBasicDetails basicDetails = new ApplicantBasicDetails();
        basicDetails.setApplicant(applicant);
        basicDetails.setMaritalStatus(dto.getMaritalStatus());
        basicDetails.setEducation(dto.getEducation());
        basicDetails.setNationality(dto.getNationality());
        basicDetails.setPanNumber(dto.getPanNumber());
        basicDetails.setAadhaarNumber(dto.getAadhaarNumber());
        basicDetailsRepository.save(basicDetails);
        
        // Create Employment Details
        ApplicantEmployment employment = new ApplicantEmployment();
        employment.setApplicant(applicant);
        employment.setEmployerName(dto.getEmployerName());
        employment.setDesignation(dto.getDesignation());
        employment.setEmploymentType(dto.getEmploymentType());
        employment.setStartDate(dto.getEmploymentStartDate());
        employment.setMonthlyIncome(dto.getMonthlyIncome());
        employment.setVerifiedStatus("pending");
        employmentRepository.save(employment);
        
        // Create Financial Details
        ApplicantFinancials financials = new ApplicantFinancials();
        financials.setApplicant(applicant);
        financials.setBankName(dto.getBankName());
        financials.setAccountNumber(dto.getAccountNumber());
        financials.setAccountType(dto.getAccountType());
        financials.setIfscCode(dto.getIfscCode());
        financialsRepository.save(financials);
        
        // Create Property Details
        ApplicantPropertyDetails propertyDetails = new ApplicantPropertyDetails();
        propertyDetails.setApplicant(applicant);
        propertyDetails.setResidenceType(dto.getResidenceType());
        propertyDetails.setMonthlyRent(dto.getMonthlyRent());
        propertyDetails.setYearsAtCurrentAddress(dto.getYearsAtCurrentAddress());
        propertyDetails.setPropertyValue(dto.getPropertyValue());
        propertyDetails.setPropertyType(dto.getPropertyType());
        propertyDetailsRepository.save(propertyDetails);
        
        // Create Credit History
        if (dto.getCreditScore() != null) {
            ApplicantCreditHistory creditHistory = new ApplicantCreditHistory();
            creditHistory.setApplicant(applicant);
            creditHistory.setCreditScore(dto.getCreditScore());
            creditHistory.setCreditBureau(dto.getCreditBureau());
            creditHistory.setTotalActiveLoans(0);
            creditHistory.setTotalOutstandingDebt(BigDecimal.ZERO);
            creditHistory.setCreditCardCount(0);
            creditHistory.setDefaultsCount(0);
            creditHistory.setBankruptcyFiled(false);
            creditHistoryRepository.save(creditHistory);
        }
        
        // Create Loan Details
        ApplicantLoanDetails loanDetails = new ApplicantLoanDetails();
        loanDetails.setApplicant(applicant);
        loanDetails.setLoanType(dto.getLoanType());
        loanDetails.setLoanAmount(dto.getLoanAmount());
        loanDetails.setTenureMonths(dto.getTenureMonths());
        loanDetails.setStatus("pending");
        loanDetails.setRiskScore(0);
        
        // Calculate interest rate based on loan type and credit score
        BigDecimal interestRate = calculateInterestRate(dto.getLoanType(), dto.getCreditScore());
        loanDetails.setInterestRate(interestRate);
        
        loanDetails = loanDetailsRepository.save(loanDetails);
        
        // Link all previously uploaded documents (with null loan_id) to this loan
        linkDocumentsToLoan(applicant.getApplicantId(), loanDetails.getLoanId());
        
        return applicant;
    }
    
    @Override
    @Transactional
    public Applicant submitCompleteLoanApplication(CompleteLoanApplicationDTO completeLoanApplicationDTO) {
        return completeLoanApplicationService.submitCompleteLoanApplication(completeLoanApplicationDTO);
    }
    
    @Override
    @Transactional
    public ApplicantLoanDetails submitLoanApplicationForExistingApplicant(LoanApplicationForExistingApplicantDTO dto) {
        // Get existing applicant
        Applicant applicant = applicantRepository.findById(dto.getApplicantId())
                .orElseThrow(() -> new RuntimeException("Applicant not found with ID: " + dto.getApplicantId()));
        
        // Update or create Basic Details
        ApplicantBasicDetails basicDetails = basicDetailsRepository.findByApplicant_ApplicantId(dto.getApplicantId())
                .orElse(new ApplicantBasicDetails());
        basicDetails.setApplicant(applicant);
        basicDetails.setMaritalStatus(dto.getMaritalStatus());
        basicDetails.setEducation(dto.getEducation());
        basicDetails.setNationality(dto.getNationality());
        basicDetails.setPanNumber(dto.getPanNumber());
        basicDetails.setAadhaarNumber(dto.getAadhaarNumber());
        basicDetailsRepository.save(basicDetails);
        
        // Update or create Employment Details
        ApplicantEmployment employment = employmentRepository.findByApplicant_ApplicantId(dto.getApplicantId())
                .orElse(new ApplicantEmployment());
        employment.setApplicant(applicant);
        employment.setCompanyName(dto.getEmployerName());
        employment.setDesignation(dto.getDesignation());
        employment.setEmploymentType(dto.getEmploymentType());
        employment.setMonthlyIncome(dto.getMonthlyIncome());
        employment.setVerifiedStatus("pending");
        employmentRepository.save(employment);
        
        // Update or create Financial Details
        ApplicantFinancials financials = financialsRepository.findByApplicant_ApplicantId(dto.getApplicantId())
                .orElse(new ApplicantFinancials());
        financials.setApplicant(applicant);
        financials.setBankName(dto.getBankName());
        financials.setAccountNumber(dto.getAccountNumber());
        financials.setAccountType(dto.getAccountType());
        financials.setIfscCode(dto.getIfscCode());
        financials.setTotalCreditLastMonth(dto.getTotalCreditLastMonth());
        financials.setTotalDebitLastMonth(dto.getTotalDebitLastMonth());
        financialsRepository.save(financials);
        
        // Update or create Property Details
        ApplicantPropertyDetails propertyDetails = propertyDetailsRepository.findByApplicant_ApplicantId(dto.getApplicantId())
                .orElse(new ApplicantPropertyDetails());
        propertyDetails.setApplicant(applicant);
        propertyDetails.setResidenceType(dto.getResidenceType());
        propertyDetails.setPropertyOwnership(dto.getPropertyOwnership());
        propertyDetails.setYearsAtCurrentAddress(dto.getYearsAtCurrentAddress());
        propertyDetails.setPropertyValue(dto.getPropertyValue());
        propertyDetails.setPropertyType(dto.getPropertyType());
        propertyDetails.setTotalAreaSqft(dto.getTotalAreaSqft());
        propertyDetails.setHasHomeLoan(dto.getHasHomeLoan());
        propertyDetailsRepository.save(propertyDetails);
        
        // Update or create Credit History
        ApplicantCreditHistory creditHistory = creditHistoryRepository.findByApplicant_ApplicantId(dto.getApplicantId())
                .orElse(new ApplicantCreditHistory());
        creditHistory.setApplicant(applicant);
        creditHistory.setCreditScore(dto.getCreditScore());
        creditHistory.setCreditBureau(dto.getCreditBureau());
        creditHistory.setTotalActiveLoans(dto.getTotalActiveLoans());
        creditHistory.setTotalOutstandingDebt(dto.getTotalOutstandingDebt());
        creditHistory.setTotalMonthlyEmi(dto.getTotalMonthlyEmi());
        creditHistory.setCreditCardCount(dto.getCreditCardCount());
        creditHistory.setPaymentHistory(dto.getPaymentHistory());
        creditHistory.setDefaultsCount(dto.getDefaultsCount());
        creditHistory.setBankruptcyFiled(dto.getBankruptcyFiled());
        creditHistoryRepository.save(creditHistory);
        
        // Create new Loan Details
        ApplicantLoanDetails loanDetails = new ApplicantLoanDetails();
        loanDetails.setApplicant(applicant);
        loanDetails.setLoanType(dto.getLoanType());
        loanDetails.setLoanAmount(dto.getLoanAmount());
        loanDetails.setTenureMonths(dto.getTenureMonths());
        loanDetails.setStatus("pending");
        loanDetails.setRiskScore(0);
        
        // Calculate interest rate
        BigDecimal interestRate = calculateInterestRate(dto.getLoanType(), dto.getCreditScore());
        loanDetails.setInterestRate(interestRate);
        
        loanDetails = loanDetailsRepository.save(loanDetails);
        
        // Link all previously uploaded documents (with null loan_id) to this loan
        linkDocumentsToLoan(applicant.getApplicantId(), loanDetails.getLoanId());
        
        // Save Documents if provided
        if (dto.getDocuments() != null && !dto.getDocuments().isEmpty()) {
            saveDocuments(applicant, dto.getDocuments());
            
            // TODO: Document extraction will be triggered separately via DocumentUploadController
            // when documents are uploaded, not during loan submission
            System.out.println("Documents saved for applicant ID: " + applicant.getApplicantId());
        }
        
        // Save References if provided
        if (dto.getReferences() != null && !dto.getReferences().isEmpty()) {
            saveReferences(applicant, dto.getReferences());
        }
        
        // Save Dependents if provided
        if (dto.getDependents() != null && !dto.getDependents().isEmpty()) {
            saveDependents(applicant, dto.getDependents());
        }
        
        // Save Collaterals if provided
        if (dto.getCollaterals() != null && !dto.getCollaterals().isEmpty()) {
            saveCollaterals(loanDetails, dto.getCollaterals());
        }
        
        return loanDetails;
    }
    
    @Override
    public Applicant getApplicantById(Long applicantId) {
        return applicantRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("Applicant not found with ID: " + applicantId));
    }
    
    @Override
    public List<ApplicantLoanDetailsDTO> getApplicantLoans(Long applicantId) {
        List<ApplicantLoanDetails> loans = loanDetailsRepository.findByApplicant_ApplicantId(applicantId);
        return loans.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    private ApplicantLoanDetailsDTO convertToDTO(ApplicantLoanDetails loan) {
        ApplicantLoanDetailsDTO dto = new ApplicantLoanDetailsDTO();
        dto.setLoanId(loan.getLoanId());
        dto.setLoanType(loan.getLoanType());
        dto.setLoanAmount(loan.getLoanAmount());
        dto.setInterestRate(loan.getInterestRate());
        dto.setTenureMonths(loan.getTenureMonths());
        dto.setStatus(loan.getStatus());
        dto.setLoanPurpose(loan.getLoanPurpose());
        dto.setApplicationStatus(loan.getApplicationStatus());
        // Use status field for loanStatus since loan_status column may not exist
        dto.setLoanStatus(loan.getStatus() != null ? loan.getStatus().toUpperCase() : "PENDING");
        dto.setRiskScore(loan.getRiskScore());
        dto.setSubmittedAt(loan.getSubmittedAt());
        dto.setReviewedAt(loan.getReviewedAt());
        
        // Set applicant basic info without circular reference
        if (loan.getApplicant() != null) {
            dto.setApplicantId(loan.getApplicant().getApplicantId());
            dto.setApplicantFirstName(loan.getApplicant().getFirstName());
            dto.setApplicantLastName(loan.getApplicant().getLastName());
            dto.setApplicantEmail(loan.getApplicant().getEmail());
            dto.setApplicantPhone(loan.getApplicant().getPhone());
        }
        
        return dto;
    }
    
    @Override
    public List<Applicant> getAllApplicants() {
        return applicantRepository.findAll();
    }
    
    @Override
    public ApplicantLoanDetails getLoanById(Long loanId) {
        return loanDetailsRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found with ID: " + loanId));
    }
    
    @Override
    public ApplicantLoanDetails saveLoanDetails(ApplicantLoanDetails loanDetails) {
        return loanDetailsRepository.save(loanDetails);
    }
    
    private BigDecimal calculateInterestRate(String loanType, Integer creditScore) {
        BigDecimal baseRate;
        
        // Base rates by loan type
        switch (loanType.toLowerCase()) {
            case "home":
                baseRate = new BigDecimal("8.5");
                break;
            case "gold":
                baseRate = new BigDecimal("7.0");
                break;
            case "personal":
                baseRate = new BigDecimal("12.0");
                break;
            case "vehicle":
                baseRate = new BigDecimal("9.5");
                break;
            case "education":
                baseRate = new BigDecimal("10.0");
                break;
            case "business":
                baseRate = new BigDecimal("11.0");
                break;
            default:
                baseRate = new BigDecimal("12.0");
        }
        
        // Adjust based on credit score
        if (creditScore != null) {
            if (creditScore >= 750) {
                baseRate = baseRate.subtract(new BigDecimal("1.0")); // -1% for excellent credit
            } else if (creditScore >= 700) {
                baseRate = baseRate.subtract(new BigDecimal("0.5")); // -0.5% for good credit
            } else if (creditScore < 650) {
                baseRate = baseRate.add(new BigDecimal("2.0")); // +2% for poor credit
            }
        }
        
        return baseRate;
    }
    
    // Helper methods for saving related entities
    private void saveDocuments(Applicant applicant, List<LoanApplicationForExistingApplicantDTO.DocumentDTO> documents) {
        // Implementation would depend on your Document entity structure
        // This is a placeholder - implement based on your document entity
        for (LoanApplicationForExistingApplicantDTO.DocumentDTO docDto : documents) {
            // Create and save document entity
            // Document document = new Document();
            // document.setApplicant(applicant);
            // document.setDocType(docDto.getDocType());
            // ... set other fields
            // documentRepository.save(document);
        }
    }
    
    private void saveReferences(Applicant applicant, List<LoanApplicationForExistingApplicantDTO.ReferenceDTO> references) {
        // Implementation would depend on your Reference entity structure
        for (LoanApplicationForExistingApplicantDTO.ReferenceDTO refDto : references) {
            // Create and save reference entity
            // Reference reference = new Reference();
            // reference.setApplicant(applicant);
            // reference.setReferenceName(refDto.getReferenceName());
            // ... set other fields
            // referenceRepository.save(reference);
        }
    }
    
    private void saveDependents(Applicant applicant, List<LoanApplicationForExistingApplicantDTO.DependentDTO> dependents) {
        // Implementation would depend on your Dependent entity structure
        for (LoanApplicationForExistingApplicantDTO.DependentDTO depDto : dependents) {
            // Create and save dependent entity
            // Dependent dependent = new Dependent();
            // dependent.setApplicant(applicant);
            // dependent.setDependentName(depDto.getDependentName());
            // ... set other fields
            // dependentRepository.save(dependent);
        }
    }
    
    private void saveCollaterals(ApplicantLoanDetails loanDetails, List<LoanApplicationForExistingApplicantDTO.CollateralDTO> collaterals) {
        // Implementation would depend on your Collateral entity structure
        for (LoanApplicationForExistingApplicantDTO.CollateralDTO colDto : collaterals) {
            // Create and save collateral entity
            // LoanCollateral collateral = new LoanCollateral();
            // collateral.setLoanDetails(loanDetails);
            // collateral.setCollateralType(colDto.getCollateralType());
            // ... set other fields
            // collateralRepository.save(collateral);
        }
    }
    
    /**
     * Link all documents with null loan_id for this applicant to the specified loan
     */
    private void linkDocumentsToLoan(Long applicantId, Long loanId) {
        // Find the loan entity
        ApplicantLoanDetails loan = loanDetailsRepository.findById(loanId).orElse(null);
        if (loan == null) {
            return;
        }
        
        // Find all documents for this applicant with null loan_id
        List<UploadedDocument> documents = uploadedDocumentRepository.findByApplicant_ApplicantId(applicantId);
        int linkedCount = 0;
        
        for (UploadedDocument doc : documents) {
            if (doc.getLoan() == null) {
                doc.setLoan(loan);
                uploadedDocumentRepository.save(doc);
                linkedCount++;
            }
        }
        
        if (linkedCount > 0) {
            System.out.println("Linked " + linkedCount + " document(s) to loan ID: " + loanId);
        }
    }
}
