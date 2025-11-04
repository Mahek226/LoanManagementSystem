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
    
    // ==================== KYC Verification ====================
    
    /**
     * Perform KYC verification (PAN/Aadhaar)
     */
    KYCVerificationResponse performKYCVerification(KYCVerificationRequest request);
    
    // ==================== AML & Sanctions Screening ====================
    
    /**
     * Perform AML screening
     */
    AMLScreeningResponse performAMLScreening(AMLScreeningRequest request);
    
    /**
     * Check RBI defaulters list
     */
    Map<String, Object> checkRBIDefaulters(String panNumber);
    
    /**
     * Check sanctions list (FATF/OFAC)
     */
    Map<String, Object> checkSanctionsList(String name);
    
    /**
     * Check internal blacklist
     */
    Map<String, Object> checkInternalBlacklist(Long applicantId);
    
    /**
     * Check PEP status
     */
    Map<String, Object> checkPEPStatus(String name, String pan);
    
    // ==================== Risk Correlation Analysis ====================
    
    /**
     * Get risk correlation analysis
     */
    RiskCorrelationAnalysisResponse getRiskCorrelationAnalysis(Long loanId);
    
    // ==================== Audit Logs ====================
    
    /**
     * Get audit logs for assignment
     */
    List<ComplianceAuditLogResponse> getAuditLogs(Long assignmentId);
    
    /**
     * Get all audit logs for officer
     */
    List<ComplianceAuditLogResponse> getAllAuditLogs(Long officerId, int page, int size);
    
    // ==================== Document Management ====================
    
    /**
     * Get loan documents
     */
    List<DocumentResponse> getLoanDocuments(Long loanId);
    
    /**
     * Get fraud history
     */
    List<FraudHistoryResponse> getFraudHistory(Long applicantId);
    
    /**
     * Get external fraud data for an applicant
     */
    Map<String, Object> getExternalFraudData(Long applicantId);
    
    /**
     * Get enhanced screening details for compliance assignment
     */
    Map<String, Object> getEnhancedScreeningDetails(Long assignmentId);
    
    /**
     * Request additional documents
     */
    Map<String, Object> requestAdditionalDocuments(Long officerId, AdditionalDocumentRequest request);
    
    // ==================== Report Generation ====================
    
    /**
     * Generate compliance report PDF
     */
    byte[] generateComplianceReportPDF(Long assignmentId);
    
    // ==================== Comprehensive Compliance Review ====================
    
    /**
     * Get comprehensive review details including documents, external fraud data, and screening results
     */
    ComplianceReviewDetailsResponse getComprehensiveReviewDetails(Long assignmentId);
    
    /**
     * Submit compliance verdict to loan officer
     */
    ComplianceVerdictResponse submitComplianceVerdict(ComplianceVerdictRequest request);
    
    /**
     * Request document resubmission with detailed reasons
     */
    Map<String, Object> requestDocumentResubmissionDetailed(DocumentResubmissionRequestDTO request);
    
<<<<<<< HEAD
    // ==================== External Fraud Data ====================
    
    /**
     * Get external fraud data for an applicant from external_lms database
     */
    Map<String, Object> getExternalFraudData(Long applicantId);
    
    /**
     * Get bank records for an applicant from external database
     */
    List<Map<String, Object>> getBankRecords(Long applicantId);
    
    /**
     * Get criminal records for an applicant from external database
     */
    List<Map<String, Object>> getCriminalRecords(Long applicantId);
    
    /**
     * Get historical and current loans for an applicant from external database
     */
    List<Map<String, Object>> getLoanHistory(Long applicantId);
=======
    /**
     * Get external person details by person ID from external database
     */
    Map<String, Object> getExternalPersonDetails(Long personId);
>>>>>>> fbd8d4982247036d3e587f42bd5f81bc6ccc9259
}
