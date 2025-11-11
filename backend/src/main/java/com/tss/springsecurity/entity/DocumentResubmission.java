package com.tss.springsecurity.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_resubmission")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResubmission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resubmission_id")
    private Long resubmissionId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private ComplianceOfficerApplicationAssignment assignment;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private Applicant applicant;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_officer_id", nullable = false)
    private ComplianceOfficer requestedByOfficer;
    
    @Column(name = "requested_documents", columnDefinition = "TEXT", nullable = false)
    private String requestedDocuments; // JSON array of document types
    
    @Column(name = "reason", columnDefinition = "TEXT", nullable = false)
    private String reason;
    
    @Column(name = "additional_comments", columnDefinition = "TEXT")
    private String additionalComments;
    
    @Column(name = "priority_level")
    private Integer priorityLevel = 1;
    
    @Column(name = "status", length = 50)
    private String status = "REQUESTED"; // REQUESTED, SUBMITTED, REVIEWED
    
    @Column(name = "requested_at", updatable = false)
    private LocalDateTime requestedAt;
    
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @Column(name = "processed_by", length = 200)
    private String processedBy;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        requestedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
