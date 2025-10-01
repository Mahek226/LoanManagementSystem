package com.tss.springsecurity.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "applicant_financials")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicantFinancials {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", referencedColumnName = "applicant_id")
    private Applicant applicant;
    
    @Column(name = "bank_name", length = 150)
    private String bankName;
    
    @Column(name = "account_number", length = 50)
    private String accountNumber;
    
    @Column(name = "account_type", length = 50)
    private String accountType;
    
    @Column(name = "ifsc_code", length = 20)
    private String ifscCode;
    
    @Column(name = "total_credit_last_month", precision = 15, scale = 2)
    private BigDecimal totalCreditLastMonth;
    
    @Column(name = "total_debit_last_month", precision = 15, scale = 2)
    private BigDecimal totalDebitLastMonth;
    
    @Column(name = "anomalies", columnDefinition = "TEXT")
    private String anomalies;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
