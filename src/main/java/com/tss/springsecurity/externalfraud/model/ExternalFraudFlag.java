package com.tss.springsecurity.externalfraud.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalFraudFlag {
    
    private String ruleCode;
    private String ruleName;
    private String category; // CRIMINAL, LOAN_HISTORY, BANK_RECORDS, DOCUMENT_VERIFICATION
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    private int points;
    private String description;
    private String details;
    private boolean triggered;
    
    public static ExternalFraudFlag create(String ruleCode, String ruleName, String category, 
                                         String severity, int points, String description, String details) {
        return ExternalFraudFlag.builder()
                .ruleCode(ruleCode)
                .ruleName(ruleName)
                .category(category)
                .severity(severity)
                .points(points)
                .description(description)
                .details(details)
                .triggered(true)
                .build();
    }
}
