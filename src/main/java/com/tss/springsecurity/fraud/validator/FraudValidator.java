package com.tss.springsecurity.fraud.validator;

import com.tss.springsecurity.entity.FraudRuleDefinition;
import com.tss.springsecurity.fraud.model.FraudValidationResult;

/**
 * Base interface for all fraud validators
 * Each validator handles specific fraud detection logic
 */
public interface FraudValidator {
    
    /**
     * Validate a specific fraud rule
     * @param ruleDefinition The rule to validate
     * @param applicantId The applicant to check
     * @return Validation result with details
     */
    FraudValidationResult validate(FraudRuleDefinition ruleDefinition, Long applicantId);
    
    /**
     * Get the category this validator handles
     */
    String getCategory();
    
    /**
     * Get supported rule types
     */
    String[] getSupportedRuleTypes();
    
    /**
     * Check if this validator can handle the given rule
     */
    default boolean canHandle(FraudRuleDefinition rule) {
        if (!getCategory().equals(rule.getRuleCategory())) {
            return false;
        }
        
        String[] supportedTypes = getSupportedRuleTypes();
        if (supportedTypes == null || supportedTypes.length == 0) {
            return true;
        }
        
        for (String type : supportedTypes) {
            if (type.equals(rule.getRuleType())) {
                return true;
            }
        }
        return false;
    }
}
