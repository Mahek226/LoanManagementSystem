package com.tss.springsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AMLFinding {
    
    private String source; // RBI_DEFAULTERS, FATF, OFAC, etc.
    private String matchType; // EXACT, PARTIAL, NONE
    private int matchScore; // 0-100
    private String details;
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
}
