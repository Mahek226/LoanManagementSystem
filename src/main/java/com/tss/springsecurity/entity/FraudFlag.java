package com.tss.springsecurity.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_flags", indexes = {
    @Index(name = "idx_fraud_flag_applicant_id", columnList = "applicant_id"),
    @Index(name = "idx_fraud_flag_severity", columnList = "severity"),
    @Index(name = "idx_fraud_flag_created_at", columnList = "created_at"),
    @Index(name = "idx_fraud_flag_rule_name", columnList = "rule_name")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FraudFlag {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", referencedColumnName = "applicant_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Applicant applicant;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", referencedColumnName = "loan_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ApplicantLoanDetails loan;
    
    @Column(name = "rule_name", length = 100)
    private String ruleName;
    
    @Column(name = "severity")
    private Integer severity;
    
    @Column(name = "flag_notes", columnDefinition = "TEXT")
    private String flagNotes;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
