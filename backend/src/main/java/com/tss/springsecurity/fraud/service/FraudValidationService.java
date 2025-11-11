package com.tss.springsecurity.fraud.service;

import com.tss.springsecurity.entity.FraudRuleDefinition;
import com.tss.springsecurity.fraud.FraudDetectionResult;
import com.tss.springsecurity.fraud.FraudRule;
import com.tss.springsecurity.fraud.model.FraudValidationResult;
import com.tss.springsecurity.fraud.validator.FraudValidator;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for orchestrating fraud validation
 * Coordinates between validators and rule service
 */
@Service
public class FraudValidationService {
    
    private final FraudRuleService fraudRuleService;
    private final Map<String, List<FraudValidator>> validatorsByCategory;
    
    public FraudValidationService(FraudRuleService fraudRuleService, List<FraudValidator> validators) {
        this.fraudRuleService = fraudRuleService;
        this.validatorsByCategory = validators.stream()
                .collect(Collectors.groupingBy(FraudValidator::getCategory));
    }
    
    /**
     * Run fraud validation for a specific category
     */
    public FraudDetectionResult validateCategory(String category, Long applicantId) {
        FraudDetectionResult result = new FraudDetectionResult();
        result.setApplicantId(applicantId);
        
        // Set applicant name (you may need to inject ApplicantRepository for this)
        result.setApplicantName("Applicant " + applicantId);
        
        // Get rules for category
        List<FraudRuleDefinition> rules = fraudRuleService.getActiveRulesByCategory(category);
        
        // Get validators for category
        List<FraudValidator> validators = validatorsByCategory.get(category);
        if (validators == null || validators.isEmpty()) {
            throw new RuntimeException("No validators found for category: " + category);
        }
        
        // Validate each rule
        for (FraudRuleDefinition rule : rules) {
            FraudValidator validator = findValidatorForRule(validators, rule);
            if (validator != null) {
                FraudValidationResult validationResult = validator.validate(rule, applicantId);
                
                if (validationResult.isTriggered()) {
                    FraudRule fraudRule = fraudRuleService.createFraudRule(rule, validationResult.getFlagDetails());
                    result.addTriggeredRule(fraudRule);
                }
            }
        }
        
        result.calculateRiskLevel();
        return result;
    }
    
    /**
     * Run fraud validation for all categories
     */
    public FraudDetectionResult validateAll(Long applicantId) {
        FraudDetectionResult combinedResult = new FraudDetectionResult();
        combinedResult.setApplicantId(applicantId);
        combinedResult.setApplicantName("Applicant " + applicantId);
        
        // Validate each category
        for (String category : validatorsByCategory.keySet()) {
            FraudDetectionResult categoryResult = validateCategory(category, applicantId);
            
            // Merge results
            for (FraudRule rule : categoryResult.getTriggeredRules()) {
                combinedResult.addTriggeredRule(rule);
            }
        }
        
        combinedResult.calculateRiskLevel();
        return combinedResult;
    }
    
    /**
     * Find appropriate validator for a rule
     */
    private FraudValidator findValidatorForRule(List<FraudValidator> validators, FraudRuleDefinition rule) {
        return validators.stream()
                .filter(validator -> validator.canHandle(rule))
                .findFirst()
                .orElse(null);
    }
}
