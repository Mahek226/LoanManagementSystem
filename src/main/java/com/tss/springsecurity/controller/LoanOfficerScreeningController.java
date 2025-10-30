package com.tss.springsecurity.controller;

import com.tss.springsecurity.dto.LoanScreeningRequest;
import com.tss.springsecurity.dto.LoanScreeningResponse;
import com.tss.springsecurity.dto.LoanScreeningDecision;
import com.tss.springsecurity.dto.ScreeningDashboardResponse;
import com.tss.springsecurity.service.LoanOfficerScreeningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loan-officer")
@RequiredArgsConstructor
public class LoanOfficerScreeningController {
    
    private final LoanOfficerScreeningService screeningService;
    private final com.tss.springsecurity.service.DocumentExtractionService documentExtractionService;
    private final com.tss.springsecurity.repository.UploadedDocumentRepository documentRepository;
    private final com.tss.springsecurity.service.DocumentUploadService documentUploadService;
    private final com.tss.springsecurity.repository.ApplicantLoanDetailsRepository loanRepository;
    private final com.tss.springsecurity.repository.ApplicantRepository applicantRepository;
    
    @GetMapping("/{officerId}/assigned-loans")
    public ResponseEntity<List<LoanScreeningResponse>> getAssignedLoans(@PathVariable Long officerId) {
        List<LoanScreeningResponse> loans = screeningService.getAssignedLoansForOfficer(officerId);
        return ResponseEntity.ok(loans);
    }
    
    @GetMapping("/assignment/{assignmentId}/details")
    public ResponseEntity<?> getLoanDetailsForScreening(@PathVariable Long assignmentId) {
        try {
            LoanScreeningResponse response = screeningService.getLoanDetailsForScreening(assignmentId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
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
                    .body(new ErrorResponse(e.getMessage()));
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
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @GetMapping("/{officerId}/screening-dashboard")
    public ResponseEntity<?> getScreeningDashboard(@PathVariable Long officerId) {
        try {
            ScreeningDashboardResponse dashboard = screeningService.getScreeningDashboard(officerId);
            return ResponseEntity.ok(dashboard);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
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
                    .body(new ErrorResponse(e.getMessage()));
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
                    .body(new ErrorResponse(e.getMessage()));
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
            
            // Get loan details to find applicant ID
            LoanScreeningResponse loanDetails = screeningService.getLoanDetailsForScreening(assignmentId);
            Long applicantId = loanDetails.getApplicantId();
            
            System.out.println("Found applicant ID: " + applicantId + " for assignment: " + assignmentId);
            
            // Get all documents for this applicant
            List<com.tss.springsecurity.entity.UploadedDocument> documents = 
                documentRepository.findByApplicant_ApplicantId(applicantId);
            
            System.out.println("Found " + documents.size() + " documents for applicant ID: " + applicantId);
            
            if (documents.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("No documents found for this applicant"));
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
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            System.err.println("Exception in document extraction: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to extract documents: " + e.getMessage()));
        }
    }
    
    /**
     * Get documents for a specific loan
     */
    @GetMapping("/loan/{loanId}/documents")
    public ResponseEntity<?> getLoanDocuments(@PathVariable Long loanId) {
        try {
            System.out.println("Fetching documents for loan ID: " + loanId);
            
            List<com.tss.springsecurity.entity.UploadedDocument> documents = 
                documentRepository.findByLoan_LoanId(loanId);
            
            System.out.println("Found " + documents.size() + " documents for loan ID: " + loanId);
            
            // If no documents found with loan relationship, try finding by applicant
            if (documents.isEmpty()) {
                // Get loan details to find applicant
                com.tss.springsecurity.entity.ApplicantLoanDetails loan = 
                    loanRepository.findById(loanId).orElse(null);
                
                if (loan != null && loan.getApplicant() != null) {
                    Long applicantId = loan.getApplicant().getApplicantId();
                    System.out.println("No documents found with loan_id, trying applicant_id: " + applicantId);
                    documents = documentRepository.findByApplicant_ApplicantId(applicantId);
                    System.out.println("Found " + documents.size() + " documents for applicant ID: " + applicantId);
                }
            }
            
            List<java.util.Map<String, Object>> documentDetails = new java.util.ArrayList<>();
            for (com.tss.springsecurity.entity.UploadedDocument doc : documents) {
                java.util.Map<String, Object> docInfo = new java.util.HashMap<>();
                docInfo.put("documentId", doc.getDocumentId());
                docInfo.put("documentType", doc.getDocumentType());
                docInfo.put("documentName", doc.getDocumentName());
                docInfo.put("documentUrl", doc.getCloudinaryUrl());
                docInfo.put("uploadedAt", doc.getUploadedAt());
                docInfo.put("verificationStatus", doc.getVerificationStatus().toString());
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
                    .body(new ErrorResponse("Failed to retrieve documents: " + e.getMessage()));
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
            docInfo.put("verificationStatus", updatedDoc.getVerificationStatus().toString());
            docInfo.put("verifiedBy", updatedDoc.getVerifiedBy());
            docInfo.put("verifiedAt", updatedDoc.getVerifiedAt());
            docInfo.put("remarks", updatedDoc.getVerificationNotes());
            
            return ResponseEntity.ok(docInfo);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Failed to verify document: " + e.getMessage()));
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
            @SuppressWarnings("unchecked")
            List<String> documentTypes = (List<String>) request.get("documentTypes");
            String reason = request.get("reason").toString();
            String remarks = request.get("remarks") != null ? request.get("remarks").toString() : "";
            
            // TODO: Implement notification service to send email/SMS to applicant
            // For now, just log the request
            System.out.println("Document resubmission requested for Loan #" + loanId);
            System.out.println("Applicant ID: " + applicantId);
            System.out.println("Document Types: " + documentTypes);
            System.out.println("Reason: " + reason);
            
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
                    .body(new ErrorResponse("Failed to request document resubmission: " + e.getMessage()));
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
            
            // TODO: Call external fraud screening API
            // For now, return mock fraud check result
            java.util.Map<String, Object> fraudResult = new java.util.HashMap<>();
            fraudResult.put("checkId", System.currentTimeMillis());
            fraudResult.put("loanId", loanId);
            fraudResult.put("applicantId", applicantId);
            fraudResult.put("panNumber", loan.getPanNumber());
            fraudResult.put("aadhaarNumber", loan.getAadhaarNumber());
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
                    .body(new ErrorResponse("Failed to trigger fraud check: " + e.getMessage()));
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
                    .body(new ErrorResponse("Fraud check results not found"));
        }
    }
    
    // ==================== Response Classes ====================
    
    private record ErrorResponse(String message) {}
    
    private record SuccessResponse(String message) {}
}
