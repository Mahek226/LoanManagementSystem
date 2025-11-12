package com.tss.springsecurity.controller;

import com.tss.springsecurity.dto.*;
import com.tss.springsecurity.service.LoanOfficerScreeningService;
import com.tss.springsecurity.service.ComplianceOfficerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/compliance-officer")
public class ComplianceOfficerController {
    
    private final LoanOfficerScreeningService screeningService;
    private final ComplianceOfficerService complianceOfficerService;
    
    public ComplianceOfficerController(LoanOfficerScreeningService screeningService, 
                                     ComplianceOfficerService complianceOfficerService) {
        this.screeningService = screeningService;
        this.complianceOfficerService = complianceOfficerService;
    }
    
    @GetMapping("/escalations")
    public ResponseEntity<List<LoanScreeningResponse>> getComplianceEscalations() {
        List<LoanScreeningResponse> escalations = screeningService.getComplianceEscalations();
        return ResponseEntity.ok(escalations);
    }
    
    @PostMapping("/{complianceOfficerId}/process-decision")
    public ResponseEntity<?> processComplianceDecision(
            @PathVariable Long complianceOfficerId,
            @Valid @RequestBody LoanScreeningRequest request) {
        try {
            LoanScreeningResponse response = screeningService.processComplianceDecision(complianceOfficerId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @GetMapping("/assignment/{assignmentId}/details")
    public ResponseEntity<?> getComplianceAssignmentDetails(@PathVariable Long assignmentId) {
        try {
            LoanScreeningResponse response = complianceOfficerService.getLoanScreeningDetails(assignmentId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    // ==================== Enhanced Compliance Officer Endpoints ====================
    
    /**
     * Get detailed loan screening information for compliance review
     */
    @GetMapping("/screening/{assignmentId}")
    public ResponseEntity<?> getLoanScreeningDetails(@PathVariable Long assignmentId) {
        try {
            LoanScreeningResponse response = complianceOfficerService.getLoanScreeningDetails(assignmentId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Request document resubmission from applicant
     */
    @PostMapping("/request-documents")
    public ResponseEntity<?> requestDocumentResubmission(
            @Valid @RequestBody DocumentResubmissionRequest request) {
        try {
            DocumentResubmissionResponse response = complianceOfficerService.requestDocumentResubmission(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Get all document resubmission requests for an assignment
     */
    @GetMapping("/assignment/{assignmentId}/document-requests")
    public ResponseEntity<List<DocumentResubmissionResponse>> getDocumentResubmissionRequests(
            @PathVariable Long assignmentId) {
        List<DocumentResubmissionResponse> requests = complianceOfficerService.getDocumentResubmissionRequests(assignmentId);
        return ResponseEntity.ok(requests);
    }
    
    /**
     * Approve loan application
     */
    @PostMapping("/approve/{assignmentId}")
    public ResponseEntity<?> approveLoan(
            @PathVariable Long assignmentId,
            @RequestParam Long complianceOfficerId,
            @RequestBody(required = false) Map<String, String> requestBody) {
        try {
            String remarks = requestBody != null ? requestBody.get("remarks") : null;
            LoanScreeningResponse response = complianceOfficerService.approveLoan(assignmentId, complianceOfficerId, remarks);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Reject loan application
     */
    @PostMapping("/reject/{assignmentId}")
    public ResponseEntity<?> rejectLoan(
            @PathVariable Long assignmentId,
            @RequestParam Long complianceOfficerId,
            @Valid @RequestBody Map<String, String> requestBody) {
        try {
            String rejectionReason = requestBody.get("rejectionReason");
            String remarks = requestBody.get("remarks");
            
            if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("Rejection reason is required"));
            }
            
            LoanScreeningResponse response = complianceOfficerService.rejectLoan(assignmentId, complianceOfficerId, rejectionReason, remarks);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Process comprehensive compliance decision (approve/reject/request documents)
     */
    @PostMapping("/decision")
    public ResponseEntity<?> processComplianceDecision(
            @RequestParam Long complianceOfficerId,
            @Valid @RequestBody ComplianceDecisionRequest request) {
        try {
            LoanScreeningResponse response = complianceOfficerService.processComplianceDecision(complianceOfficerId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Get compliance officer dashboard with statistics
     */
    @GetMapping("/dashboard/{complianceOfficerId}")
    public ResponseEntity<?> getComplianceDashboard(@PathVariable Long complianceOfficerId) {
        try {
            Map<String, Object> dashboard = complianceOfficerService.getComplianceDashboard(complianceOfficerId);
            return ResponseEntity.ok(dashboard);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Get all pending document resubmission requests
     */
    @GetMapping("/document-requests/pending")
    public ResponseEntity<List<DocumentResubmissionResponse>> getPendingDocumentRequests() {
        List<DocumentResubmissionResponse> requests = complianceOfficerService.getPendingDocumentRequests();
        return ResponseEntity.ok(requests);
    }
    
    /**
     * Get compliance officer's processing history
     */
    @GetMapping("/history/{complianceOfficerId}")
    public ResponseEntity<List<LoanScreeningResponse>> getProcessingHistory(
            @PathVariable Long complianceOfficerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<LoanScreeningResponse> history = complianceOfficerService.getProcessingHistory(complianceOfficerId, page, size);
        return ResponseEntity.ok(history);
    }
    
    // ==================== KYC Verification ====================
    
    /**
     * Perform KYC verification (PAN/Aadhaar)
     */
    @PostMapping("/kyc-verification")
    public ResponseEntity<?> performKYCVerification(@Valid @RequestBody KYCVerificationRequest request) {
        try {
            KYCVerificationResponse response = complianceOfficerService.performKYCVerification(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    // ==================== AML & Sanctions Screening ====================
    
    /**
     * Perform AML screening (RBI, FATF, OFAC, Internal Blacklist)
     */
    @PostMapping("/aml-screening")
    public ResponseEntity<?> performAMLScreening(@Valid @RequestBody AMLScreeningRequest request) {
        try {
            AMLScreeningResponse response = complianceOfficerService.performAMLScreening(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Check RBI defaulters list
     */
    @GetMapping("/check-rbi-defaulters/{panNumber}")
    public ResponseEntity<?> checkRBIDefaulters(@PathVariable String panNumber) {
        try {
            Map<String, Object> result = complianceOfficerService.checkRBIDefaulters(panNumber);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Check sanctions list (FATF/OFAC)
     */
    @GetMapping("/check-sanctions")
    public ResponseEntity<?> checkSanctionsList(@RequestParam String name) {
        try {
            Map<String, Object> result = complianceOfficerService.checkSanctionsList(name);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Check internal blacklist
     */
    @GetMapping("/check-blacklist/{applicantId}")
    public ResponseEntity<?> checkInternalBlacklist(@PathVariable Long applicantId) {
        try {
            Map<String, Object> result = complianceOfficerService.checkInternalBlacklist(applicantId);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Check PEP (Politically Exposed Person) status
     */
    @GetMapping("/check-pep")
    public ResponseEntity<?> checkPEPStatus(
            @RequestParam String name,
            @RequestParam String pan) {
        try {
            Map<String, Object> result = complianceOfficerService.checkPEPStatus(name, pan);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    // ==================== Risk Correlation Analysis ====================
    
    /**
     * Get risk correlation analysis for a loan
     */
    @GetMapping("/loan/{loanId}/risk-correlation")
    public ResponseEntity<?> getRiskCorrelationAnalysis(@PathVariable Long loanId) {
        try {
            RiskCorrelationAnalysisResponse response = complianceOfficerService.getRiskCorrelationAnalysis(loanId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    // ==================== Audit Logs ====================
    
    /**
     * Get audit logs for a specific assignment
     */
    @GetMapping("/assignment/{assignmentId}/audit-logs")
    public ResponseEntity<?> getAuditLogs(@PathVariable Long assignmentId) {
        try {
            List<ComplianceAuditLogResponse> logs = complianceOfficerService.getAuditLogs(assignmentId);
            return ResponseEntity.ok(logs);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Get all audit logs for a compliance officer
     */
    @GetMapping("/{officerId}/audit-logs")
    public ResponseEntity<?> getAllAuditLogs(
            @PathVariable Long officerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            List<ComplianceAuditLogResponse> logs = complianceOfficerService.getAllAuditLogs(officerId, page, size);
            return ResponseEntity.ok(logs);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    // ==================== Document Management ====================
    
    /**
     * Get loan documents for compliance review
     */
    @GetMapping("/loan/{loanId}/documents")
    public ResponseEntity<?> getLoanDocuments(@PathVariable Long loanId) {
        try {
            List<DocumentResponse> documents = complianceOfficerService.getLoanDocuments(loanId);
            return ResponseEntity.ok(documents);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Get fraud history for an applicant
     */
    @GetMapping("/applicant/{applicantId}/fraud-history")
    public ResponseEntity<?> getFraudHistory(@PathVariable Long applicantId) {
        try {
            List<FraudHistoryResponse> history = complianceOfficerService.getFraudHistory(applicantId);
            return ResponseEntity.ok(history);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Get external fraud data for an applicant
     */
    @GetMapping("/applicant/{applicantId}/external-fraud-data")
    public ResponseEntity<?> getExternalFraudData(@PathVariable Long applicantId) {
        try {
            Map<String, Object> externalData = complianceOfficerService.getExternalFraudData(applicantId);
            return ResponseEntity.ok(externalData);
        } catch (Exception e) {
//            log.error("Error fetching external fraud data for applicant ID: {}", applicantId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch external fraud data: " + e.getMessage()));
        }
    }
    
    /**
     * Get external fraud data for a loan
     */
    @GetMapping("/loan/{loanId}/external-fraud-data")
    public ResponseEntity<?> getExternalFraudDataByLoan(@PathVariable Long loanId) {
        try {
            Map<String, Object> externalData = complianceOfficerService.getExternalFraudDataByLoanId(loanId);
            return ResponseEntity.ok(externalData);
        } catch (Exception e) {
//            log.error("Error fetching external fraud data for loan ID: {}", loanId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch external fraud data: " + e.getMessage()));
        }
    }
    
    /**
     * Get external person details by person ID from external database
     */
    @GetMapping("/external-person/{personId}")
    public ResponseEntity<?> getExternalPersonDetails(@PathVariable Long personId) {
        try {
            Map<String, Object> personDetails = complianceOfficerService.getExternalPersonDetails(personId);
            return ResponseEntity.ok(personDetails);
        } catch (Exception e) {
//            log.error("Error fetching external person details for person ID: {}", personId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch external person details: " + e.getMessage()));
        }
    }
    
    /**
     * Get enhanced screening details for compliance assignment
     */
    @GetMapping("/assignment/{assignmentId}/enhanced-screening")
    public ResponseEntity<?> getEnhancedScreeningDetails(@PathVariable Long assignmentId) {
        try {
            Map<String, Object> enhancedDetails = complianceOfficerService.getEnhancedScreeningDetails(assignmentId);
            return ResponseEntity.ok(enhancedDetails);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Request additional documents from applicant
     */
    @PostMapping("/{officerId}/request-documents")
    public ResponseEntity<?> requestAdditionalDocuments(
            @PathVariable Long officerId,
            @Valid @RequestBody AdditionalDocumentRequest request) {
        try {
            Map<String, Object> response = complianceOfficerService.requestAdditionalDocuments(officerId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    // ==================== Report Generation ====================
    
    /**
     * Generate compliance report PDF
     */
    @GetMapping("/assignment/{assignmentId}/generate-report")
    public ResponseEntity<?> generateComplianceReport(@PathVariable Long assignmentId) {
        try {
            byte[] pdfBytes = complianceOfficerService.generateComplianceReportPDF(assignmentId);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename=compliance-report-" + assignmentId + ".pdf")
                    .body(pdfBytes);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    // ==================== Comprehensive Compliance Review ====================
    
    /**
     * Get comprehensive review details including:
     * 1. Loan draft and documents (same as applicant sees)
     * 2. External fraud data (bank records, criminal records, loan history from external_lms)
     * 3. Screening risk level and mismatched rules
     * 4. Flags allocated
     */
    @GetMapping("/assignment/{assignmentId}/comprehensive-review")
    public ResponseEntity<?> getComprehensiveReviewDetails(@PathVariable Long assignmentId) {
        try {
            ComplianceReviewDetailsResponse response = complianceOfficerService.getComprehensiveReviewDetails(assignmentId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Submit compliance verdict to loan officer
     * Compliance officer CANNOT directly approve/reject - only provides recommendation to loan officer
     */
    @PostMapping("/submit-verdict")
    public ResponseEntity<?> submitComplianceVerdict(
            @Valid @RequestBody ComplianceVerdictRequest request) {
        try {
            ComplianceVerdictResponse response = complianceOfficerService.submitComplianceVerdict(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * Request document resubmission for a specific document
     * Request goes to loan officer first, who can then forward to applicant
     */
    @PostMapping("/document/{documentId}/request-resubmission")
    public ResponseEntity<?> requestDocumentResubmission(
            @PathVariable Long documentId,
            @Valid @RequestBody DocumentResubmissionRequestDTO request) {
        try {
            Map<String, Object> response = complianceOfficerService.requestDocumentResubmissionDetailed(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    // ==================== Document Resubmission Management ====================

    /**
     * Get document resubmission requests by status
     */
    @GetMapping("/document-resubmission-requests")
    public ResponseEntity<?> getDocumentResubmissionRequests(@RequestParam String status) {
        try {
            List<Map<String, Object>> requests = complianceOfficerService.getDocumentResubmissionRequestsByStatus(status);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to fetch document resubmission requests: " + e.getMessage()));
        }
    }

    /**
     * Get document resubmission requests by officer ID
     */
    @GetMapping("/{officerId}/document-resubmission-requests")
    public ResponseEntity<?> getDocumentResubmissionRequestsByOfficer(
            @PathVariable Long officerId,
            @RequestParam(required = false) String status) {
        try {
            List<Map<String, Object>> requests = complianceOfficerService.getDocumentResubmissionRequestsByOfficer(officerId, status);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to fetch document resubmission requests: " + e.getMessage()));
        }
    }
    
    /**
     * Get forwarded documents from loan officers
     */
    @GetMapping("/forwarded-documents")
    public ResponseEntity<?> getForwardedDocuments(@RequestParam(required = false) String status) {
        try {
            List<Map<String, Object>> forwardedDocs = complianceOfficerService.getForwardedDocuments(status);
            return ResponseEntity.ok(forwardedDocs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to fetch forwarded documents: " + e.getMessage()));
        }
    }
    
    // ==================== Response Classes ====================
    
    private record ErrorResponse(String message) {}
    
    private record SuccessResponse(String message) {}
}