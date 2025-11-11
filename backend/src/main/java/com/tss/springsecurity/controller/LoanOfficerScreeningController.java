package com.tss.springsecurity.controller;

import com.tss.springsecurity.dto.LoanScreeningRequest;
import com.tss.springsecurity.dto.LoanScreeningResponse;
import com.tss.springsecurity.dto.LoanScreeningDecision;
import com.tss.springsecurity.dto.ScreeningDashboardResponse;
import com.tss.springsecurity.service.LoanOfficerScreeningService;
import com.tss.springsecurity.payload.response.MessageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loan-officer")
@RequiredArgsConstructor
@Slf4j
public class LoanOfficerScreeningController {
    
    private final LoanOfficerScreeningService screeningService;
    private final com.tss.springsecurity.service.DocumentExtractionService documentExtractionService;
    private final com.tss.springsecurity.service.DocumentUploadService documentUploadService;
    private final com.tss.springsecurity.service.ApplicantNotificationService notificationService;
    private final com.tss.springsecurity.service.ComprehensiveLoanViewService comprehensiveLoanViewService;
    private final com.tss.springsecurity.service.ComprehensiveDashboardService comprehensiveDashboardService;
    private final com.tss.springsecurity.service.LoanOfficerDocumentService loanOfficerDocumentService;
    private final com.tss.springsecurity.repository.ApplicantRepository applicantRepository;
    private final com.tss.springsecurity.repository.ApplicantLoanDetailsRepository loanRepository;
    private final com.tss.springsecurity.repository.ApplicantBasicDetailsRepository basicDetailsRepository;
    private final com.tss.springsecurity.repository.OfficerApplicationAssignmentRepository assignmentRepository;
    private final com.tss.springsecurity.repository.DocumentResubmissionRepository documentResubmissionRepository;
    
    @GetMapping("/{officerId}/assigned-loans")
    public ResponseEntity<List<LoanScreeningResponse>> getAssignedLoans(@PathVariable Long officerId) {
        List<LoanScreeningResponse> loans = screeningService.getAssignedLoansForOfficer(officerId);
        return ResponseEntity.ok(loans);
    }
    
