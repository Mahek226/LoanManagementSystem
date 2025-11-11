package com.tss.springsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskFactor {
    
    private String category;
    private String description;
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    private double weight; // 0.0 - 1.0
}
