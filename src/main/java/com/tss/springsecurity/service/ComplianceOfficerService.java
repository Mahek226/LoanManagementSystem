package com.tss.springsecurity.service;

import com.tss.springsecurity.dto.*;

import java.util.List;
import java.util.Map;

public interface ComplianceOfficerService {
    
    /**
     * Get detailed loan screening information for compliance review
     */
    LoanScreeningResponse getLoanScreeningDetails(Long assignmentId);
    
    /**
     * Request document resubmission from applicant
     */
    DocumentResubmissionResponse requestDocumentResubmission(DocumentResubmissionRequest request);
    
    /**
     * Get all document resubmission requests for an assignment
     */
    List<DocumentResubmissionResponse> getDocumentResubmissionRequests(Long assignmentId);
    
    /**
     * Approve loan application
     */
    LoanScreeningResponse approveLoan(Long assignmentId, Long complianceOfficerId, String remarks);
    
    /**
     * Reject loan application
     */
    LoanScreeningResponse rejectLoan(Long assignmentId, Long complianceOfficerId, String rejectionReason, String remarks);
    
    /**
     * Process comprehensive compliance decision
     */
    LoanScreeningResponse processComplianceDecision(Long complianceOfficerId, ComplianceDecisionRequest request);
    
    /**
     * Get compliance officer dashboard with statistics
     */
    Map<String, Object> getComplianceDashboard(Long complianceOfficerId);
    
    /**
     * Get all pending document resubmission requests
     */
    List<DocumentResubmissionResponse> getPendingDocumentRequests();
    
    /**
     * Get compliance officer's processing history
     */
    List<LoanScreeningResponse> getProcessingHistory(Long complianceOfficerId, int page, int size);
}
