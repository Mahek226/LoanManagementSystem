package com.tss.springsecurity.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "passport_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PassportDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", referencedColumnName = "applicant_id")
    private Applicant applicant;
    
    @Column(name = "passport_number", length = 20)
    private String passportNumber;
    
    @Column(name = "name", length = 150)
    private String name;
    
    @Column(name = "dob")
    private LocalDate dob;
    
    @Column(name = "nationality", length = 50)
    private String nationality;
    
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    
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
