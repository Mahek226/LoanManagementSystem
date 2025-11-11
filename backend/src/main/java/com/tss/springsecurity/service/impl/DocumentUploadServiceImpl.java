package com.tss.springsecurity.service.impl;

import com.tss.springsecurity.entity.Applicant;
import com.tss.springsecurity.entity.ApplicantLoanDetails;
import com.tss.springsecurity.entity.UploadedDocument;
import com.tss.springsecurity.repository.ApplicantRepository;
import com.tss.springsecurity.repository.ApplicantLoanDetailsRepository;
import com.tss.springsecurity.repository.UploadedDocumentRepository;
import com.tss.springsecurity.service.CloudinaryService;
import com.tss.springsecurity.service.DocumentUploadService;
import com.tss.springsecurity.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
@Slf4j
public class DocumentUploadServiceImpl implements DocumentUploadService {
    
    @Autowired
    private UploadedDocumentRepository uploadedDocumentRepository;
    
    @Autowired
    private ApplicantRepository applicantRepository;
    
    @Autowired
    private ApplicantLoanDetailsRepository loanDetailsRepository;
    
    @Autowired
    private CloudinaryService cloudinaryService;
    
    @Autowired
    private EmailService emailService;
    
    @Override
    public List<UploadedDocument> uploadDocuments(Long applicantId, Long loanId, 
                                                List<MultipartFile> files, 
                                                List<String> documentTypes) throws IOException {
        
        // Validate applicant exists
        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("Applicant not found with ID: " + applicantId));
        
        // Validate loan exists if provided
        // If loanId is null, documents will be auto-linked when loan is created
        ApplicantLoanDetails loan = null;
        if (loanId != null) {
            loan = loanDetailsRepository.findById(loanId)
                    .orElseThrow(() -> new RuntimeException("Loan not found with ID: " + loanId));
            log.info("Documents will be linked to loan ID: {}", loanId);
        } else {
            log.info("No loan ID provided. Documents will be auto-linked when loan application is submitted.");
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
        // If loanId is null, document will be auto-linked when loan is created
        ApplicantLoanDetails loan = null;
        if (loanId != null) {
            loan = loanDetailsRepository.findById(loanId)
                    .orElseThrow(() -> new RuntimeException("Loan not found with ID: " + loanId));
            log.info("Document will be linked to loan ID: {}", loanId);
        } else {
            log.info("No loan ID provided. Document will be auto-linked when loan application is submitted.");
        }
        
        return uploadSingleDocument(applicant, loan, file, documentType);
    }
    
    private UploadedDocument uploadSingleDocument(Applicant applicant, ApplicantLoanDetails loan, 
                                                MultipartFile file, String documentType) throws IOException {
        
        log.info("Starting document upload - File: {}, Type: {}, Size: {}, ContentType: {}", 
                file.getOriginalFilename(), documentType, file.getSize(), file.getContentType());
        
        // Validate file
        if (file.isEmpty()) {
            log.error("File is empty: {}", file.getOriginalFilename());
            throw new RuntimeException("File is empty");
        }
        
        // Validate file size (max 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            log.error("File size exceeds limit: {} bytes", file.getSize());
            throw new RuntimeException("File size exceeds maximum limit of 10MB");
        }
        
        // Validate file type
        String contentType = file.getContentType();
        boolean isValidByContentType = contentType != null && isValidFileType(contentType);
        boolean isValidByExtension = isValidFileExtension(file.getOriginalFilename());
        
        // Accept file if either content type OR file extension is valid
        if (!isValidByContentType && !isValidByExtension) {
            log.error("Invalid file type - ContentType: {}, File: {}, Extension check: {}", 
                    contentType, file.getOriginalFilename(), isValidByExtension);
            throw new RuntimeException("Invalid file type: " + contentType + 
                    ". Only PDF, JPG, JPEG, PNG files are allowed. File: " + file.getOriginalFilename());
        }
        
        if (!isValidByContentType) {
            log.warn("File accepted by extension only - ContentType: {}, File: {}", 
                    contentType, file.getOriginalFilename());
        }
        
        log.info("File validation passed for: {}", file.getOriginalFilename());
        
        try {
            // Upload to Cloudinary
            log.info("Uploading to Cloudinary: {}", file.getOriginalFilename());
            Map<String, Object> uploadResult = cloudinaryService.uploadDocumentWithDetails(
                    file, documentType, applicant.getApplicantId().toString());
            log.info("Cloudinary upload successful. URL: {}", uploadResult.get("secure_url"));
            
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
            
            UploadedDocument savedDocument = uploadedDocumentRepository.save(document);
            log.info("Document saved to database with ID: {}", savedDocument.getDocumentId());
            
            return savedDocument;
            
        } catch (IOException e) {
            log.error("IOException during document upload: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload document: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during document upload: {}", e.getMessage(), e);
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
        
        UploadedDocument savedDocument = uploadedDocumentRepository.save(document);
        
        // Send email notification to applicant about document verification status
        try {
            Applicant applicant = document.getApplicant();
            String applicantName = applicant.getFirstName() + " " + applicant.getLastName();
            emailService.sendDocumentVerificationEmail(
                applicant.getEmail(),
                applicantName,
                document.getDocumentType(),
                status.toString()
            );
            log.info("Document verification email sent to: {} for document type: {}", 
                    applicant.getEmail(), document.getDocumentType());
        } catch (Exception e) {
            log.error("Failed to send document verification email for document ID: {}", documentId, e);
        }
        
        return savedDocument;
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
    
    @Override
    public List<UploadedDocument> getAllDocuments() {
        return uploadedDocumentRepository.findAll();
    }
    
    private boolean isValidFileType(String contentType) {
        if (contentType == null) {
            return false;
        }
        
        // Normalize content type by converting to lowercase and removing any parameters
        String normalizedType = contentType.toLowerCase().split(";")[0].trim();
        
        log.info("Validating file type - Original: {}, Normalized: {}", contentType, normalizedType);
        
        // Make validation more flexible by checking if content type starts with allowed types
        return normalizedType.equals("application/pdf") ||
               normalizedType.startsWith("image/jpeg") ||
               normalizedType.startsWith("image/jpg") ||
               normalizedType.startsWith("image/png") ||
               normalizedType.equals("image/pjpeg") || // Progressive JPEG
               normalizedType.equals("image/x-png") ||  // Alternative PNG MIME type
               normalizedType.equals("image/jpe") ||    // Another JPEG variant
               // Also check by file extension pattern in content type
               normalizedType.contains("jpeg") ||
               normalizedType.contains("jpg") ||
               normalizedType.contains("png") ||
               normalizedType.contains("pdf");
    }
    
    private boolean isValidFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }
        
        String lowerFilename = filename.toLowerCase();
        boolean isValid = lowerFilename.endsWith(".pdf") ||
                         lowerFilename.endsWith(".jpg") ||
                         lowerFilename.endsWith(".jpeg") ||
                         lowerFilename.endsWith(".png") ||
                         lowerFilename.endsWith(".jpe");
        
        log.info("File extension validation for '{}': {}", filename, isValid);
        return isValid;
    }
    
    private String generateDocumentName(String documentType, Long applicantId) {
        return documentType.toUpperCase() + "_" + applicantId + "_" + System.currentTimeMillis();
    }
}
