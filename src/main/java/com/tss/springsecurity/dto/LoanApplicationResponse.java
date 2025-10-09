package com.tss.springsecurity.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoanApplicationResponse {
    
    private Long applicationId;
    private String applicantName;
    private String email;
    private String phone;
    private String loanType;
    private BigDecimal loanAmount;
    private Integer loanTenure;
    private BigDecimal emiAmount;
    private String applicationStatus;
    private LocalDateTime applicationDate;
    private LocalDateTime lastUpdated;
    private String remarks;
    
    // Document information
    private List<DocumentInfo> documents;
    
    // Nested class for document information
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DocumentInfo {
        private String documentType;
        private String documentName;
        private String documentUrl;
        private String verificationStatus;
        private LocalDateTime uploadedAt;
    }
}
