package com.tss.springsecurity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceVerdictRequest {
    
    @NotNull(message = "Assignment ID is required")
    private Long assignmentId;
    
    @NotNull(message = "Compliance Officer ID is required")
    private Long complianceOfficerId;
    
    @NotNull(message = "Verdict is required")
    private VerdictType verdict; // RECOMMEND_APPROVE, RECOMMEND_REJECT, REQUEST_MORE_INFO
    
    @NotBlank(message = "Verdict reason is required")
    private String verdictReason;
    
    private String detailedRemarks;
    
    // For RECOMMEND_REJECT verdict
    private List<String> rejectionReasons;
    
    // For REQUEST_MORE_INFO verdict
    private List<DocumentResubmissionInfo> documentsToResubmit;
    
    private List<String> additionalChecksRequired;
    
    // Target officer ID (optional - if not provided, goes back to originally assigned loan officer)
    private Long targetLoanOfficerId;
    
    public enum VerdictType {
        RECOMMEND_APPROVE,
        RECOMMEND_REJECT,
        REQUEST_MORE_INFO
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentResubmissionInfo {
        @NotNull
        private Long documentId;
        
        @NotBlank
        private String documentType;
        
        @NotBlank
        private String resubmissionReason;
        
        private String specificInstructions;
    }
}
