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
    
    // Frontend expected fields
    private Long id;
    private Long applicantId;
    private String applicantName;
    private String loanType;
    private BigDecimal requestedAmount;
    private String purpose;
    private String employmentStatus;
    private Double monthlyIncome;
    private String status;
    private String appliedDate;
    private String reviewedDate;
    private String reviewedBy;
    private String comments;
    
    // Additional fields
    private String email;
    private String phone;
    private Integer loanTenure;
    private BigDecimal emiAmount;
    
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
