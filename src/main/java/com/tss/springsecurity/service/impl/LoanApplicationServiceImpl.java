package com.tss.springsecurity.service.impl;

import com.tss.springsecurity.dto.CompleteLoanApplicationDTO;
import com.tss.springsecurity.dto.LoanApplicationDTO;
import com.tss.springsecurity.entity.*;
import com.tss.springsecurity.repository.*;
import com.tss.springsecurity.service.LoanApplicationService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

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
    
    public LoanApplicationServiceImpl(
            ApplicantRepository applicantRepository,
            ApplicantBasicDetailsRepository basicDetailsRepository,
            ApplicantEmploymentRepository employmentRepository,
            ApplicantFinancialsRepository financialsRepository,
            ApplicantPropertyDetailsRepository propertyDetailsRepository,
            ApplicantCreditHistoryRepository creditHistoryRepository,
            ApplicantLoanDetailsRepository loanDetailsRepository,
            CompleteLoanApplicationServiceImpl completeLoanApplicationService) {
        this.applicantRepository = applicantRepository;
        this.basicDetailsRepository = basicDetailsRepository;
        this.employmentRepository = employmentRepository;
        this.financialsRepository = financialsRepository;
        this.propertyDetailsRepository = propertyDetailsRepository;
        this.creditHistoryRepository = creditHistoryRepository;
        this.loanDetailsRepository = loanDetailsRepository;
        this.completeLoanApplicationService = completeLoanApplicationService;
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
        
        loanDetailsRepository.save(loanDetails);
        
        return applicant;
    }
    
    @Override
    @Transactional
    public Applicant submitCompleteLoanApplication(CompleteLoanApplicationDTO completeLoanApplicationDTO) {
        return completeLoanApplicationService.submitCompleteLoanApplication(completeLoanApplicationDTO);
    }
    
    @Override
    public Applicant getApplicantById(Long applicantId) {
        return applicantRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("Applicant not found with ID: " + applicantId));
    }
    
    @Override
    public List<ApplicantLoanDetails> getApplicantLoans(Long applicantId) {
        return loanDetailsRepository.findByApplicant_ApplicantId(applicantId);
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
}
