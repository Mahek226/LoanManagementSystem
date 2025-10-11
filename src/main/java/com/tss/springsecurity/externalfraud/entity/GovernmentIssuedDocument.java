package com.tss.springsecurity.externalfraud.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "government_issued_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GovernmentIssuedDocument {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "person_id")
    private Long personId;
    
    @Column(name = "document_type")
    private String documentType; // PAN, AADHAAR, PASSPORT, VOTER_ID, DL
    
    @Column(name = "document_number")
    private String documentNumber;
    
    @Column(name = "issued_date")
    private LocalDate issuedDate;
    
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    
    @Column(name = "issuing_authority")
    private String issuingAuthority;
    
    @Column(name = "verification_status")
    private String verificationStatus; // VERIFIED, UNVERIFIED, EXPIRED
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", insertable = false, updatable = false)
    private Person person;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
