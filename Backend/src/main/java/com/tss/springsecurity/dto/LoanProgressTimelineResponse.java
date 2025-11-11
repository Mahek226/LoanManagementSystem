package com.tss.springsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoanProgressTimelineResponse {
    
    private Long loanId;
    private Long applicantId;
    private String applicantName;
    private String loanType;
    private BigDecimal loanAmount;
    private String currentStatus;
    private String appliedAt;
    private List<LoanProgressEvent> events;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LoanProgressEvent {
        private Integer eventId;
        private String eventType; // APPLICATION_SUBMITTED, ASSIGNED_TO_OFFICER, OFFICER_REVIEW, ESCALATED, COMPLIANCE_REVIEW, APPROVED, REJECTED
        private String eventStatus;
        private String performedBy;
        private String performedByRole; // APPLICANT, LOAN_OFFICER, COMPLIANCE_OFFICER, SYSTEM
        private Long officerId;
        private String officerName;
        private String action;
        private String remarks;
        private String timestamp;
    }
}
