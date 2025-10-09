package com.tss.springsecurity.fraud;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FraudRule {
    private String ruleName;
    private String ruleDescription;
    private int fraudPoints;
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    private String category; // IDENTITY, FINANCIAL, EMPLOYMENT, etc.
    private boolean isFlagged;
    private String flagDetails;
}
