package com.tss.springsecurity.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
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
    
    @Min(value = 300, message = "Credit score must be at least 300")
    @Max(value = 900, message = "Credit score cannot exceed 900")
    @Column(name = "credit_score")
    private Integer creditScore;
    
    @Pattern(regexp = "CIBIL|Experian|Equifax|CRIF|TransUnion", message = "Credit bureau must be CIBIL, Experian, Equifax, CRIF, or TransUnion")
    @Column(name = "credit_bureau", length = 50)
    private String creditBureau; // CIBIL, Experian, Equifax, CRIF
    
    @Min(value = 0, message = "Total active loans cannot be negative")
    @Max(value = 50, message = "Total active loans cannot exceed 50")
    @Column(name = "total_active_loans")
    private Integer totalActiveLoans = 0;
    
    @DecimalMin(value = "0.0", message = "Total outstanding debt cannot be negative")
    @DecimalMax(value = "1000000000.0", message = "Total outstanding debt cannot exceed ₹100 crores")
    @Digits(integer = 13, fraction = 2, message = "Total outstanding debt must have at most 13 digits before decimal and 2 after")
    @Column(name = "total_outstanding_debt", precision = 15, scale = 2)
    private BigDecimal totalOutstandingDebt;
    
    @DecimalMin(value = "0.0", message = "Total monthly EMI cannot be negative")
    @DecimalMax(value = "10000000.0", message = "Total monthly EMI cannot exceed ₹1 crore")
    @Digits(integer = 10, fraction = 2, message = "Total monthly EMI must have at most 10 digits before decimal and 2 after")
    @Column(name = "total_monthly_emi", precision = 12, scale = 2)
    private BigDecimal totalMonthlyEmi;
    
    @Min(value = 0, message = "Credit card count cannot be negative")
    @Max(value = 20, message = "Credit card count cannot exceed 20")
    @Column(name = "credit_card_count")
    private Integer creditCardCount = 0;
    
    @DecimalMin(value = "0.0", message = "Total credit limit cannot be negative")
    @DecimalMax(value = "100000000.0", message = "Total credit limit cannot exceed ₹10 crores")
    @Digits(integer = 13, fraction = 2, message = "Total credit limit must have at most 13 digits before decimal and 2 after")
    @Column(name = "total_credit_limit", precision = 15, scale = 2)
    private BigDecimal totalCreditLimit;
    
    @DecimalMin(value = "0.0", message = "Credit utilization ratio cannot be negative")
    @DecimalMax(value = "100.0", message = "Credit utilization ratio cannot exceed 100%")
    @Digits(integer = 3, fraction = 2, message = "Credit utilization ratio must have at most 3 digits before decimal and 2 after")
    @Column(name = "credit_utilization_ratio", precision = 5, scale = 2)
    private BigDecimal creditUtilizationRatio;
    
    @Pattern(regexp = "excellent|good|fair|poor|no_history", message = "Payment history must be excellent, good, fair, poor, or no_history")
    @Column(name = "payment_history", length = 50)
    private String paymentHistory; // excellent, good, fair, poor
    
    @Min(value = 0, message = "Defaults count cannot be negative")
    @Max(value = 100, message = "Defaults count cannot exceed 100")
    @Column(name = "defaults_count")
    private Integer defaultsCount = 0;
    
    @Column(name = "bankruptcy_filed")
    private Boolean bankruptcyFiled = false;
    
    @Size(max = 500, message = "Credit report URL must not exceed 500 characters")
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
