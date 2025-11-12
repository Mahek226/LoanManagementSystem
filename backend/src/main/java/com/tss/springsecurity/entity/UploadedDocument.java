package com.tss.springsecurity.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "uploaded_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadedDocument {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Long documentId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", referencedColumnName = "applicant_id")
    private Applicant applicant;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", referencedColumnName = "loan_id")
    private ApplicantLoanDetails loan;
    
    @Column(name = "document_type", length = 100, nullable = false)
    private String documentType;
    
    @Column(name = "document_name", length = 255, nullable = false)
    private String documentName;
    
    @Column(name = "original_filename", length = 255)
    private String originalFilename;
    
    @Column(name = "cloudinary_url", columnDefinition = "TEXT", nullable = false)
    private String cloudinaryUrl;
    
    @Column(name = "cloudinary_public_id", length = 255)
    private String cloudinaryPublicId;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "mime_type", length = 100)
    private String mimeType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "upload_status")
    private UploadStatus uploadStatus = UploadStatus.UPLOADED;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status")
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;
    
    @Column(name = "verification_notes", columnDefinition = "TEXT")
    private String verificationNotes;
    
    // Extracted data fields
    @Column(name = "extracted_text", columnDefinition = "LONGTEXT")
    private String extractedText;
    
    @Column(name = "extracted_json", columnDefinition = "JSON")
    private String extractedJson;
    
    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;
    
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    @Column(name = "verified_by")
    private String verifiedBy;
    
    // Resubmission fields for compliance officer
    @Column(name = "resubmission_requested")
    private Boolean resubmissionRequested = false;
    
    @Column(name = "resubmission_reason", columnDefinition = "TEXT")
    private String resubmissionReason;
    
    @Column(name = "resubmission_instructions", columnDefinition = "TEXT")
    private String resubmissionInstructions;
    
    // New resubmission tracking fields
    @Column(name = "is_resubmission")
    private Boolean isResubmission = false;
    
    @Column(name = "original_notification_id")
    private Long originalNotificationId;
    
    @Column(name = "assignment_id")
    private Long assignmentId;
    
    @Column(name = "applicant_comments", columnDefinition = "TEXT")
    private String applicantComments;
    
    // Relationship to extracted fields
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ExtractedField> extractedFields;
    
    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
    
    // Enum definitions
    public enum UploadStatus {
        UPLOADED, FAILED, PROCESSED
    }
    
    public enum VerificationStatus {
        PENDING, VERIFIED, REJECTED, RESUBMISSION_REQUESTED
    }
}
