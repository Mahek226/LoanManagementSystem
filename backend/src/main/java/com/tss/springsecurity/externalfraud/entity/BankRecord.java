package com.tss.springsecurity.externalfraud.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "person_id")
    private Long personId;
    
    @Column(name = "bank_name")
    private String bankName;
    
    @Column(name = "account_number")
    private String accountNumber;
    
    @Column(name = "account_type")
    private String accountType; // SAVINGS, CURRENT, LOAN, FIXED_DEPOSIT
    
    @Column(name = "balance_amount", precision = 18, scale = 2)
    private BigDecimal balanceAmount;
    
    @Column(name = "last_transaction_date")
    private LocalDate lastTransactionDate;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
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
