package com.tss.springsecurity.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "forwarded_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForwardedDocument {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "forwarded_id")
    private Long forwardedId;
    
    @Column(name = "document_id", nullable = false)
    private Long documentId;
    
    @Column(name = "document_type", nullable = false)
    private String documentType;
    
    @Column(name = "document_name")
    private String documentName;
    
    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;
    
    @Column(name = "applicant_name", nullable = false)
    private String applicantName;
    
    @Column(name = "loan_id", nullable = false)
    private Long loanId;
    
    @Column(name = "loan_type")
    private String loanType;
    
    @Column(name = "loan_amount")
    private Double loanAmount;
    
    @Column(name = "forwarded_by_officer_id", nullable = false)
    private Long forwardedByOfficerId;
    
    @Column(name = "forwarded_to_compliance_officer_id")
    private Long forwardedToComplianceOfficerId;
    
    @Column(name = "forwarded_at", nullable = false)
    private LocalDateTime forwardedAt;
    
    @Column(name = "status", nullable = false)
    private String status = "FORWARDED";
    
    @Column(name = "reason")
    private String reason;
    
    @Column(name = "priority_level")
    private Integer priorityLevel = 3;
    
    @PrePersist
    protected void onCreate() {
        if (forwardedAt == null) {
            forwardedAt = LocalDateTime.now();
        }
    }
}
