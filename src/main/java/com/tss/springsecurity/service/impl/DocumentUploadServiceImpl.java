package com.tss.springsecurity.service.impl;

import com.tss.springsecurity.entity.Applicant;
import com.tss.springsecurity.entity.ApplicantLoanDetails;
import com.tss.springsecurity.entity.UploadedDocument;
import com.tss.springsecurity.repository.ApplicantRepository;
import com.tss.springsecurity.repository.ApplicantLoanDetailsRepository;
import com.tss.springsecurity.repository.UploadedDocumentRepository;
import com.tss.springsecurity.service.CloudinaryService;
import com.tss.springsecurity.service.DocumentUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class DocumentUploadServiceImpl implements DocumentUploadService {
    
    @Autowired
    private UploadedDocumentRepository uploadedDocumentRepository;
    
    @Autowired
    private ApplicantRepository applicantRepository;
    
    @Autowired
    private ApplicantLoanDetailsRepository loanDetailsRepository;
    
    @Autowired
    private CloudinaryService cloudinaryService;
    
    @Override
    public List<UploadedDocument> uploadDocuments(Long applicantId, Long loanId, 
                                                List<MultipartFile> files, 
                                                List<String> documentTypes) throws IOException {
        
        // Validate applicant exists
        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("Applicant not found with ID: " + applicantId));
        
        // Validate loan exists if provided
        ApplicantLoanDetails loan = null;
        if (loanId != null) {
            loan = loanDetailsRepository.findById(loanId)
                    .orElseThrow(() -> new RuntimeException("Loan not found with ID: " + loanId));
        }
        
        List<UploadedDocument> uploadedDocuments = new ArrayList<>();
        
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String documentType = i < documentTypes.size() ? documentTypes.get(i) : "OTHER";
            
            if (!file.isEmpty()) {
                UploadedDocument document = uploadSingleDocument(applicant, loan, file, documentType);
                uploadedDocuments.add(document);
            }
        }
        
        return uploadedDocuments;
    }
    
    @Override
    public UploadedDocument uploadDocument(Long applicantId, Long loanId, 
                                         MultipartFile file, String documentType) throws IOException {
        
        // Validate applicant exists
        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("Applicant not found with ID: " + applicantId));
        
        // Validate loan exists if provided
        ApplicantLoanDetails loan = null;
        if (loanId != null) {
            loan = loanDetailsRepository.findById(loanId)
                    .orElseThrow(() -> new RuntimeException("Loan not found with ID: " + loanId));
        }
        
        return uploadSingleDocument(applicant, loan, file, documentType);
    }
    
    private UploadedDocument uploadSingleDocument(Applicant applicant, ApplicantLoanDetails loan, 
                                                MultipartFile file, String documentType) throws IOException {
        
        // Validate file
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }
        
        // Validate file size (max 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new RuntimeException("File size exceeds maximum limit of 10MB");
        }
        
        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !isValidFileType(contentType)) {
            throw new RuntimeException("Invalid file type. Only PDF, JPG, JPEG, PNG files are allowed");
        }
        
        try {
            // Upload to Cloudinary
            Map<String, Object> uploadResult = cloudinaryService.uploadDocumentWithDetails(
                    file, documentType, applicant.getApplicantId().toString());
            
            // Create document record
            UploadedDocument document = new UploadedDocument();
            document.setApplicant(applicant);
            document.setLoan(loan);
            document.setDocumentType(documentType);
            document.setDocumentName(generateDocumentName(documentType, applicant.getApplicantId()));
            document.setOriginalFilename(file.getOriginalFilename());
            document.setCloudinaryUrl((String) uploadResult.get("secure_url"));
            document.setCloudinaryPublicId((String) uploadResult.get("public_id"));
            document.setFileSize(file.getSize());
            document.setMimeType(file.getContentType());
            document.setUploadStatus(UploadedDocument.UploadStatus.UPLOADED);
            document.setVerificationStatus(UploadedDocument.VerificationStatus.PENDING);
            
            return uploadedDocumentRepository.save(document);
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload document: " + e.getMessage(), e);
        }
    }
    
    @Override
    public List<UploadedDocument> getDocumentsByApplicant(Long applicantId) {
        return uploadedDocumentRepository.findByApplicant_ApplicantId(applicantId);
    }
    
    @Override
    public List<UploadedDocument> getDocumentsByLoan(Long loanId) {
        return uploadedDocumentRepository.findByLoan_LoanId(loanId);
    }
    
    @Override
    public boolean deleteDocument(Long documentId) {
        try {
            UploadedDocument document = uploadedDocumentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document not found with ID: " + documentId));
            
            // Delete from Cloudinary
            boolean cloudinaryDeleted = cloudinaryService.deleteFile(document.getCloudinaryPublicId());
            
            // Delete from database
            uploadedDocumentRepository.delete(document);
            
            return cloudinaryDeleted;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public UploadedDocument updateVerificationStatus(Long documentId, String verificationStatus, 
                                                   String verificationNotes, String verifiedBy) {
        UploadedDocument document = uploadedDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found with ID: " + documentId));
        
        // Convert string to enum
        UploadedDocument.VerificationStatus status;
        try {
            status = UploadedDocument.VerificationStatus.valueOf(verificationStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid verification status: " + verificationStatus);
        }
        
        document.setVerificationStatus(status);
        document.setVerificationNotes(verificationNotes);
        document.setVerifiedBy(verifiedBy);
        document.setVerifiedAt(LocalDateTime.now());
        
        return uploadedDocumentRepository.save(document);
    }
    
    @Override
    public Map<String, Object> getDocumentUploadSummary(Long applicantId) {
        Long totalDocuments = uploadedDocumentRepository.countDocumentsByApplicantId(applicantId);
        Long verifiedDocuments = uploadedDocumentRepository.countVerifiedDocumentsByApplicantId(applicantId);
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalDocuments", totalDocuments);
        summary.put("verifiedDocuments", verifiedDocuments);
        summary.put("pendingDocuments", totalDocuments - verifiedDocuments);
        summary.put("completionPercentage", totalDocuments > 0 ? (verifiedDocuments * 100.0 / totalDocuments) : 0);
        
        return summary;
    }
    
    private boolean isValidFileType(String contentType) {
        return contentType.equals("application/pdf") ||
               contentType.equals("image/jpeg") ||
               contentType.equals("image/jpg") ||
               contentType.equals("image/png");
    }
    
    private String generateDocumentName(String documentType, Long applicantId) {
        return documentType.toUpperCase() + "_" + applicantId + "_" + System.currentTimeMillis();
    }
}
