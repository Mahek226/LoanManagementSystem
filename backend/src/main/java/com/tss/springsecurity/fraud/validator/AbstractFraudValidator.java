package com.tss.springsecurity.fraud.validator;

import com.tss.springsecurity.entity.Applicant;
import com.tss.springsecurity.fraud.model.FraudValidationResult;
import com.tss.springsecurity.repository.ApplicantRepository;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract base class for fraud validators
 * Provides common functionality and data access
 */
public abstract class AbstractFraudValidator implements FraudValidator {
    
    @Autowired
    protected ApplicantRepository applicantRepository;
    
    /**
     * Get applicant by ID with error handling
     */
    protected Applicant getApplicant(Long applicantId) {
        return applicantRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("Applicant not found: " + applicantId));
    }
    
    /**
     * Create success result
     */
    protected FraudValidationResult createSuccessResult(String ruleCode, String ruleName) {
        return FraudValidationResult.success(ruleCode, ruleName);
    }
    
    /**
     * Create failure result
     */
    protected FraudValidationResult createFailureResult(String ruleCode, String ruleName, 
                                                       int points, String severity, 
                                                       String category, String flagDetails) {
        return FraudValidationResult.failure(ruleCode, ruleName, points, severity, category, flagDetails);
    }
    
    /**
     * Check if two strings match (case-insensitive, null-safe)
     */
    protected boolean stringsMatch(String str1, String str2) {
        if (str1 == null && str2 == null) return true;
        if (str1 == null || str2 == null) return false;
        return str1.trim().equalsIgnoreCase(str2.trim());
    }
    
    /**
     * Check if string is null or empty
     */
    protected boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}
