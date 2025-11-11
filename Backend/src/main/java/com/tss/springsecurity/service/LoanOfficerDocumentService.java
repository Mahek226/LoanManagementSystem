package com.tss.springsecurity.service;

import com.tss.springsecurity.entity.*;
import com.tss.springsecurity.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
}
