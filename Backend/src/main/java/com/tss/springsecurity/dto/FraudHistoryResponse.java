package com.tss.springsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudHistoryResponse {
    
    private Long recordId;
    private Long applicantId;
    private Long loanId;
    private String fraudType;
    private List<String> fraudTags;
    private String riskLevel;
    private int riskScore;
    private LocalDateTime detectedAt;
    private LocalDateTime resolvedAt;
    private String status; // DETECTED, RESOLVED, UNDER_INVESTIGATION, CLEARED
    private String remarks;
}
