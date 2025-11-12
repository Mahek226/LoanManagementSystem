package com.tss.springsecurity.repository;

import com.tss.springsecurity.entity.UploadedDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UploadedDocumentRepository extends JpaRepository<UploadedDocument, Long> {
    
    /**
     * Find all documents for a specific applicant
     */
    List<UploadedDocument> findByApplicant_ApplicantId(Long applicantId);
    
    /**
     * Find all documents for a specific loan
     */
    List<UploadedDocument> findByLoan_LoanId(Long loanId);
    
    /**
     * Find documents by applicant and document type
     */
    List<UploadedDocument> findByApplicant_ApplicantIdAndDocumentType(Long applicantId, String documentType);
    
    /**
     * Find documents by verification status
     */
    List<UploadedDocument> findByVerificationStatus(UploadedDocument.VerificationStatus verificationStatus);
    
    /**
     * Find documents by upload status
     */
    List<UploadedDocument> findByUploadStatus(UploadedDocument.UploadStatus uploadStatus);
    
    /**
     * Count documents for an applicant
     */
    @Query("SELECT COUNT(ud) FROM UploadedDocument ud WHERE ud.applicant.applicantId = :applicantId")
    Long countDocumentsByApplicantId(@Param("applicantId") Long applicantId);
    
    /**
     * Count verified documents for an applicant
     */
    @Query("SELECT COUNT(ud) FROM UploadedDocument ud WHERE ud.applicant.applicantId = :applicantId AND ud.verificationStatus = com.tss.springsecurity.entity.UploadedDocument$VerificationStatus.VERIFIED")
    Long countVerifiedDocumentsByApplicantId(@Param("applicantId") Long applicantId);
    
    /**
     * Find documents by Cloudinary public ID
     */
    UploadedDocument findByCloudinaryPublicId(String cloudinaryPublicId);
    
    /**
     * Find documents by both applicant and loan to ensure only loan-specific documents are returned
     */
    @Query("SELECT ud FROM UploadedDocument ud WHERE ud.applicant.applicantId = :applicantId AND (ud.loan.loanId = :loanId OR ud.loan.loanId IS NULL)")
    List<UploadedDocument> findByApplicantIdAndLoanId(@Param("applicantId") Long applicantId, @Param("loanId") Long loanId);
    
    /**
     * Find documents by loan ID (alias method)
     */
    default List<UploadedDocument> findByLoanId(Long loanId) {
        return findByLoan_LoanId(loanId);
    }
    
    /**
     * Find documents by assignment ID and resubmission status
     */
    List<UploadedDocument> findByAssignmentIdAndIsResubmission(Long assignmentId, Boolean isResubmission);
    
    /**
     * Find all resubmitted documents
     */
    List<UploadedDocument> findByIsResubmissionTrue();
    
    /**
     * Find resubmitted documents by applicant
     */
    List<UploadedDocument> findByApplicant_ApplicantIdAndIsResubmissionTrue(Long applicantId);
}
