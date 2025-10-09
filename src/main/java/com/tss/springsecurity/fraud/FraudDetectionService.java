package com.tss.springsecurity.fraud;

import com.tss.springsecurity.entity.Applicant;
import com.tss.springsecurity.entity.ApplicantLoanDetails;
import com.tss.springsecurity.entity.FraudFlag;
import com.tss.springsecurity.repository.ApplicantLoanDetailsRepository;
import com.tss.springsecurity.repository.ApplicantRepository;
import com.tss.springsecurity.repository.FraudFlagRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FraudDetectionService {
    
    private final IdentityFraudDetectionEngine identityFraudEngine;
    private final FinancialFraudDetectionEngine financialFraudEngine;
    private final EmploymentFraudDetectionEngine employmentFraudEngine;
    private final CrossVerificationFraudDetectionEngine crossVerificationEngine;
    private final ApplicantRepository applicantRepository;
    private final ApplicantLoanDetailsRepository loanDetailsRepository;
    private final FraudFlagRepository fraudFlagRepository;
    
    public FraudDetectionService(
            IdentityFraudDetectionEngine identityFraudEngine,
            FinancialFraudDetectionEngine financialFraudEngine,
            EmploymentFraudDetectionEngine employmentFraudEngine,
            CrossVerificationFraudDetectionEngine crossVerificationEngine,
            ApplicantRepository applicantRepository,
            ApplicantLoanDetailsRepository loanDetailsRepository,
            FraudFlagRepository fraudFlagRepository) {
        this.identityFraudEngine = identityFraudEngine;
        this.financialFraudEngine = financialFraudEngine;
        this.employmentFraudEngine = employmentFraudEngine;
        this.crossVerificationEngine = crossVerificationEngine;
        this.applicantRepository = applicantRepository;
        this.loanDetailsRepository = loanDetailsRepository;
        this.fraudFlagRepository = fraudFlagRepository;
    }
    
    /**
     * Run complete fraud detection for an applicant and save flags to database
     */
    @Transactional
    public FraudDetectionResult runFraudDetection(Long applicantId) {
        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("Applicant not found with ID: " + applicantId));
        
        // Run identity fraud detection
        FraudDetectionResult identityResult = identityFraudEngine.detectIdentityFraud(applicantId);
        
        // Run financial fraud detection
        FraudDetectionResult financialResult = financialFraudEngine.detectFinancialFraud(applicantId);
        
        // Run employment fraud detection
        FraudDetectionResult employmentResult = employmentFraudEngine.detectEmploymentFraud(applicantId);
        
        // Run cross-verification fraud detection
        FraudDetectionResult crossVerificationResult = crossVerificationEngine.detectCrossVerificationFraud(applicantId);
        
        // Merge results
        FraudDetectionResult combinedResult = mergeResults(identityResult, financialResult, employmentResult, crossVerificationResult);
        
        // Get the latest loan application for this applicant
        List<ApplicantLoanDetails> loans = loanDetailsRepository
                .findByApplicant_ApplicantId(applicantId);
        
        ApplicantLoanDetails latestLoan = loans.isEmpty() ? null : 
                loans.get(loans.size() - 1);
        
        // Save fraud flags to database
        saveFraudFlags(applicant, latestLoan, combinedResult);
        
        // Update loan risk score if loan exists
        if (latestLoan != null) {
            latestLoan.setRiskScore(combinedResult.getTotalFraudScore());
            
            // Update loan status based on fraud score
            if (combinedResult.getTotalFraudScore() >= 60) {
                latestLoan.setStatus("rejected_fraud");
            } else if (combinedResult.getTotalFraudScore() >= 30) {
                latestLoan.setStatus("under_review");
            }
            
            loanDetailsRepository.save(latestLoan);
        }
        
        return combinedResult;
    }
    
    /**
     * Run only identity fraud detection
     */
    @Transactional
    public FraudDetectionResult runIdentityFraudDetection(Long applicantId) {
        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("Applicant not found with ID: " + applicantId));
        
        FraudDetectionResult result = identityFraudEngine.detectIdentityFraud(applicantId);
        
        List<ApplicantLoanDetails> loans = loanDetailsRepository
                .findByApplicant_ApplicantId(applicantId);
        ApplicantLoanDetails latestLoan = loans.isEmpty() ? null : loans.get(loans.size() - 1);
        
        saveFraudFlags(applicant, latestLoan, result);
        
        return result;
    }
    
    /**
     * Run only financial fraud detection
     */
    @Transactional
    public FraudDetectionResult runFinancialFraudDetection(Long applicantId) {
        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("Applicant not found with ID: " + applicantId));
        
        FraudDetectionResult result = financialFraudEngine.detectFinancialFraud(applicantId);
        
        List<ApplicantLoanDetails> loans = loanDetailsRepository
                .findByApplicant_ApplicantId(applicantId);
        ApplicantLoanDetails latestLoan = loans.isEmpty() ? null : loans.get(loans.size() - 1);
        
        saveFraudFlags(applicant, latestLoan, result);
        
        return result;
    }
    
    /**
     * Run only employment fraud detection
     */
    @Transactional
    public FraudDetectionResult runEmploymentFraudDetection(Long applicantId) {
        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("Applicant not found with ID: " + applicantId));
        
        FraudDetectionResult result = employmentFraudEngine.detectEmploymentFraud(applicantId);
        
        List<ApplicantLoanDetails> loans = loanDetailsRepository
                .findByApplicant_ApplicantId(applicantId);
        ApplicantLoanDetails latestLoan = loans.isEmpty() ? null : loans.get(loans.size() - 1);
        
        saveFraudFlags(applicant, latestLoan, result);
        
        return result;
    }
    
    /**
     * Run only cross-verification fraud detection
     */
    @Transactional
    public FraudDetectionResult runCrossVerificationFraudDetection(Long applicantId) {
        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("Applicant not found with ID: " + applicantId));
        
        FraudDetectionResult result = crossVerificationEngine.detectCrossVerificationFraud(applicantId);
        
        List<ApplicantLoanDetails> loans = loanDetailsRepository
                .findByApplicant_ApplicantId(applicantId);
        ApplicantLoanDetails latestLoan = loans.isEmpty() ? null : loans.get(loans.size() - 1);
        
        saveFraudFlags(applicant, latestLoan, result);
        
        return result;
    }
    
    /**
     * Merge identity, financial, employment, and cross-verification fraud results
     */
    private FraudDetectionResult mergeResults(FraudDetectionResult identity, 
                                              FraudDetectionResult financial,
                                              FraudDetectionResult employment,
                                              FraudDetectionResult crossVerification) {
        FraudDetectionResult merged = new FraudDetectionResult();
        merged.setApplicantId(identity.getApplicantId());
        merged.setApplicantName(identity.getApplicantName());
        
        // Add all triggered rules from all four engines
        merged.getTriggeredRules().addAll(identity.getTriggeredRules());
        merged.getTriggeredRules().addAll(financial.getTriggeredRules());
        merged.getTriggeredRules().addAll(employment.getTriggeredRules());
        merged.getTriggeredRules().addAll(crossVerification.getTriggeredRules());
        
        // Calculate combined fraud score
        merged.setTotalFraudScore(identity.getTotalFraudScore() + 
                                 financial.getTotalFraudScore() + 
                                 employment.getTotalFraudScore() +
                                 crossVerification.getTotalFraudScore());
        
        // Recalculate risk level based on combined score
        merged.calculateRiskLevel();
        
        return merged;
    }
    
    /**
     * Save fraud flags to database
     */
    private void saveFraudFlags(Applicant applicant, ApplicantLoanDetails loan, 
                               FraudDetectionResult result) {
        for (FraudRule rule : result.getTriggeredRules()) {
            FraudFlag flag = new FraudFlag();
            flag.setApplicant(applicant);
            flag.setLoan(loan);
            flag.setRuleName(rule.getRuleName());
            flag.setFlagNotes(rule.getFlagDetails());
            
            // Map severity to integer (1=LOW, 2=MEDIUM, 3=HIGH, 4=CRITICAL)
            int severityInt = switch (rule.getSeverity()) {
                case "LOW" -> 1;
                case "MEDIUM" -> 2;
                case "HIGH" -> 3;
                case "CRITICAL" -> 4;
                default -> 2;
            };
            flag.setSeverity(severityInt);
            
            fraudFlagRepository.save(flag);
        }
    }
    
    /**
     * Get all fraud flags for an applicant
     */
    public List<FraudFlag> getFraudFlags(Long applicantId) {
        return fraudFlagRepository.findByApplicant_ApplicantId(applicantId);
    }
    
    /**
     * Get fraud flags for a specific loan
     */
    public List<FraudFlag> getLoanFraudFlags(Long loanId) {
        return fraudFlagRepository.findByLoan_LoanId(loanId);
    }
    
    /**
     * Get high severity fraud flags across all applicants
     */
    public List<FraudFlag> getHighSeverityFlags() {
        return fraudFlagRepository.findBySeverity(3); // HIGH
    }
    
    /**
     * Get critical fraud flags across all applicants
     */
    public List<FraudFlag> getCriticalFlags() {
        return fraudFlagRepository.findBySeverity(4); // CRITICAL
    }
}
