package com.tss.springsecurity.fraud.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model class for individual fraud rule validation results
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FraudValidationResult {
    private String ruleCode;
    private String ruleName;
    private boolean triggered;
    private int points;
    private String severity;
    private String category;
    private String description;
    private String flagDetails;
    private String validationLogic;
    
    public static FraudValidationResult success(String ruleCode, String ruleName) {
        return new FraudValidationResult(ruleCode, ruleName, false, 0, "", "", "Validation passed", "", "");
    }
    
    public static FraudValidationResult failure(String ruleCode, String ruleName, int points, 
                                              String severity, String category, String flagDetails) {
        return new FraudValidationResult(ruleCode, ruleName, true, points, severity, category, 
                                       "Fraud rule triggered", flagDetails, "");
    }
}
