package com.tss.springsecurity.externalfraud.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "historical_and_current_loans")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalAndCurrentLoan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "person_id")
    private Long personId;
    
    @Column(name = "loan_type")
    private String loanType; // PERSONAL, HOME, AUTO, EDUCATION
    
    @Column(name = "institution_name")
    private String institutionName;
    
    @Column(name = "loan_amount", precision = 18, scale = 2)
    private BigDecimal loanAmount;
    
    @Column(name = "outstanding_balance", precision = 18, scale = 2)
    private BigDecimal outstandingBalance;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Column(name = "status")
    private String status; // ACTIVE, CLOSED, DEFAULTED
    
    @Column(name = "default_flag")
    private Boolean defaultFlag = false;
    
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
