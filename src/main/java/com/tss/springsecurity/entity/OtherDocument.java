package com.tss.springsecurity.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "other_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtherDocument {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", referencedColumnName = "applicant_id")
    private Applicant applicant;
    
    @Column(name = "doc_type", length = 50)
    private String docType;
    
    @Column(name = "doc_number", length = 100)
    private String docNumber;
    
    @Column(name = "cloudinary_url", columnDefinition = "TEXT")
    private String cloudinaryUrl;
    
    @Column(name = "ocr_text", columnDefinition = "TEXT")
    private String ocrText;
    
    @Column(name = "is_tampered")
    private Boolean isTampered = false;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
