package com.tss.springsecurity.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "applicant_credit_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicantCreditHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", referencedColumnName = "applicant_id")
    private Applicant applicant;
    
    @Column(name = "credit_score")
    private Integer creditScore;
    
    @Column(name = "credit_bureau", length = 50)
    private String creditBureau; // CIBIL, Experian, Equifax, CRIF
    
    @Column(name = "total_active_loans")
    private Integer totalActiveLoans = 0;
    
    @Column(name = "total_outstanding_debt", precision = 15, scale = 2)
    private BigDecimal totalOutstandingDebt;
    
    @Column(name = "total_monthly_emi", precision = 12, scale = 2)
    private BigDecimal totalMonthlyEmi;
    
    @Column(name = "credit_card_count")
    private Integer creditCardCount = 0;
    
    @Column(name = "total_credit_limit", precision = 15, scale = 2)
    private BigDecimal totalCreditLimit;
    
    @Column(name = "credit_utilization_ratio", precision = 5, scale = 2)
    private BigDecimal creditUtilizationRatio;
    
    @Column(name = "payment_history", length = 50)
    private String paymentHistory; // excellent, good, fair, poor
    
    @Column(name = "defaults_count")
    private Integer defaultsCount = 0;
    
    @Column(name = "bankruptcy_filed")
    private Boolean bankruptcyFiled = false;
    
    @Column(name = "credit_report_url", columnDefinition = "TEXT")
    private String creditReportUrl;
    
    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
