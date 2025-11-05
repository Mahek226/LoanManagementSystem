package com.tss.springsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudFlagResponse {
    private Long id;
    private String ruleName;
    private Integer severity;
    private String flagNotes;
    private LocalDateTime createdAt;
    private Long applicantId;
    private Long loanId;
    private String severityLevel;
    
    public String getSeverityLevel() {
        if (severity == null) return "UNKNOWN";
        if (severity >= 4) return "CRITICAL";
        if (severity >= 3) return "HIGH";
        if (severity >= 2) return "MEDIUM";
        return "LOW";
    }
}
