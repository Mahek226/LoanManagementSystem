package com.tss.springsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KYCVerificationResponse {
    
    private boolean verified;
    private String panStatus;
    private String aadhaarStatus;
    private boolean nameMatch;
    private boolean addressMatch;
    private boolean duplicateFound;
    private String remarks;
}
