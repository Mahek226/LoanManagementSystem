package com.tss.springsecurity.service;

import com.tss.springsecurity.entity.*;
import com.tss.springsecurity.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanOfficerDocumentService {

    private final UploadedDocumentRepository documentRepository;
    private final ApplicantLoanDetailsRepository loanRepository;
    private final ApplicantRepository applicantRepository;
    private final OfficerApplicationAssignmentRepository assignmentRepository;
    private final ApplicantBasicDetailsRepository basicDetailsRepository;
    private final DocumentResubmissionRepository documentResubmissionRepository;

    public List<UploadedDocument> getDocumentsByLoanId(Long loanId) {
        return documentRepository.findByLoan_LoanId(loanId);
    }

    public Applicant getApplicantById(Long applicantId) {
        return applicantRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("Applicant not found"));
    }

    public ApplicantLoanDetails getLoanById(Long loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));
    }

    public Optional<ApplicantBasicDetails> getBasicDetailsByApplicantId(Long applicantId) {
        return basicDetailsRepository.findByApplicant_ApplicantId(applicantId);
    }

    public OfficerApplicationAssignment getAssignmentById(Long assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
    }

    public void updateDocumentVerificationStatus(Long documentId, String status, String reason, String verifiedBy) {
        UploadedDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        
        document.setVerificationStatus(UploadedDocument.VerificationStatus.valueOf(status));
        document.setVerificationNotes(reason);
        document.setVerifiedBy(verifiedBy);
        document.setVerifiedAt(LocalDateTime.now());
        
        documentRepository.save(document);
    }

    public void updateDocumentsForResubmission(Long loanId, List<String> documentTypes, String reason, Long officerId) {
        List<UploadedDocument> documents = documentRepository.findByLoan_LoanId(loanId);
        
        for (UploadedDocument doc : documents) {
            if (documentTypes.contains(doc.getDocumentType())) {
                doc.setVerificationStatus(UploadedDocument.VerificationStatus.RESUBMISSION_REQUESTED);
                doc.setVerificationNotes(reason);
                doc.setVerifiedBy("Officer #" + officerId);
                doc.setVerifiedAt(LocalDateTime.now());
                documentRepository.save(doc);
            }
        }
    }

    public List<OfficerApplicationAssignment> getAssignmentsByApplicantId(Long applicantId) {
        return assignmentRepository.findByApplicant_ApplicantId(applicantId);
    }

    public List<ApplicantLoanDetails> getLoansByApplicantId(Long applicantId) {
        return loanRepository.findByApplicant_ApplicantId(applicantId);
    }

    public List<DocumentResubmission> getDocumentResubmissionRequests(String status) {
        return documentResubmissionRepository.findByStatusOrderByPriorityLevelDescRequestedAtDesc(status);
    }

    public DocumentResubmission getDocumentResubmissionById(Long resubmissionId) {
        return documentResubmissionRepository.findById(resubmissionId)
                .orElseThrow(() -> new RuntimeException("Document resubmission request not found"));
    }

    public void updateDocumentResubmissionStatus(Long resubmissionId, String status, String processedBy) {
        DocumentResubmission docRequest = getDocumentResubmissionById(resubmissionId);
        docRequest.setStatus(status);
        docRequest.setProcessedBy(processedBy);
        docRequest.setProcessedAt(LocalDateTime.now());
        documentResubmissionRepository.save(docRequest);
    }
    
    /**
     * Get resubmitted documents for a specific loan officer
     */
    public List<Map<String, Object>> getResubmittedDocumentsForOfficer(Long officerId) {
        log.info("Fetching resubmitted documents for officer: {}", officerId);
        
        // Get all assignments for this officer
        List<OfficerApplicationAssignment> assignments = assignmentRepository.findByOfficer_OfficerId(officerId);
        
        List<Map<String, Object>> resubmittedDocuments = new ArrayList<>();
        
        for (OfficerApplicationAssignment assignment : assignments) {
            // Get documents that are resubmissions for this assignment
            List<UploadedDocument> documents = documentRepository.findByAssignmentIdAndIsResubmission(
                    assignment.getAssignmentId(), true);
            
            for (UploadedDocument doc : documents) {
                Map<String, Object> docInfo = new HashMap<>();
                docInfo.put("documentId", doc.getDocumentId());
                docInfo.put("loanId", doc.getLoan() != null ? doc.getLoan().getLoanId() : null);
                docInfo.put("applicantId", doc.getApplicant().getApplicantId());
                docInfo.put("applicantName", doc.getApplicant().getFirstName() + " " + doc.getApplicant().getLastName());
                docInfo.put("documentType", doc.getDocumentType());
                docInfo.put("documentName", doc.getDocumentName());
                docInfo.put("documentUrl", doc.getCloudinaryUrl());
                docInfo.put("resubmittedAt", doc.getUploadedAt());
                docInfo.put("originalRequestReason", "Document resubmission requested");
                docInfo.put("applicantComments", doc.getApplicantComments());
                docInfo.put("status", doc.getVerificationStatus().name());
                docInfo.put("loanType", doc.getLoan() != null ? doc.getLoan().getLoanType() : "Unknown");
                docInfo.put("loanAmount", doc.getLoan() != null ? doc.getLoan().getLoanAmount() : 0);
                docInfo.put("notificationId", doc.getOriginalNotificationId());
                docInfo.put("assignmentId", doc.getAssignmentId());
                
                resubmittedDocuments.add(docInfo);
            }
        }
        
        // Sort by resubmission date (newest first)
        resubmittedDocuments.sort((a, b) -> {
            LocalDateTime dateA = (LocalDateTime) a.get("resubmittedAt");
            LocalDateTime dateB = (LocalDateTime) b.get("resubmittedAt");
            return dateB.compareTo(dateA);
        });
        
        log.info("Found {} resubmitted documents for officer {}", resubmittedDocuments.size(), officerId);
        return resubmittedDocuments;
    }
    
    /**
     * Process a resubmitted document
     */
    public Map<String, Object> processResubmittedDocument(Long documentId, String action, String remarks, 
                                                        String forwardReason, Long officerId, Long assignmentId) {
        
        log.info("Processing resubmitted document - DocumentId: {}, Action: {}, OfficerId: {}", 
                documentId, action, officerId);
        
        UploadedDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found with ID: " + documentId));
        
        String officerName = "Officer #" + officerId;
        
        switch (action.toUpperCase()) {
            case "APPROVE":
                document.setVerificationStatus(UploadedDocument.VerificationStatus.VERIFIED);
                document.setVerificationNotes(remarks != null ? remarks : "Document approved after resubmission");
                document.setVerifiedBy(officerName);
                document.setVerifiedAt(LocalDateTime.now());
                break;
                
            case "REJECT":
                document.setVerificationStatus(UploadedDocument.VerificationStatus.REJECTED);
                document.setVerificationNotes(remarks != null ? remarks : "Document rejected after resubmission");
                document.setVerifiedBy(officerName);
                document.setVerifiedAt(LocalDateTime.now());
                break;
                
            case "FORWARD_TO_COMPLIANCE":
                document.setVerificationStatus(UploadedDocument.VerificationStatus.PENDING);
                document.setVerificationNotes("Forwarded to compliance: " + (forwardReason != null ? forwardReason : "Requires compliance review"));
                document.setVerifiedBy(officerName);
                document.setVerifiedAt(LocalDateTime.now());
                break;
                
            default:
                throw new RuntimeException("Invalid action: " + action + ". Must be APPROVE, REJECT, or FORWARD_TO_COMPLIANCE");
        }
        
        documentRepository.save(document);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Document " + action.toLowerCase() + " successfully");
        response.put("documentId", documentId);
        response.put("newStatus", document.getVerificationStatus().name());
        response.put("processedAt", LocalDateTime.now());
        response.put("processedBy", officerName);
        
        return response;
    }
    
    /**
     * Get document resubmission requests from compliance officers for a loan officer
     */
    public List<Map<String, Object>> getDocumentResubmissionRequestsForOfficer(Long officerId) {
        log.info("Fetching document resubmission requests for officer: {}", officerId);
        
        // Get all document resubmission requests that are in REQUESTED status
        List<DocumentResubmission> requests = documentResubmissionRepository.findByStatusOrderByPriorityLevelDescRequestedAtDesc("REQUESTED");
        
        List<Map<String, Object>> filteredRequests = new ArrayList<>();
        
        for (DocumentResubmission request : requests) {
            // Check if this request is for an application assigned to this loan officer
            // We need to find the loan officer assignment for the same loan/applicant
            Long loanId = request.getAssignment().getLoan().getLoanId();
            Long applicantId = request.getApplicant().getApplicantId();
            
            // Find loan officer assignments for this loan/applicant
            List<OfficerApplicationAssignment> loanOfficerAssignments = assignmentRepository
                .findByApplicant_ApplicantIdAndOfficer_OfficerId(applicantId, officerId);
            
            // Check if this loan officer is assigned to this applicant
            boolean isAssignedToOfficer = loanOfficerAssignments.stream()
                .anyMatch(assignment -> assignment.getLoan() != null && 
                         assignment.getLoan().getLoanId().equals(loanId));
            
            if (isAssignedToOfficer) {
                Map<String, Object> requestInfo = new HashMap<>();
                requestInfo.put("resubmissionId", request.getResubmissionId());
                requestInfo.put("assignmentId", request.getAssignment().getAssignmentId());
                requestInfo.put("loanId", loanId);
                requestInfo.put("applicantId", applicantId);
                requestInfo.put("applicantName", request.getApplicant().getFirstName() + " " + request.getApplicant().getLastName());
                requestInfo.put("loanType", request.getAssignment().getLoan().getLoanType());
                requestInfo.put("loanAmount", request.getAssignment().getLoan().getLoanAmount());
                requestInfo.put("requestedDocuments", request.getRequestedDocuments());
                requestInfo.put("reason", request.getReason());
                requestInfo.put("additionalComments", request.getAdditionalComments());
                requestInfo.put("priorityLevel", request.getPriorityLevel());
                requestInfo.put("status", request.getStatus());
                requestInfo.put("requestedAt", request.getRequestedAt());
                
                filteredRequests.add(requestInfo);
            }
        }
        
        log.info("Found {} document resubmission requests for officer: {}", filteredRequests.size(), officerId);
        filteredRequests.sort((a, b) -> {
            Integer priorityA = (Integer) a.get("priorityLevel");
            Integer priorityB = (Integer) b.get("priorityLevel");
            int priorityComparison = priorityB.compareTo(priorityA); // Higher priority first
            
            if (priorityComparison != 0) {
                return priorityComparison;
            }
            
            LocalDateTime dateA = (LocalDateTime) a.get("requestedAt");
            LocalDateTime dateB = (LocalDateTime) b.get("requestedAt");
            return dateB.compareTo(dateA); // Newer first
        });
        
        log.info("Found {} document resubmission requests for officer {}", filteredRequests.size(), officerId);
        return filteredRequests;
    }
}
