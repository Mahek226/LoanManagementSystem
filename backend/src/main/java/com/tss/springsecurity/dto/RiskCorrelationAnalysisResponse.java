package com.tss.springsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskCorrelationAnalysisResponse {
    
    private Long loanId;
    private Long applicantId;
    private List<String> fraudTags;
    private boolean defaulterHistory;
    private List<String> transactionAnomalies;
    private int complianceRiskRating; // 1-5
    private List<RiskFactor> riskFactors;
    private String recommendation;
}
