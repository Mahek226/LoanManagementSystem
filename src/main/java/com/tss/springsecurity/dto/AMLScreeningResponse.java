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
public class AMLScreeningResponse {
    
    private Long applicantId;
    private LocalDateTime screeningDate;
    private String overallRisk; // CLEAR, LOW, MEDIUM, HIGH, CRITICAL
    private List<AMLFinding> findings;
    private boolean isPEP; // Politically Exposed Person
    private List<String> recommendations;
}
