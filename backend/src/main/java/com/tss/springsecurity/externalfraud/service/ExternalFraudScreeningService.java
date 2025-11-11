package com.tss.springsecurity.externalfraud.service;

import com.tss.springsecurity.entity.Applicant;
import com.tss.springsecurity.externalfraud.engine.ExternalFraudRuleEngine;
import com.tss.springsecurity.externalfraud.model.ExternalFraudCheckRequest;
import com.tss.springsecurity.externalfraud.model.ExternalFraudCheckResult;
import com.tss.springsecurity.repository.ApplicantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@ConditionalOnProperty(name = "external-fraud.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class ExternalFraudScreeningService {
    
    @Autowired
    private ExternalFraudRuleEngine fraudRuleEngine;
    
    @Autowired
    private ApplicantRepository applicantRepository;
    
    /**
     * Perform external fraud screening for an applicant
     */
    public ExternalFraudCheckResult screenApplicant(Long applicantId) {
        log.info("Starting external fraud screening for applicant ID: {}", applicantId);
        
        try {
            // Get applicant from internal database
            Applicant applicant = applicantRepository.findById(applicantId)
                    .orElseThrow(() -> new RuntimeException("Applicant not found: " + applicantId));
            
            // Build external fraud check request
            ExternalFraudCheckRequest request = buildFraudCheckRequest(applicant);
            
            // Perform fraud screening using rule engine
            ExternalFraudCheckResult result = fraudRuleEngine.performFraudCheck(request);
            
            log.info("External fraud screening completed for applicant ID: {} with result: {}", 
                    applicantId, result.getRiskLevel());
            
            return result;
            
        } catch (Exception e) {
            log.error("Error during external fraud screening for applicant ID: {}", applicantId, e);
            
            // Return error result
            ExternalFraudCheckResult errorResult = ExternalFraudCheckResult.builder()
                    .personFound(false)
                    .riskLevel("MEDIUM")
                    .recommendation("REVIEW")
                    .build();
            
            errorResult.addFraudFlag(com.tss.springsecurity.externalfraud.model.ExternalFraudFlag.create(
                "SCREENING_ERROR",
                "External Screening Error",
                "SYSTEM",
                "MEDIUM",
                25,
                "Error occurred during external fraud screening",
                e.getMessage()
            ));
            
            errorResult.calculateRiskLevel();
            return errorResult;
        }
    }
    
    /**
     * Perform external fraud screening with custom parameters
     */
    public ExternalFraudCheckResult screenApplicant(Long applicantId, boolean deepScreening) {
        log.info("Starting external fraud screening for applicant ID: {} with deep screening: {}", 
                applicantId, deepScreening);
        
        try {
            Applicant applicant = applicantRepository.findById(applicantId)
                    .orElseThrow(() -> new RuntimeException("Applicant not found: " + applicantId));
            
            ExternalFraudCheckRequest request = buildFraudCheckRequest(applicant);
            request.setPerformDeepScreening(deepScreening);
            
            return fraudRuleEngine.performFraudCheck(request);
            
        } catch (Exception e) {
            log.error("Error during external fraud screening for applicant ID: {}", applicantId, e);
            throw new RuntimeException("External fraud screening failed", e);
        }
    }
    
    /**
     * Perform external fraud screening for specific identifiers
     */
    public ExternalFraudCheckResult screenByIdentifiers(String panNumber, String aadhaarNumber, 
                                                       String phoneNumber, String email) {
        log.info("Starting external fraud screening for identifiers - PAN: {}, Aadhaar: {}", 
                maskIdentifier(panNumber), maskIdentifier(aadhaarNumber));
        
        ExternalFraudCheckRequest request = ExternalFraudCheckRequest.builder()
                .panNumber(panNumber)
                .aadhaarNumber(aadhaarNumber)
                .phoneNumber(phoneNumber)
                .email(email)
                .checkCriminalRecords(true)
                .checkLoanHistory(true)
                .checkBankRecords(true)
                .checkDocumentVerification(true)
                .build();
        
        return fraudRuleEngine.performFraudCheck(request);
    }
    
    private ExternalFraudCheckRequest buildFraudCheckRequest(Applicant applicant) {
        // Extract PAN number from PAN details (get first active PAN if available)
        String panNumber = null;
        if (applicant.getPanDetails() != null && !applicant.getPanDetails().isEmpty()) {
            panNumber = applicant.getPanDetails().get(0).getPanNumber();
        }
        
        // Extract Aadhaar number from Aadhaar details (get first active Aadhaar if available)
        String aadhaarNumber = null;
        if (applicant.getAadhaarDetails() != null && !applicant.getAadhaarDetails().isEmpty()) {
            aadhaarNumber = applicant.getAadhaarDetails().get(0).getAadhaarNumber();
        }
        
        return ExternalFraudCheckRequest.builder()
                .panNumber(panNumber)
                .aadhaarNumber(aadhaarNumber)
                .phoneNumber(applicant.getPhone())
                .email(applicant.getEmail())
                .firstName(applicant.getFirstName())
                .lastName(applicant.getLastName())
                .dateOfBirth(applicant.getDob()) // Use dob field directly
                .internalApplicantId(applicant.getApplicantId())
                .checkCriminalRecords(true)
                .checkLoanHistory(true)
                .checkBankRecords(true)
                .checkDocumentVerification(true)
                .performDeepScreening(false)
                .build();
    }
    
    
    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.length() < 4) {
            return "****";
        }
        return "****" + identifier.substring(identifier.length() - 4);
    }
}
