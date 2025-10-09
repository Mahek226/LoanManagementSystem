package com.tss.springsecurity.fraud.validator.identity;

import com.tss.springsecurity.entity.Applicant;
import com.tss.springsecurity.entity.FraudRuleDefinition;
import com.tss.springsecurity.fraud.model.FraudValidationResult;
import com.tss.springsecurity.fraud.validator.AbstractFraudValidator;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Validator for duplicate Aadhaar numbers
 * Checks if the same Aadhaar is used by multiple applicants
 */
@Component
public class DuplicateAadhaarValidator extends AbstractFraudValidator {
    
    @Override
    public FraudValidationResult validate(FraudRuleDefinition ruleDefinition, Long applicantId) {
        Applicant applicant = getApplicant(applicantId);
        
        // Get Aadhaar number from AadhaarDetails relationship
        String aadhaarNumber = getAadhaarNumber(applicant);
        if (isEmpty(aadhaarNumber)) {
            return createSuccessResult(ruleDefinition.getRuleCode(), ruleDefinition.getRuleName());
        }
        
        // Check for duplicate Aadhaar numbers
        List<Applicant> duplicates = applicantRepository.findAll().stream()
            .filter(a -> {
                String otherAadhaar = getAadhaarNumber(a);
                return otherAadhaar != null && 
                       otherAadhaar.equals(aadhaarNumber) && 
                       !a.getApplicantId().equals(applicantId);
            })
            .collect(Collectors.toList());
        
        if (!duplicates.isEmpty()) {
            String flagDetails = String.format(
                "Aadhaar number %s is already used by %d other applicant(s): %s",
                aadhaarNumber,
                duplicates.size(),
                duplicates.stream()
                    .map(a -> a.getFirstName() + " " + (a.getLastName() != null ? a.getLastName() : "") + " (ID: " + a.getApplicantId() + ")")
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("")
            );
            
            return createFailureResult(
                ruleDefinition.getRuleCode(),
                ruleDefinition.getRuleName(),
                ruleDefinition.getFraudPoints(),
                ruleDefinition.getSeverity(),
                ruleDefinition.getRuleCategory(),
                flagDetails
            );
        }
        
        return createSuccessResult(ruleDefinition.getRuleCode(), ruleDefinition.getRuleName());
    }
    
    /**
     * Extract Aadhaar number from applicant's AadhaarDetails
     */
    private String getAadhaarNumber(Applicant applicant) {
        try {
            if (applicant.getAadhaarDetails() == null || applicant.getAadhaarDetails().isEmpty()) {
                return null;
            }
            
            // Get the first Aadhaar number (assuming one per applicant)
            return applicant.getAadhaarDetails().get(0).getAadhaarNumber();
        } catch (Exception e) {
            // If there's any issue with lazy loading, return null
            return null;
        }
    }
    
    @Override
    public String getCategory() {
        return "IDENTITY";
    }
    
    @Override
    public String[] getSupportedRuleTypes() {
        return new String[]{"DUPLICATE_CHECK"};
    }
}
