package com.tss.springsecurity.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fraud_rule_definition", indexes = {
    @Index(name = "idx_fraud_rule_code", columnList = "rule_code", unique = true),
    @Index(name = "idx_fraud_rule_category", columnList = "rule_category"),
    @Index(name = "idx_fraud_rule_active", columnList = "is_active"),
    @Index(name = "idx_fraud_rule_severity", columnList = "severity"),
    @Index(name = "idx_fraud_rule_execution_order", columnList = "execution_order")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FraudRuleDefinition {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Long ruleId;
    
    @Column(name = "rule_code", unique = true, nullable = false, length = 100)
    private String ruleCode;
    
    @Column(name = "rule_name", nullable = false)
    private String ruleName;
    
    @Column(name = "rule_description", columnDefinition = "TEXT")
    private String ruleDescription;
    
    @Column(name = "rule_category", nullable = false, length = 50)
    private String ruleCategory; // IDENTITY, FINANCIAL, EMPLOYMENT, CROSS_VERIFICATION
    
    @Column(name = "severity", nullable = false, length = 20)
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    
    @Column(name = "fraud_points", nullable = false)
    private Integer fraudPoints = 0;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "rule_type", length = 50)
    private String ruleType; // THRESHOLD, PATTERN_MATCH, DUPLICATE_CHECK, CROSS_CHECK
    
    @Column(name = "execution_order")
    private Integer executionOrder = 100;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    @Column(name = "updated_by", length = 100)
    private String updatedBy;
    

    
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