    /**
     * Get loans that have been escalated to compliance and now have verdicts available
     * These loans need final action from the loan officer
     */
    @GetMapping("/{officerId}/loans-with-compliance-verdicts")
    public ResponseEntity<List<LoanScreeningResponse>> getLoansWithComplianceVerdicts(@PathVariable Long officerId) {
        try {
            List<LoanScreeningResponse> loans = screeningService.getAssignedLoansForOfficer(officerId);
            
            // Filter to only show loans that are escalated and have compliance verdicts
            List<LoanScreeningResponse> loansWithVerdicts = loans.stream()
                    .filter(loan -> "ESCALATED_TO_COMPLIANCE".equals(loan.getStatus()) && 
                                   Boolean.TRUE.equals(loan.getHasComplianceVerdict()))
                    .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(loansWithVerdicts);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new java.util.ArrayList<>());
        }
    }
    
    /**
     * Get comprehensive dashboard with all KPIs, metrics, and visualizations
     * Supports filtering by time period
     */
    @GetMapping("/{officerId}/comprehensive-dashboard")
    public ResponseEntity<?> getComprehensiveDashboard(
            @PathVariable Long officerId,
            @RequestParam(required = false, defaultValue = "MONTH") String filterType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            java.time.LocalDate start = startDate != null ? java.time.LocalDate.parse(startDate) : null;
            java.time.LocalDate end = endDate != null ? java.time.LocalDate.parse(endDate) : null;
            
            com.tss.springsecurity.dto.ComprehensiveDashboardDTO dashboard = 
                    comprehensiveDashboardService.getComprehensiveDashboard(officerId, filterType, start, end);
            
            return ResponseEntity.ok(dashboard);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error generating dashboard: " + e.getMessage()));
        }
    }
    
    @GetMapping("/assignment/{assignmentId}/details")
    public ResponseEntity<?> getLoanDetailsForScreening(@PathVariable Long assignmentId) {
        try {
            LoanScreeningResponse response = screeningService.getLoanDetailsForScreening(assignmentId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse(e.getMessage()));
        }
    }
    
    @PostMapping("/{officerId}/process-screening")
    public ResponseEntity<?> processLoanScreening(
            @PathVariable Long officerId,
            @Valid @RequestBody LoanScreeningRequest request) {
        try {
            LoanScreeningResponse response = screeningService.processLoanScreening(officerId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse(e.getMessage()));
        }
    }
    
    @PostMapping("/assignment/{assignmentId}/escalate")
    public ResponseEntity<?> escalateToCompliance(
            @PathVariable Long assignmentId,
            @RequestParam(required = false) String remarks) {
        try {
            LoanScreeningResponse response = screeningService.escalateToCompliance(assignmentId, remarks);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse(e.getMessage()));
        }
    }
    
    @GetMapping("/{officerId}/screening-dashboard")
    public ResponseEntity<?> getScreeningDashboard(@PathVariable Long officerId) {
        try {
            ScreeningDashboardResponse dashboard = screeningService.getScreeningDashboard(officerId);
            return ResponseEntity.ok(dashboard);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse(e.getMessage()));
        }
    }
    
    @PostMapping("/{officerId}/screen-loan/{assignmentId}")
    public ResponseEntity<?> screenAssignedLoan(
            @PathVariable Long officerId,
            @PathVariable Long assignmentId,
            @Valid @RequestBody LoanScreeningDecision decision) {
        try {
            LoanScreeningResponse response = screeningService.screenAssignedLoan(officerId, assignmentId, decision);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse(e.getMessage()));
        }
    }
    
    @GetMapping("/{officerId}/screening-history")
    public ResponseEntity<?> getScreeningHistory(
            @PathVariable Long officerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            List<LoanScreeningResponse> history = screeningService.getScreeningHistory(officerId, page, size);
            return ResponseEntity.ok(history);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse(e.getMessage()));
        }
    }
    
    /**
     * Extract documents for a loan application
     * This is triggered by Loan Officer during review
     */
    @PostMapping("/assignment/{assignmentId}/extract-documents")
    public ResponseEntity<?> extractDocumentsForLoan(@PathVariable Long assignmentId) {
        try {
            System.out.println("Starting document extraction for assignment ID: " + assignmentId);
            
            // Get loan details to find the specific loan ID
            LoanScreeningResponse loanDetails = screeningService.getLoanDetailsForScreening(assignmentId);
            Long loanId = loanDetails.getLoanId();
            Long applicantId = loanDetails.getApplicantId();
            
            System.out.println("Found loan ID: " + loanId + " and applicant ID: " + applicantId + " for assignment: " + assignmentId);
            
            // Get only documents for this specific loan
            List<com.tss.springsecurity.entity.UploadedDocument> documents = 
                loanOfficerDocumentService.getDocumentsByLoanId(loanId);
            
            System.out.println("Found " + documents.size() + " documents for loan ID: " + loanId);
            
            if (documents.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new MessageResponse("No documents found for this loan"));
            }
            
            // Extract each document by downloading from Cloudinary
            java.util.Map<String, Object> extractionResults = new java.util.HashMap<>();
            int successCount = 0;
            int failureCount = 0;
            
            for (com.tss.springsecurity.entity.UploadedDocument doc : documents) {
                try {
                    System.out.println("Extracting document: " + doc.getDocumentType() + " - " + doc.getDocumentName());
                    
                    // Download document from Cloudinary URL
                    java.net.URL url = new java.net.URL(doc.getCloudinaryUrl());
                    java.io.InputStream inputStream = url.openStream();
                    byte[] fileBytes = inputStream.readAllBytes();
                    inputStream.close();
                    
                    System.out.println("Downloaded " + fileBytes.length + " bytes from Cloudinary");
                    
                    // Create MultipartFile from downloaded bytes
                    String mimeType = doc.getMimeType() != null ? doc.getMimeType() : "application/pdf";
                    String filename = doc.getOriginalFilename() != null ? doc.getOriginalFilename() : doc.getDocumentName();
                    
                    // Create a custom MultipartFile implementation
                    org.springframework.web.multipart.MultipartFile multipartFile = new org.springframework.web.multipart.MultipartFile() {
                        @Override
                        public String getName() { return filename; }
                        
                        @Override
                        public String getOriginalFilename() { return filename; }
                        
                        @Override
                        public String getContentType() { return mimeType; }
                        
                        @Override
                        public boolean isEmpty() { return fileBytes.length == 0; }
                        
                        @Override
                        public long getSize() { return fileBytes.length; }
                        
                        @Override
                        public byte[] getBytes() { return fileBytes; }
                        
                        @Override
                        public java.io.InputStream getInputStream() {
                            return new java.io.ByteArrayInputStream(fileBytes);
                        }
                        
                        @Override
                        public void transferTo(java.io.File dest) throws java.io.IOException {
                            java.nio.file.Files.write(dest.toPath(), fileBytes);
                        }
                    };
                    
                    // Extract document data
                    java.util.Map<String, Object> result = documentExtractionService.extractDocumentData(
                        multipartFile, 
                        doc.getDocumentType(), 
                        applicantId
                    );
                    
                    System.out.println("Extraction successful for: " + doc.getDocumentType());
                    extractionResults.put(doc.getDocumentType(), result);
                    successCount++;
                    
                } catch (Exception e) {
                    System.err.println("Extraction failed for " + doc.getDocumentType() + ": " + e.getMessage());
                    e.printStackTrace();
                    extractionResults.put(doc.getDocumentType(), 
                        java.util.Map.of("error", e.getMessage(), "success", false));
                    failureCount++;
                }
            }
            
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("message", "Document extraction completed");
            response.put("totalDocuments", documents.size());
            response.put("successCount", successCount);
            response.put("failureCount", failureCount);
            response.put("extractionResults", extractionResults);
            
            System.out.println("Document extraction completed. Success: " + successCount + ", Failed: " + failureCount);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            System.err.println("RuntimeException in document extraction: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            System.err.println("Exception in document extraction: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Failed to extract documents: " + e.getMessage()));
        }
    }
    
    /**
     * Get documents for a specific loan
     */
    @GetMapping("/loan/{loanId}/documents")
    public ResponseEntity<?> getLoanDocuments(@PathVariable Long loanId) {
        try {
            System.out.println("Fetching documents for loan ID: " + loanId);
            
            // Only get documents specifically associated with this loan
            List<com.tss.springsecurity.entity.UploadedDocument> documents = 
                loanOfficerDocumentService.getDocumentsByLoanId(loanId);
            
            System.out.println("Found " + documents.size() + " documents for loan ID: " + loanId);
            
            List<java.util.Map<String, Object>> documentDetails = new java.util.ArrayList<>();
            for (com.tss.springsecurity.entity.UploadedDocument doc : documents) {
                java.util.Map<String, Object> docInfo = new java.util.HashMap<>();
                docInfo.put("documentId", doc.getDocumentId());
                docInfo.put("documentType", doc.getDocumentType());
                docInfo.put("documentName", doc.getDocumentName());
                docInfo.put("documentUrl", doc.getCloudinaryUrl());
                docInfo.put("uploadedAt", doc.getUploadedAt());
                docInfo.put("verificationStatus", doc.getVerificationStatus() != null ? doc.getVerificationStatus().toString() : "PENDING");
                docInfo.put("verifiedBy", doc.getVerifiedBy());
                docInfo.put("verifiedAt", doc.getVerifiedAt());
                docInfo.put("remarks", doc.getVerificationNotes());
                documentDetails.add(docInfo);
            }
            
            System.out.println("Returning " + documentDetails.size() + " document details");
            return ResponseEntity.ok(documentDetails);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Failed to retrieve documents: " + e.getMessage()));
        }
    }
    
    /**
     * Verify a document
     */
    @PostMapping("/{officerId}/verify-document")
    public ResponseEntity<?> verifyDocument(
            @PathVariable Long officerId,
            @RequestBody java.util.Map<String, Object> request) {
        try {
            Long documentId = Long.valueOf(request.get("documentId").toString());
            String verificationStatus = request.get("verificationStatus").toString();
            String remarks = request.get("remarks") != null ? request.get("remarks").toString() : "";
            
            // Get officer name (you may need to fetch from database)
            String verifiedBy = "Officer #" + officerId;
            
            com.tss.springsecurity.entity.UploadedDocument updatedDoc = 
                documentUploadService.updateVerificationStatus(documentId, verificationStatus, remarks, verifiedBy);
            
            java.util.Map<String, Object> docInfo = new java.util.HashMap<>();
            docInfo.put("documentId", updatedDoc.getDocumentId());
            docInfo.put("documentType", updatedDoc.getDocumentType());
            docInfo.put("documentName", updatedDoc.getDocumentName());
            docInfo.put("documentUrl", updatedDoc.getCloudinaryUrl());
            docInfo.put("uploadedAt", updatedDoc.getUploadedAt());
            docInfo.put("verificationStatus", updatedDoc.getVerificationStatus() != null ? updatedDoc.getVerificationStatus().toString() : "PENDING");
            docInfo.put("verifiedBy", updatedDoc.getVerifiedBy());
            docInfo.put("verifiedAt", updatedDoc.getVerifiedAt());
            docInfo.put("remarks", updatedDoc.getVerificationNotes());
            
            return ResponseEntity.ok(docInfo);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Failed to verify document: " + e.getMessage()));
        }
    }
    
    /**
     * Request document resubmission from applicant
     */
    @PostMapping("/{officerId}/request-resubmission")
    public ResponseEntity<?> requestDocumentResubmission(
            @PathVariable Long officerId,
            @RequestBody java.util.Map<String, Object> request) {
        try {
            Long loanId = Long.valueOf(request.get("loanId").toString());
            Long applicantId = Long.valueOf(request.get("applicantId").toString());
            Long assignmentId = request.get("assignmentId") != null ? 
                Long.valueOf(request.get("assignmentId").toString()) : null;
            @SuppressWarnings("unchecked")
            List<String> documentTypes = (List<String>) request.get("documentTypes");
            String reason = request.get("reason").toString();
            String remarks = request.get("remarks") != null ? request.get("remarks").toString() : "";
            
            // Get officer name
            String requestedBy = "Loan Officer #" + officerId;
            
            // Create notification for applicant
            notificationService.createDocumentRequest(
                applicantId, 
                loanId, 
                assignmentId, 
                documentTypes, 
                reason, 
                requestedBy
            );
            
            System.out.println("Document resubmission requested for Loan #" + loanId);
            System.out.println("Applicant ID: " + applicantId);
            System.out.println("Document Types: " + documentTypes);
            System.out.println("Reason: " + reason);
            System.out.println("Notification created successfully");
            
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("message", "Document resubmission request sent to applicant");
            response.put("loanId", loanId);
            response.put("applicantId", applicantId);
            response.put("documentTypes", documentTypes);
            response.put("reason", reason);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Failed to request document resubmission: " + e.getMessage()));
        }
    }
    
    /**
     * Request more information from applicant
     */
    @PostMapping("/{officerId}/request-more-info")
    public ResponseEntity<?> requestMoreInfo(
            @PathVariable Long officerId,
            @RequestBody java.util.Map<String, Object> request) {
        try {
            Long loanId = Long.valueOf(request.get("loanId").toString());
            Long applicantId = Long.valueOf(request.get("applicantId").toString());
            Long assignmentId = request.get("assignmentId") != null ? 
                Long.valueOf(request.get("assignmentId").toString()) : null;
            @SuppressWarnings("unchecked")
            List<String> infoItems = (List<String>) request.get("infoItems");
            String reason = request.get("reason").toString();
            String remarks = request.get("remarks") != null ? request.get("remarks").toString() : "";
            
            // Get officer name
            String requestedBy = "Loan Officer #" + officerId;
            
            // Create notification for applicant
            notificationService.createInfoRequest(
                applicantId, 
                loanId, 
                assignmentId, 
                infoItems, 
                reason, 
                requestedBy
            );
            
            System.out.println("Additional information requested for Loan #" + loanId);
            System.out.println("Applicant ID: " + applicantId);
            System.out.println("Information Items: " + infoItems);
            System.out.println("Reason: " + reason);
            System.out.println("Notification created successfully");
            
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("message", "Information request sent to applicant");
            response.put("loanId", loanId);
            response.put("applicantId", applicantId);
            response.put("infoItems", infoItems);
            response.put("reason", reason);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Failed to request more info: " + e.getMessage()));
        }
    }
    
    /**
     * Request document resubmission from applicant
     * Sends notification to applicant to resubmit specific documents
     */
    @PostMapping("/{officerId}/request-document-resubmission")
    public ResponseEntity<?> requestDocumentResubmission(
            @PathVariable Long officerId,
            @Valid @RequestBody com.tss.springsecurity.dto.DocumentResubmissionRequest request) {
        try {
            System.out.println("Document resubmission requested by officer ID: " + officerId);
            System.out.println("Assignment ID: " + request.getAssignmentId());
            System.out.println("Document types: " + request.getDocumentTypes());
            
            // Get assignment details
            com.tss.springsecurity.entity.OfficerApplicationAssignment assignment = 
                loanOfficerDocumentService.getAssignmentById(request.getAssignmentId());
            
            Long applicantId = assignment.getApplicant().getApplicantId();
            Long loanId = assignment.getLoan().getLoanId();
            
            // Update document status to "RESUBMISSION_REQUESTED" for specified documents
            loanOfficerDocumentService.updateDocumentsForResubmission(loanId, request.getDocumentTypes(), request.getReason(), officerId);
            
            // Create notification for applicant
            String requestedBy = "Loan Officer #" + officerId;
            notificationService.createDocumentResubmissionRequest(
                applicantId, 
                loanId, 
                request.getAssignmentId(),
                request.getDocumentTypes(), 
                request.getReason(),
                request.getAdditionalComments(),
                requestedBy
            );
            
            System.out.println("Document resubmission request sent to applicant #" + applicantId);
            
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("message", "Document resubmission request sent to applicant");
            response.put("loanId", loanId);
            response.put("applicantId", applicantId);
            response.put("documentTypes", request.getDocumentTypes());
            response.put("reason", request.getReason());
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Failed to request document resubmission: " + e.getMessage()));
        }
    }
    
    /**
     * Trigger fraud screening for a loan application
     */
    @PostMapping("/{officerId}/trigger-fraud-check")
    public ResponseEntity<?> triggerFraudCheck(
            @PathVariable Long officerId,
            @RequestBody java.util.Map<String, Object> request) {
        try {
            Long loanId = Long.valueOf(request.get("loanId").toString());
            Long applicantId = Long.valueOf(request.get("applicantId").toString());
            
            // Get applicant details
            com.tss.springsecurity.entity.Applicant applicant = applicantRepository.findById(applicantId)
                    .orElseThrow(() -> new RuntimeException("Applicant not found"));
            
            // Get loan details
            com.tss.springsecurity.entity.ApplicantLoanDetails loan = loanRepository.findById(loanId)
                    .orElseThrow(() -> new RuntimeException("Loan not found"));
            
            // Get PAN and Aadhaar from basic details
            String panNumber = null;
            String aadhaarNumber = null;
            com.tss.springsecurity.entity.ApplicantBasicDetails basicDetails = 
                basicDetailsRepository.findByApplicant_ApplicantId(applicantId).orElse(null);
            if (basicDetails != null) {
                panNumber = basicDetails.getPanNumber();
                aadhaarNumber = basicDetails.getAadhaarNumber();
            }
            
            // TODO: Call external fraud screening API
            // For now, return mock fraud check result
            java.util.Map<String, Object> fraudResult = new java.util.HashMap<>();
            fraudResult.put("checkId", System.currentTimeMillis());
            fraudResult.put("loanId", loanId);
            fraudResult.put("applicantId", applicantId);
            fraudResult.put("panNumber", panNumber);
            fraudResult.put("aadhaarNumber", aadhaarNumber);
            fraudResult.put("phoneNumber", applicant.getPhone());
            fraudResult.put("email", applicant.getEmail());
            fraudResult.put("fraudTags", new java.util.ArrayList<>());
            fraudResult.put("riskLevel", "LOW");
            fraudResult.put("riskScore", 25);
            fraudResult.put("apiRemarks", "No fraud indicators detected");
            fraudResult.put("checkedAt", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(fraudResult);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Failed to trigger fraud check: " + e.getMessage()));
        }
    }
    
    /**
     * Get fraud check results for a loan
     */
    @GetMapping("/loan/{loanId}/fraud-check")
    public ResponseEntity<?> getFraudCheckResults(@PathVariable Long loanId) {
        try {
            // TODO: Retrieve actual fraud check results from database
            // For now, return mock data
            java.util.Map<String, Object> fraudResult = new java.util.HashMap<>();
            fraudResult.put("checkId", System.currentTimeMillis());
            fraudResult.put("loanId", loanId);
            fraudResult.put("fraudTags", new java.util.ArrayList<>());
            fraudResult.put("riskLevel", "LOW");
            fraudResult.put("riskScore", 25);
            fraudResult.put("apiRemarks", "No fraud indicators detected");
            fraudResult.put("checkedAt", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(fraudResult);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse("Fraud check results not found"));
        }
    }
    
    /**
     * Get comprehensive loan view with data from all related tables
     * Fetches: loan details, applicant info, documents, dependents, collaterals,
     * employment, financials, property, credit history, assignments, verification status
     */
    @GetMapping("/loan/{loanId}/comprehensive-view")
    public ResponseEntity<?> getComprehensiveLoanView(@PathVariable Long loanId) {
        try {
            com.tss.springsecurity.dto.ComprehensiveLoanViewDTO loanView = 
                    comprehensiveLoanViewService.getComprehensiveLoanView(loanId);
            return ResponseEntity.ok(loanView);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error fetching comprehensive loan view: " + e.getMessage()));
        }
    }
    
    /**
     * Get compliance verdict for a specific loan
     * This allows loan officers to see compliance decisions and take appropriate action
     */
    @GetMapping("/loan/{loanId}/compliance-verdict")
    public ResponseEntity<?> getComplianceVerdictForLoan(@PathVariable Long loanId) {
        try {
            log.info("Getting compliance verdict for loan ID: {}", loanId);
            
            com.tss.springsecurity.dto.ComplianceVerdictResponse verdict = 
                    screeningService.getComplianceVerdictForLoan(loanId);
            
            if (verdict != null) {
                return ResponseEntity.ok(verdict);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new MessageResponse("No compliance verdict found for this loan"));
            }
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error fetching compliance verdict: " + e.getMessage()));
        }
    }
    
    /**
     * Process loan after compliance verdict
     * Allows loan officer to take final action based on compliance recommendation
     */
    @PostMapping("/{officerId}/process-after-compliance")
    public ResponseEntity<?> processLoanAfterCompliance(
            @PathVariable Long officerId,
            @RequestBody java.util.Map<String, Object> request) {
        try {
            Long loanId = Long.valueOf(request.get("loanId").toString());
            Long assignmentId = Long.valueOf(request.get("assignmentId").toString());
            String decision = request.get("decision").toString(); // APPROVE, REJECT
            String remarks = request.get("remarks") != null ? request.get("remarks").toString() : "";
            
            log.info("Processing loan {} after compliance verdict by officer {}", loanId, officerId);
            
            com.tss.springsecurity.dto.LoanScreeningResponse response = 
                    screeningService.processLoanAfterCompliance(officerId, loanId, assignmentId, decision, remarks);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error processing loan after compliance: " + e.getMessage()));
        }
    }
    
    /**
     * Get pending loans that need final decision after compliance verdict
     * This shows loans that are escalated to compliance and now have verdicts available
     */
    @GetMapping("/{officerId}/pending-after-compliance")
    public ResponseEntity<?> getPendingLoansAfterCompliance(@PathVariable Long officerId) {
        try {
            List<LoanScreeningResponse> loans = screeningService.getAssignedLoansForOfficer(officerId);
            
            // Filter to only show loans that need final action after compliance verdict
            List<LoanScreeningResponse> pendingAfterCompliance = loans.stream()
                    .filter(loan -> "COMPLIANCE_VERDICT_AVAILABLE".equals(loan.getStatus()) || 
                                   ("ESCALATED_TO_COMPLIANCE".equals(loan.getStatus()) && 
                                    Boolean.TRUE.equals(loan.getHasComplianceVerdict())))
                    .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(pendingAfterCompliance);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error fetching pending loans after compliance: " + e.getMessage()));
        }
    }
    
    // ==================== Document Resubmission Requests from Compliance Officer ====================
    
    /**
     * Get document resubmission requests from compliance officers for this loan officer
     */
    @GetMapping("/{officerId}/document-resubmission-requests")
    public ResponseEntity<?> getDocumentResubmissionRequests(@PathVariable Long officerId) {
        try {
            log.info("Getting document resubmission requests for loan officer ID: {}", officerId);
            
            // Get all document resubmission requests with status "REQUESTED"
            List<com.tss.springsecurity.entity.DocumentResubmission> docRequests = 
                documentResubmissionRepository.findByStatusOrderByPriorityLevelDescRequestedAtDesc("REQUESTED");
            
            log.info("Found {} document resubmission requests with status REQUESTED", docRequests.size());
            
            List<java.util.Map<String, Object>> requests = new java.util.ArrayList<>();
            
            for (com.tss.springsecurity.entity.DocumentResubmission docRequest : docRequests) {
                log.info("=== Processing document request ID: {} for applicant ID: {} ===", 
                    docRequest.getResubmissionId(), docRequest.getApplicant().getApplicantId());
                log.info("Requested by compliance officer: {} (ID: {})", 
                    docRequest.getRequestedByOfficer().getFirstName() + " " + docRequest.getRequestedByOfficer().getLastName(),
                    docRequest.getRequestedByOfficer().getOfficerId());
                
                // Find the original loan officer who was assigned to this loan before escalation
                // Look for the loan officer assignment for this specific loan (not just applicant)
                com.tss.springsecurity.entity.ApplicantLoanDetails foundLoan = null;
                if (docRequest.getAssignment() != null && docRequest.getAssignment().getLoan() != null) {
                    foundLoan = docRequest.getAssignment().getLoan();
                    log.info("Found loan from compliance assignment: Loan ID {} for applicant {}", 
                        foundLoan.getLoanId(), foundLoan.getApplicant().getFirstName() + " " + foundLoan.getApplicant().getLastName());
                } else {
                    log.warn("No loan found in compliance assignment for resubmission ID: {}", docRequest.getResubmissionId());
                }
                
                com.tss.springsecurity.entity.OfficerApplicationAssignment assignment = null;
                boolean shouldIncludeRequest = false;
                final com.tss.springsecurity.entity.ApplicantLoanDetails finalLoan = foundLoan;
                
                if (finalLoan != null) {
                    // Try to find any loan officer assignment for this applicant (not necessarily the same loan)
                    // Since compliance and loan officer assignments are separate, we need a more flexible approach
                    java.util.List<com.tss.springsecurity.entity.OfficerApplicationAssignment> loanOfficerAssignments = 
                        assignmentRepository.findByApplicant_ApplicantId(docRequest.getApplicant().getApplicantId());
                    
                    log.info("Found {} loan officer assignments for applicant ID: {}", loanOfficerAssignments.size(), docRequest.getApplicant().getApplicantId());
                    
                    // Log all assignments for debugging
                    for (int i = 0; i < loanOfficerAssignments.size(); i++) {
                        com.tss.springsecurity.entity.OfficerApplicationAssignment a = loanOfficerAssignments.get(i);
                        log.info("Assignment {}: ID={}, Officer ID={}, Status={}, Officer Name={}, Loan ID={}", 
                            i+1, a.getAssignmentId(), a.getOfficer().getOfficerId(), a.getStatus(),
                            a.getOfficer().getFirstName() + " " + a.getOfficer().getLastName(),
                            a.getLoan() != null ? a.getLoan().getLoanId() : "null");
                    }
                    
                    if (!loanOfficerAssignments.isEmpty()) {
                        // Find assignment for this specific loan, or any assignment for this applicant
                        assignment = loanOfficerAssignments.stream()
                            .filter(a -> a.getLoan() != null && a.getLoan().getLoanId().equals(finalLoan.getLoanId()))
                            .findFirst()
                            .orElse(loanOfficerAssignments.stream()
                                .filter(a -> a.getOfficer().getOfficerId().equals(officerId))
                                .findFirst()
                                .orElse(loanOfficerAssignments.get(0)));
                        
                        log.info("Selected assignment ID: {} with officer ID: {} for processing", 
                            assignment.getAssignmentId(), assignment.getOfficer().getOfficerId());
                        
                        // Show to any loan officer who has worked with this applicant
                        shouldIncludeRequest = loanOfficerAssignments.stream()
                            .anyMatch(a -> a.getOfficer().getOfficerId().equals(officerId));
                        
                        if (shouldIncludeRequest) {
                            log.info("Including request for loan officer ID: {} (has assignment for this applicant)", officerId);
                        } else {
                            log.info("Skipping request - loan officer {} has no assignments for applicant {}", 
                                officerId, docRequest.getApplicant().getApplicantId());
                        }
                    } else {
                        log.warn("No loan officer assignments found for applicant ID: {}", docRequest.getApplicant().getApplicantId());
                        // For testing purposes, show to all loan officers if no specific assignment exists
                        shouldIncludeRequest = true;
                        log.info("No specific assignment found - showing to all loan officers for testing");
                    }
                } else {
                    log.warn("Could not find loan information for document resubmission ID: {}", docRequest.getResubmissionId());
                    // For testing - show to all loan officers if we can't find loan info
                    shouldIncludeRequest = true;
                    log.info("No loan info found - showing to all loan officers for testing");
                }
                
                if (shouldIncludeRequest) {
                        log.info("Including request for officer ID: {}", officerId);
                        
                        // Determine which loan to use
                        com.tss.springsecurity.entity.ApplicantLoanDetails loanToUse = finalLoan;
                        
                        // If loan is still null, try to find it by applicant ID as fallback
                        if (loanToUse == null) {
                            log.warn("Loan is null, trying to find loan by applicant ID: {}", docRequest.getApplicant().getApplicantId());
                            java.util.List<com.tss.springsecurity.entity.ApplicantLoanDetails> loans = 
                                loanRepository.findByApplicant_ApplicantId(docRequest.getApplicant().getApplicantId());
                            if (!loans.isEmpty()) {
                                loanToUse = loans.get(0); // Get the first loan for this applicant
                                log.info("Found fallback loan ID: {} for applicant", loanToUse.getLoanId());
                            } else {
                                log.warn("Could not find any loan for applicant ID: {}, skipping this request", docRequest.getApplicant().getApplicantId());
                                continue;
                            }
                        }
                        
                        java.util.Map<String, Object> request = new java.util.HashMap<>();
                        request.put("resubmissionId", docRequest.getResubmissionId());
                        request.put("assignmentId", assignment != null ? assignment.getAssignmentId() : null);
                        request.put("loanId", loanToUse.getLoanId());
                        request.put("applicantId", docRequest.getApplicant().getApplicantId());
                        request.put("applicantName", docRequest.getApplicant().getFirstName() + " " + docRequest.getApplicant().getLastName());
                        request.put("loanType", loanToUse.getLoanType());
                        request.put("loanAmount", loanToUse.getLoanAmount());
                        request.put("requestedDocuments", docRequest.getRequestedDocuments());
                        request.put("reason", docRequest.getReason());
                        request.put("additionalComments", docRequest.getAdditionalComments());
                        request.put("priorityLevel", docRequest.getPriorityLevel());
                        request.put("requestedAt", docRequest.getRequestedAt());
                        request.put("requestedBy", docRequest.getRequestedByOfficer().getFirstName() + " " + 
                                                  docRequest.getRequestedByOfficer().getLastName());
                        request.put("complianceOfficerId", docRequest.getRequestedByOfficer().getOfficerId());
                        request.put("status", docRequest.getStatus());
                        requests.add(request);
                }
            }
            
            log.info("Returning {} document resubmission requests for officer ID: {}", requests.size(), officerId);
            return ResponseEntity.ok(requests);
            
        } catch (Exception e) {
            log.error("Error getting document resubmission requests for officer ID: {}", officerId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Failed to get document resubmission requests: " + e.getMessage()));
        }
    }
    
    /**
     * Process document resubmission request from compliance officer
     * Loan officer can either approve and forward to applicant, or reject with reason
     */
    @PostMapping("/{officerId}/process-document-resubmission-request")
    public ResponseEntity<?> processDocumentResubmissionRequest(
            @PathVariable Long officerId,
            @RequestBody java.util.Map<String, Object> request) {
        try {
            Long resubmissionId = Long.valueOf(request.get("resubmissionId").toString());
            String action = request.get("action").toString(); // APPROVE, REJECT
            String loanOfficerRemarks = request.get("remarks") != null ? request.get("remarks").toString() : "";
            
            // Get the document resubmission request
            com.tss.springsecurity.entity.DocumentResubmission docRequest = 
                documentResubmissionRepository.findById(resubmissionId)
                    .orElseThrow(() -> new RuntimeException("Document resubmission request not found"));
            
            if ("APPROVE".equals(action)) {
                // Forward the request to applicant
                try {
                    // Parse the requested documents JSON
                    com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    @SuppressWarnings("unchecked")
                    List<String> documentTypes = objectMapper.readValue(docRequest.getRequestedDocuments(), List.class);
                    
                    // Create notification for applicant
                    String requestedBy = "Loan Officer #" + officerId + " (forwarded from Compliance)";
                    notificationService.createDocumentResubmissionRequest(
                        docRequest.getApplicant().getApplicantId(),
                        docRequest.getAssignment().getApplicant().getLoanDetails().get(0).getLoanId(),
                        docRequest.getAssignment().getAssignmentId(),
                        documentTypes,
                        docRequest.getReason(),
                        docRequest.getAdditionalComments() + 
                        (loanOfficerRemarks.isEmpty() ? "" : " | Loan Officer Notes: " + loanOfficerRemarks),
                        requestedBy
                    );
                    
                    // Update document resubmission status
                    docRequest.setStatus("FORWARDED_TO_APPLICANT");
                    docRequest.setReviewedAt(java.time.LocalDateTime.now());
                    documentResubmissionRepository.save(docRequest);
                    
                    // Update assignment status
                    List<com.tss.springsecurity.entity.OfficerApplicationAssignment> assignments = 
                        assignmentRepository.findByApplicant_ApplicantIdAndOfficer_OfficerId(
                            docRequest.getApplicant().getApplicantId(), officerId);
                    
                    for (com.tss.springsecurity.entity.OfficerApplicationAssignment assignment : assignments) {
                        assignment.setStatus("DOCUMENT_RESUBMISSION_FORWARDED");
                        assignment.setRemarks("Document resubmission request forwarded to applicant. " + loanOfficerRemarks);
                        assignmentRepository.save(assignment);
                    }
                    
                    java.util.Map<String, Object> response = new java.util.HashMap<>();
                    response.put("success", true);
                    response.put("message", "Document resubmission request forwarded to applicant");
                    response.put("resubmissionId", resubmissionId);
                    response.put("action", "FORWARDED");
                    
                    return ResponseEntity.ok(response);
                    
                } catch (Exception e) {
                    throw new RuntimeException("Failed to forward request to applicant: " + e.getMessage());
                }
                
            } else if ("REJECT".equals(action)) {
                String rejectionReason = request.get("rejectionReason") != null ? 
                    request.get("rejectionReason").toString() : "Rejected by loan officer";
                
                // Update document resubmission status
                docRequest.setStatus("REJECTED_BY_LOAN_OFFICER");
                docRequest.setReviewedAt(java.time.LocalDateTime.now());
                docRequest.setAdditionalComments(docRequest.getAdditionalComments() + 
                    " | Rejected by Loan Officer: " + rejectionReason + 
                    (loanOfficerRemarks.isEmpty() ? "" : " | Remarks: " + loanOfficerRemarks));
                documentResubmissionRepository.save(docRequest);
                
                // Update assignment status
                List<com.tss.springsecurity.entity.OfficerApplicationAssignment> assignments = 
                    assignmentRepository.findByApplicant_ApplicantIdAndOfficer_OfficerId(
                        docRequest.getApplicant().getApplicantId(), officerId);
                
                for (com.tss.springsecurity.entity.OfficerApplicationAssignment assignment : assignments) {
                    assignment.setStatus("DOCUMENT_RESUBMISSION_REJECTED");
                    assignment.setRemarks("Document resubmission request rejected. Reason: " + rejectionReason + 
                                        (loanOfficerRemarks.isEmpty() ? "" : " | " + loanOfficerRemarks));
                    assignmentRepository.save(assignment);
                }
                
                java.util.Map<String, Object> response = new java.util.HashMap<>();
                response.put("success", true);
                response.put("message", "Document resubmission request rejected");
                response.put("resubmissionId", resubmissionId);
                response.put("action", "REJECTED");
                response.put("rejectionReason", rejectionReason);
                
                return ResponseEntity.ok(response);
                
            } else {
                throw new RuntimeException("Invalid action: " + action + ". Must be APPROVE or REJECT");
            }
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Failed to process document resubmission request: " + e.getMessage()));
        }
    }
    
}
