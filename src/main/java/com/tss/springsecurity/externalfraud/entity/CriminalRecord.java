package com.tss.springsecurity.externalfraud.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "criminal_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CriminalRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "person_id")
    private Long personId;
    
    @Column(name = "case_number")
    private String caseNumber;
    
    @Column(name = "case_type")
    private String caseType;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "court_name")
    private String courtName;
    
    @Column(name = "status")
    private String status; // OPEN, CLOSED, CONVICTED, ACQUITTED
    
    @Column(name = "verdict_date")
    private LocalDate verdictDate;
    
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
