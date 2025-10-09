package com.tss.springsecurity.service.impl;

import com.tss.springsecurity.dto.CompleteLoanApplicationDTO;
import com.tss.springsecurity.entity.*;
import com.tss.springsecurity.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class CompleteLoanApplicationServiceImpl {
    
    private final ApplicantRepository applicantRepository;
    private final ApplicantBasicDetailsRepository basicDetailsRepository;
    private final ApplicantEmploymentRepository employmentRepository;
    private final ApplicantFinancialsRepository financialsRepository;
    private final ApplicantPropertyDetailsRepository propertyDetailsRepository;
    private final ApplicantCreditHistoryRepository creditHistoryRepository;
    private final ApplicantLoanDetailsRepository loanDetailsRepository;
    private final AadhaarDetailsRepository aadhaarDetailsRepository;
    private final PanDetailsRepository panDetailsRepository;
    private final PassportDetailsRepository passportDetailsRepository;
    private final OtherDocumentRepository otherDocumentRepository;
    private final ApplicantDependentRepository dependentRepository;
    private final LoanCollateralRepository collateralRepository;
    
    public CompleteLoanApplicationServiceImpl(
            ApplicantRepository applicantRepository,
            ApplicantBasicDetailsRepository basicDetailsRepository,
            ApplicantEmploymentRepository employmentRepository,
            ApplicantFinancialsRepository financialsRepository,
            ApplicantPropertyDetailsRepository propertyDetailsRepository,
            ApplicantCreditHistoryRepository creditHistoryRepository,
            ApplicantLoanDetailsRepository loanDetailsRepository,
            AadhaarDetailsRepository aadhaarDetailsRepository,
            PanDetailsRepository panDetailsRepository,
            PassportDetailsRepository passportDetailsRepository,
            OtherDocumentRepository otherDocumentRepository,
            ApplicantDependentRepository dependentRepository,
            LoanCollateralRepository collateralRepository) {
        this.applicantRepository = applicantRepository;
        this.basicDetailsRepository = basicDetailsRepository;
        this.employmentRepository = employmentRepository;
        this.financialsRepository = financialsRepository;
        this.propertyDetailsRepository = propertyDetailsRepository;
        this.creditHistoryRepository = creditHistoryRepository;
        this.loanDetailsRepository = loanDetailsRepository;
        this.aadhaarDetailsRepository = aadhaarDetailsRepository;
        this.panDetailsRepository = panDetailsRepository;
        this.passportDetailsRepository = passportDetailsRepository;
        this.otherDocumentRepository = otherDocumentRepository;
        this.dependentRepository = dependentRepository;
        this.collateralRepository = collateralRepository;
    }
    
    @Transactional
    public Applicant submitCompleteLoanApplication(CompleteLoanApplicationDTO dto) {
        // Validate duplicate
        if (applicantRepository.existsByEmail(dto.getApplicant().getEmail())) {
            throw new RuntimeException("Applicant with email " + dto.getApplicant().getEmail() + " already exists");
        }
        if (applicantRepository.existsByPhone(dto.getApplicant().getPhone())) {
            throw new RuntimeException("Applicant with phone " + dto.getApplicant().getPhone() + " already exists");
        }
        
        // 1. Create Applicant
        Applicant applicant = createApplicant(dto);
        
        // 2. Create Basic Details
        createBasicDetails(applicant, dto);
        
        // 3. Create Employment
        createEmployment(applicant, dto);
        
        // 4. Create Financials
        createFinancials(applicant, dto);
        
        // 5. Create Property Details
        createPropertyDetails(applicant, dto);
        
        // 6. Create Credit History
        createCreditHistory(applicant, dto);
        
        // 7. Create Loan Details
        ApplicantLoanDetails loan = createLoanDetails(applicant, dto);
        
        // 8. Create Documents
        createDocuments(applicant, dto);

        
        // 10. Create Dependents
        createDependents(applicant, dto);
        
     
        
        // 12. Create Collaterals
        createCollaterals(loan, dto);
        
        return applicant;
    }
    
    private Applicant createApplicant(CompleteLoanApplicationDTO dto) {
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
        return applicantRepository.save(applicant);
    }
    
    private void createBasicDetails(Applicant applicant, CompleteLoanApplicationDTO dto) {
        ApplicantBasicDetails basicDetails = new ApplicantBasicDetails();
        basicDetails.setApplicant(applicant);
        basicDetails.setMaritalStatus(dto.getMaritalStatus());
        basicDetails.setEducation(dto.getEducation());
        basicDetails.setNationality(dto.getNationality());
        basicDetails.setPanNumber(dto.getPanNumber());
        basicDetails.setAadhaarNumber(dto.getAadhaarNumber());
        basicDetails.setVoterId(dto.getVoterId());
        basicDetails.setPassportNumber(dto.getPassportNumber());
        basicDetailsRepository.save(basicDetails);
    }
    
    private void createEmployment(Applicant applicant, CompleteLoanApplicationDTO dto) {
        ApplicantEmployment employment = new ApplicantEmployment();
        employment.setApplicant(applicant);
        employment.setEmployerName(dto.getEmployerName());
        employment.setDesignation(dto.getDesignation());
        employment.setEmploymentType(dto.getEmploymentType());
        employment.setStartDate(dto.getEmploymentStartDate());
        employment.setMonthlyIncome(dto.getMonthlyIncome());
        employment.setVerifiedStatus("pending");
        employmentRepository.save(employment);
    }
    
    private void createFinancials(Applicant applicant, CompleteLoanApplicationDTO dto) {
        ApplicantFinancials financials = new ApplicantFinancials();
        financials.setApplicant(applicant);
        financials.setBankName(dto.getBankName());
        financials.setAccountNumber(dto.getAccountNumber());
        financials.setAccountType(dto.getAccountType());
        financials.setIfscCode(dto.getIfscCode());
        financials.setTotalCreditLastMonth(dto.getTotalCreditLastMonth());
        financials.setTotalDebitLastMonth(dto.getTotalDebitLastMonth());
        financials.setAnomalies(dto.getAnomalies());
        financialsRepository.save(financials);
    }
    
    private void createPropertyDetails(Applicant applicant, CompleteLoanApplicationDTO dto) {
        ApplicantPropertyDetails propertyDetails = new ApplicantPropertyDetails();
        propertyDetails.setApplicant(applicant);
        propertyDetails.setResidenceType(dto.getResidenceType());
        propertyDetails.setPropertyOwnership(dto.getPropertyOwnership());
        propertyDetails.setMonthlyRent(dto.getMonthlyRent());
        propertyDetails.setYearsAtCurrentAddress(dto.getYearsAtCurrentAddress());
        propertyDetails.setPropertyValue(dto.getPropertyValue());
        propertyDetails.setPropertyType(dto.getPropertyType());
        propertyDetails.setTotalAreaSqft(dto.getTotalAreaSqft());
        propertyDetails.setHasHomeLoan(dto.getHasHomeLoan());
        propertyDetails.setOutstandingHomeLoan(dto.getOutstandingHomeLoan());
        propertyDetails.setHomeLoanEmi(dto.getHomeLoanEmi());
        propertyDetailsRepository.save(propertyDetails);
    }
    
    private void createCreditHistory(Applicant applicant, CompleteLoanApplicationDTO dto) {
        if (dto.getCreditScore() != null) {
            ApplicantCreditHistory creditHistory = new ApplicantCreditHistory();
            creditHistory.setApplicant(applicant);
            creditHistory.setCreditScore(dto.getCreditScore());
            creditHistory.setCreditBureau(dto.getCreditBureau());
            creditHistory.setTotalActiveLoans(dto.getTotalActiveLoans());
            creditHistory.setTotalOutstandingDebt(dto.getTotalOutstandingDebt());
            creditHistory.setTotalMonthlyEmi(dto.getTotalMonthlyEmi());
            creditHistory.setCreditCardCount(dto.getCreditCardCount());
            creditHistory.setTotalCreditLimit(dto.getTotalCreditLimit());
            creditHistory.setCreditUtilizationRatio(dto.getCreditUtilizationRatio());
            creditHistory.setPaymentHistory(dto.getPaymentHistory());
            creditHistory.setDefaultsCount(dto.getDefaultsCount());
            creditHistory.setBankruptcyFiled(dto.getBankruptcyFiled());
            creditHistory.setCreditReportUrl(dto.getCreditReportUrl());
            creditHistory.setLastCheckedAt(LocalDateTime.now());
            creditHistoryRepository.save(creditHistory);
        }
    }
    
    private ApplicantLoanDetails createLoanDetails(Applicant applicant, CompleteLoanApplicationDTO dto) {
        ApplicantLoanDetails loanDetails = new ApplicantLoanDetails();
        loanDetails.setApplicant(applicant);
        loanDetails.setLoanType(dto.getLoanType());
        loanDetails.setLoanAmount(dto.getLoanAmount());
        loanDetails.setTenureMonths(dto.getTenureMonths());
        loanDetails.setStatus("pending");
        loanDetails.setRiskScore(0);
        
        BigDecimal interestRate = calculateInterestRate(dto.getLoanType(), dto.getCreditScore());
        loanDetails.setInterestRate(interestRate);
        
        return loanDetailsRepository.save(loanDetails);
    }
    
    private void createDocuments(Applicant applicant, CompleteLoanApplicationDTO dto) {
        if (dto.getDocuments() != null && !dto.getDocuments().isEmpty()) {
            for (CompleteLoanApplicationDTO.DocumentDTO docDTO : dto.getDocuments()) {
                switch (docDTO.getDocType().toLowerCase()) {
                    case "aadhaar":
                        AadhaarDetails aadhaar = new AadhaarDetails();
                        aadhaar.setApplicant(applicant);
                        aadhaar.setAadhaarNumber(docDTO.getDocNumber());
                        aadhaar.setName(docDTO.getName());
                        aadhaar.setDob(docDTO.getDob());
                        aadhaar.setGender(docDTO.getGender());
                        aadhaar.setAddress(docDTO.getAddress());
                        aadhaar.setQrCodeData(docDTO.getQrCodeData());
                        aadhaar.setCloudinaryUrl(docDTO.getCloudinaryUrl());
                        aadhaar.setOcrText(docDTO.getOcrText());
                        aadhaar.setIsTampered(docDTO.getIsTampered());
                        aadhaarDetailsRepository.save(aadhaar);
                        break;
                        
                    case "pan":
                        PanDetails pan = new PanDetails();
                        pan.setApplicant(applicant);
                        pan.setPanNumber(docDTO.getDocNumber());
                        pan.setName(docDTO.getName());
                        pan.setFatherName(docDTO.getFatherName());
                        pan.setDob(docDTO.getDob());
                        pan.setCloudinaryUrl(docDTO.getCloudinaryUrl());
                        pan.setOcrText(docDTO.getOcrText());
                        pan.setIsTampered(docDTO.getIsTampered());
                        panDetailsRepository.save(pan);
                        break;
                        
                    case "passport":
                        PassportDetails passport = new PassportDetails();
                        passport.setApplicant(applicant);
                        passport.setPassportNumber(docDTO.getDocNumber());
                        passport.setName(docDTO.getName());
                        passport.setDob(docDTO.getDob());
                        passport.setNationality(docDTO.getNationality());
                        passport.setExpiryDate(docDTO.getExpiryDate());
                        passport.setCloudinaryUrl(docDTO.getCloudinaryUrl());
                        passport.setOcrText(docDTO.getOcrText());
                        passport.setIsTampered(docDTO.getIsTampered());
                        passportDetailsRepository.save(passport);
                        break;
                        
                    default:
                        OtherDocument otherDoc = new OtherDocument();
                        otherDoc.setApplicant(applicant);
                        otherDoc.setDocType(docDTO.getDocType());
                        otherDoc.setDocNumber(docDTO.getDocNumber());
                        otherDoc.setCloudinaryUrl(docDTO.getCloudinaryUrl());
                        otherDoc.setOcrText(docDTO.getOcrText());
                        otherDoc.setIsTampered(docDTO.getIsTampered());
                        otherDocumentRepository.save(otherDoc);
                        break;
                }
            }
        }
    }
    
  
    private void createDependents(Applicant applicant, CompleteLoanApplicationDTO dto) {
        if (dto.getDependents() != null && !dto.getDependents().isEmpty()) {
            for (CompleteLoanApplicationDTO.DependentDTO depDTO : dto.getDependents()) {
                ApplicantDependent dependent = new ApplicantDependent();
                dependent.setApplicant(applicant);
                dependent.setDependentName(depDTO.getDependentName());
                dependent.setRelationship(depDTO.getRelationship());
                dependent.setDob(depDTO.getDob());
                dependent.setAge(depDTO.getAge());
                dependent.setIsFinanciallyDependent(depDTO.getIsFinanciallyDependent());
                dependent.setEducationStatus(depDTO.getEducationStatus());
                dependent.setOccupation(depDTO.getOccupation());
                dependentRepository.save(dependent);
            }
        }
    }
    
   
    
    private void createCollaterals(ApplicantLoanDetails loan, CompleteLoanApplicationDTO dto) {
        if (dto.getCollaterals() != null && !dto.getCollaterals().isEmpty()) {
            for (CompleteLoanApplicationDTO.CollateralDTO collDTO : dto.getCollaterals()) {
                LoanCollateral collateral = new LoanCollateral();
                collateral.setLoan(loan);
                collateral.setCollateralType(collDTO.getCollateralType());
                collateral.setCollateralDescription(collDTO.getCollateralDescription());
                collateral.setEstimatedValue(collDTO.getEstimatedValue());
                collateral.setValuationDate(LocalDateTime.now());
                collateral.setValuationBy(collDTO.getValuationBy());
                collateral.setOwnershipProofUrl(collDTO.getOwnershipProofUrl());
                collateral.setValuationReportUrl(collDTO.getValuationReportUrl());
                collateral.setIsVerified(false);
                collateralRepository.save(collateral);
            }
        }
    }
    
    private BigDecimal calculateInterestRate(String loanType, Integer creditScore) {
        BigDecimal baseRate;
        
        switch (loanType.toLowerCase()) {
            case "home": baseRate = new BigDecimal("8.5"); break;
            case "gold": baseRate = new BigDecimal("7.0"); break;
            case "personal": baseRate = new BigDecimal("12.0"); break;
            case "vehicle": baseRate = new BigDecimal("9.5"); break;
            case "education": baseRate = new BigDecimal("10.0"); break;
            case "business": baseRate = new BigDecimal("11.0"); break;
            default: baseRate = new BigDecimal("12.0");
        }
        
        if (creditScore != null) {
            if (creditScore >= 750) baseRate = baseRate.subtract(new BigDecimal("1.0"));
            else if (creditScore >= 700) baseRate = baseRate.subtract(new BigDecimal("0.5"));
            else if (creditScore < 650) baseRate = baseRate.add(new BigDecimal("2.0"));
        }
        
        return baseRate;
    }
}
