package com.tss.springsecurity.controller;

import com.tss.springsecurity.entity.UploadedDocument;
import com.tss.springsecurity.service.DocumentUploadService;
import com.tss.springsecurity.service.DocumentExtractionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"}, allowCredentials = "true")
@Slf4j
public class DocumentUploadController {
    
    @Autowired
    private DocumentUploadService documentUploadService;
    
    @Autowired
    private DocumentExtractionService documentExtractionService;
    
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadDocuments(
            @RequestParam("applicantId") Long applicantId,
            @RequestParam(value = "loanId", required = false) Long loanId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("documentTypes") List<String> documentTypes) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate input
            if (files == null || files.isEmpty()) {
                response.put("success", false);
                response.put("message", "No files provided for upload");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            
            if (documentTypes == null || documentTypes.size() != files.size()) {
                response.put("success", false);
                response.put("message", "Document types must be provided for each file");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            
            // Upload documents
            List<UploadedDocument> uploadedDocuments = documentUploadService.uploadDocuments(
                    applicantId, loanId, files, documentTypes);
            
            // Trigger document extraction asynchronously for each document
            for (int i = 0; i < uploadedDocuments.size(); i++) {
                UploadedDocument doc = uploadedDocuments.get(i);
                MultipartFile file = files.get(i);
                
                try {
                    log.info("Triggering extraction for document: {} (type: {})", 
                            doc.getDocumentName(), doc.getDocumentType());
                    
                    // Call extraction service asynchronously (non-blocking)
                    new Thread(() -> {
                        try {
                            Map<String, Object> extractedData = documentExtractionService.extractDocumentData(
                                    file, doc.getDocumentType(), applicantId);
                            
                            if (extractedData != null && !extractedData.containsKey("error")) {
                                log.info("Document extraction successful for documentId: {}", doc.getDocumentId());
                                // TODO: Store extracted data in database if needed
                            } else {
                                log.warn("Document extraction failed for documentId: {}", doc.getDocumentId());
                            }
                        } catch (Exception e) {
                            log.error("Error during async extraction for documentId: {}", doc.getDocumentId(), e);
                        }
                    }).start();
                    
                } catch (Exception e) {
                    log.error("Failed to trigger extraction for document: {}", doc.getDocumentName(), e);
                    // Continue with other documents even if extraction fails
                }
            }
            
            // Prepare response data
            List<Map<String, Object>> documentDetails = new ArrayList<>();
            for (UploadedDocument doc : uploadedDocuments) {
                Map<String, Object> docInfo = new HashMap<>();
                docInfo.put("documentId", doc.getDocumentId());
                docInfo.put("documentType", doc.getDocumentType());
                docInfo.put("documentName", doc.getDocumentName());
                docInfo.put("originalFilename", doc.getOriginalFilename());
                docInfo.put("cloudinaryUrl", doc.getCloudinaryUrl());
                docInfo.put("fileSize", doc.getFileSize());
                docInfo.put("uploadStatus", doc.getUploadStatus());
                docInfo.put("verificationStatus", doc.getVerificationStatus());
                docInfo.put("uploadedAt", doc.getUploadedAt());
                documentDetails.add(docInfo);
            }
            
            response.put("success", true);
            response.put("message", "Application submitted successfully! Now you can track your application");
            response.put("totalUploaded", uploadedDocuments.size());
            response.put("documents", documentDetails);
            response.put("applicantId", applicantId);
            
            if (loanId != null) {
                response.put("loanId", loanId);
            }
            
            return new ResponseEntity<>(response, HttpStatus.CREATED);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to upload documents: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PostMapping("/upload-single")
    public ResponseEntity<Map<String, Object>> uploadSingleDocument(
            @RequestParam("applicantId") Long applicantId,
            @RequestParam(value = "loanId", required = false) Long loanId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType) {
        
        log.info("Received upload request - ApplicantId: {}, DocumentType: {}, File: {}", 
                applicantId, documentType, file.getOriginalFilename());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Upload document
            UploadedDocument uploadedDocument = documentUploadService.uploadDocument(
                    applicantId, loanId, file, documentType);
            
            log.info("Document upload completed successfully for: {}", file.getOriginalFilename());
            
            // Prepare response data
            Map<String, Object> docInfo = new HashMap<>();
            docInfo.put("documentId", uploadedDocument.getDocumentId());
            docInfo.put("documentType", uploadedDocument.getDocumentType());
            docInfo.put("documentName", uploadedDocument.getDocumentName());
            docInfo.put("originalFilename", uploadedDocument.getOriginalFilename());
            docInfo.put("cloudinaryUrl", uploadedDocument.getCloudinaryUrl());
            docInfo.put("fileSize", uploadedDocument.getFileSize());
            docInfo.put("uploadStatus", uploadedDocument.getUploadStatus());
            docInfo.put("verificationStatus", uploadedDocument.getVerificationStatus());
            docInfo.put("uploadedAt", uploadedDocument.getUploadedAt());
            
            response.put("success", true);
            response.put("message", "Document uploaded successfully");
            response.put("document", docInfo);
            response.put("applicantId", applicantId);
            
            if (loanId != null) {
                response.put("loanId", loanId);
            }
            
            log.info("Returning success response for: {}", file.getOriginalFilename());
            return new ResponseEntity<>(response, HttpStatus.CREATED);
            
        } catch (RuntimeException e) {
            log.error("RuntimeException during upload - File: {}, Error: {}", file.getOriginalFilename(), e.getMessage(), e);
            response.put("success", false);
            response.put("message", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Exception during upload - File: {}, Error: {}", file.getOriginalFilename(), e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to upload document: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/applicant/{applicantId}")
    public ResponseEntity<Map<String, Object>> getDocumentsByApplicant(@PathVariable Long applicantId) {
        try {
            List<UploadedDocument> documents = documentUploadService.getDocumentsByApplicant(applicantId);
            Map<String, Object> summary = documentUploadService.getDocumentUploadSummary(applicantId);
            
            List<Map<String, Object>> documentDetails = new ArrayList<>();
            for (UploadedDocument doc : documents) {
                Map<String, Object> docInfo = new HashMap<>();
                docInfo.put("documentId", doc.getDocumentId());
                docInfo.put("documentType", doc.getDocumentType());
                docInfo.put("documentName", doc.getDocumentName());
                docInfo.put("originalFilename", doc.getOriginalFilename());
                docInfo.put("cloudinaryUrl", doc.getCloudinaryUrl());
                docInfo.put("fileSize", doc.getFileSize());
                docInfo.put("uploadStatus", doc.getUploadStatus());
                docInfo.put("verificationStatus", doc.getVerificationStatus());
                docInfo.put("uploadedAt", doc.getUploadedAt());
                docInfo.put("verifiedAt", doc.getVerifiedAt());
                docInfo.put("verifiedBy", doc.getVerifiedBy());
                docInfo.put("verificationNotes", doc.getVerificationNotes());
                documentDetails.add(docInfo);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("applicantId", applicantId);
            response.put("documents", documentDetails);
            response.put("summary", summary);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to retrieve documents: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/loan/{loanId}")
    public ResponseEntity<Map<String, Object>> getDocumentsByLoan(@PathVariable Long loanId) {
        try {
            List<UploadedDocument> documents = documentUploadService.getDocumentsByLoan(loanId);
            
            List<Map<String, Object>> documentDetails = new ArrayList<>();
            for (UploadedDocument doc : documents) {
                Map<String, Object> docInfo = new HashMap<>();
                docInfo.put("documentId", doc.getDocumentId());
                docInfo.put("documentType", doc.getDocumentType());
                docInfo.put("documentName", doc.getDocumentName());
                docInfo.put("originalFilename", doc.getOriginalFilename());
                docInfo.put("cloudinaryUrl", doc.getCloudinaryUrl());
                docInfo.put("fileSize", doc.getFileSize());
                docInfo.put("uploadStatus", doc.getUploadStatus());
                docInfo.put("verificationStatus", doc.getVerificationStatus());
                docInfo.put("uploadedAt", doc.getUploadedAt());
                docInfo.put("verifiedAt", doc.getVerifiedAt());
                docInfo.put("verifiedBy", doc.getVerifiedBy());
                docInfo.put("verificationNotes", doc.getVerificationNotes());
                documentDetails.add(docInfo);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("loanId", loanId);
            response.put("documents", documentDetails);
            response.put("totalDocuments", documents.size());
            
            return new ResponseEntity<>(response, HttpStatus.OK);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to retrieve documents: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @DeleteMapping("/{documentId}")
    public ResponseEntity<Map<String, Object>> deleteDocument(@PathVariable Long documentId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean deleted = documentUploadService.deleteDocument(documentId);
            
            if (deleted) {
                response.put("success", true);
                response.put("message", "Document deleted successfully");
            } else {
                response.put("success", false);
                response.put("message", "Failed to delete document from cloud storage");
            }
            
            return new ResponseEntity<>(response, HttpStatus.OK);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to delete document: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PutMapping("/{documentId}/verify")
    public ResponseEntity<Map<String, Object>> updateVerificationStatus(
            @PathVariable Long documentId,
            @RequestParam("verificationStatus") String verificationStatus,
            @RequestParam(value = "verificationNotes", required = false) String verificationNotes,
            @RequestParam("verifiedBy") String verifiedBy) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            UploadedDocument updatedDocument = documentUploadService.updateVerificationStatus(
                    documentId, verificationStatus, verificationNotes, verifiedBy);
            
            Map<String, Object> docInfo = new HashMap<>();
            docInfo.put("documentId", updatedDocument.getDocumentId());
            docInfo.put("documentType", updatedDocument.getDocumentType());
            docInfo.put("verificationStatus", updatedDocument.getVerificationStatus());
            docInfo.put("verificationNotes", updatedDocument.getVerificationNotes());
            docInfo.put("verifiedBy", updatedDocument.getVerifiedBy());
            docInfo.put("verifiedAt", updatedDocument.getVerifiedAt());
            
            response.put("success", true);
            response.put("message", "Document verification status updated successfully");
            response.put("document", docInfo);
            
            return new ResponseEntity<>(response, HttpStatus.OK);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to update verification status: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
