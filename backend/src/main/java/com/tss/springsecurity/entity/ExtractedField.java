package com.tss.springsecurity.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "extracted_fields")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedField {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "field_id")
    private Long fieldId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", referencedColumnName = "document_id")
    private UploadedDocument document;
    
    @Column(name = "field_name", length = 255, nullable = false)
    private String fieldName;
    
    @Column(name = "field_value", columnDefinition = "TEXT")
    private String fieldValue;
    
    @Column(name = "confidence_score", precision = 5, scale = 4)
    private BigDecimal confidenceScore;
    
    @Column(name = "extraction_method", length = 100)
    private String extractionMethod;
    
    @Column(name = "verified", nullable = false)
    private Boolean verified = false;
    
    @Column(name = "verified_by", length = 255)
    private String verifiedBy;
    
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
