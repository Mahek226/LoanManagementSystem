package com.tss.springsecurity.service;

import com.tss.springsecurity.entity.UploadedDocument;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface DocumentUploadService {
    
    /**
     * Upload multiple documents for an applicant
     * @param applicantId Applicant ID
     * @param loanId Loan ID (optional)
     * @param files List of files to upload
     * @param documentTypes List of document types corresponding to files
     * @return List of uploaded document records
     * @throws IOException if upload fails
     */
    List<UploadedDocument> uploadDocuments(Long applicantId, Long loanId, 
                                         List<MultipartFile> files, 
                                         List<String> documentTypes) throws IOException;
    
    /**
     * Upload a single document for an applicant
     * @param applicantId Applicant ID
     * @param loanId Loan ID (optional)
     * @param file File to upload
     * @param documentType Document type
     * @return Uploaded document record
     * @throws IOException if upload fails
     */
    UploadedDocument uploadDocument(Long applicantId, Long loanId, 
                                  MultipartFile file, String documentType) throws IOException;
    
    /**
     * Upload document resubmission
     * @param applicantId Applicant ID
     * @param loanId Loan ID
     * @param file File to upload
     * @param documentType Document type
     * @param notificationId Notification ID that triggered this resubmission
     * @param assignmentId Assignment ID for routing to correct officer
     * @param applicantComments Optional comments from applicant
     * @return Map with upload result details
     * @throws IOException if upload fails
     */
    Map<String, Object> uploadDocumentResubmission(Long applicantId, Long loanId, 
                                                 MultipartFile file, String documentType, 
                                                 Long notificationId, Long assignmentId, 
                                                 String applicantComments) throws IOException;
    
    /**
     * Get all documents for an applicant
     * @param applicantId Applicant ID
     * @return List of uploaded documents
     */
    List<UploadedDocument> getDocumentsByApplicant(Long applicantId);
    
    /**
     * Get all documents for a loan
     * @param loanId Loan ID
     * @return List of uploaded documents
     */
    List<UploadedDocument> getDocumentsByLoan(Long loanId);
    
    /**
     * Delete a document
     * @param documentId Document ID
     * @return true if deleted successfully
     */
    boolean deleteDocument(Long documentId);
    
    /**
     * Update document verification status
     * @param documentId Document ID
     * @param verificationStatus New verification status
     * @param verificationNotes Verification notes
     * @param verifiedBy Who verified the document
     * @return Updated document
     */
    UploadedDocument updateVerificationStatus(Long documentId, String verificationStatus, 
                                            String verificationNotes, String verifiedBy);
    
    /**
     * Get document upload summary for an applicant
     * @param applicantId Applicant ID
     * @return Summary map with counts and status
     */
    Map<String, Object> getDocumentUploadSummary(Long applicantId);
    
    /**
     * Get all documents in the system
     * @return List of all uploaded documents
     */
    List<UploadedDocument> getAllDocuments();
}
