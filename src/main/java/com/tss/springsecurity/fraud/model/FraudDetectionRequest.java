package com.tss.springsecurity.fraud.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Model class for fraud detection requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FraudDetectionRequest {
    private Long applicantId;
    private List<String> categories; // IDENTITY, FINANCIAL, EMPLOYMENT, CROSS_VERIFICATION
    private boolean includeScoreBreakdown = true;
    private boolean includeExplanation = true;
    private String requestedBy;
    private String reason;
}
