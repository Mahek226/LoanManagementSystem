package com.tss.springsecurity.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
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
    
    @Column(name = "monthly_income", precision = 15, scale = 2)
    private BigDecimal monthlyIncome;
    
    @Column(name = "other_income", precision = 15, scale = 2)
    private BigDecimal otherIncome;
    
    @Column(name = "monthly_expenses", precision = 15, scale = 2)
    private BigDecimal monthlyExpenses;
    
    @Column(name = "existing_loan_emi", precision = 15, scale = 2)
    private BigDecimal existingLoanEmi;
    
    @Column(name = "credit_card_outstanding", precision = 15, scale = 2)
    private BigDecimal creditCardOutstanding;
    
    @Size(min = 2, max = 150, message = "Bank name must be between 2 and 150 characters")
    @Column(name = "bank_name", length = 150)
    private String bankName;
    
    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "^[0-9]{9,18}$", message = "Account number must be 9-18 digits")
    @Column(name = "account_number", length = 50)
    private String accountNumber;
    
    @NotBlank(message = "Account type is required")
    @Pattern(regexp = "savings|current|salary|fixed_deposit|recurring_deposit", message = "Account type must be savings, current, salary, fixed_deposit, or recurring_deposit")
    @Column(name = "account_type", length = 50)
    private String accountType;
    
    @NotBlank(message = "IFSC code is required")
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC code format. Must be in format ABCD0123456")
    @Column(name = "ifsc_code", length = 20)
    private String ifscCode;
    
    @DecimalMin(value = "0.0", message = "Total credit cannot be negative")
    @DecimalMax(value = "100000000.0", message = "Total credit cannot exceed 10 crores")
    @Digits(integer = 13, fraction = 2, message = "Total credit must have at most 13 digits before decimal and 2 after")
    @Column(name = "total_credit_last_month", precision = 15, scale = 2)
    private BigDecimal totalCreditLastMonth;
    
    @DecimalMin(value = "0.0", message = "Total debit cannot be negative")
    @DecimalMax(value = "100000000.0", message = "Total debit cannot exceed 10 crores")
    @Digits(integer = 13, fraction = 2, message = "Total debit must have at most 13 digits before decimal and 2 after")
    @Column(name = "total_debit_last_month", precision = 15, scale = 2)
    private BigDecimal totalDebitLastMonth;
    
    @Size(max = 1000, message = "Anomalies description must not exceed 1000 characters")
    @Column(name = "anomalies", columnDefinition = "TEXT")
    private String anomalies;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
