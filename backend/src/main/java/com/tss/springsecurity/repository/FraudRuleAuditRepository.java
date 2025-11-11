package com.tss.springsecurity.repository;

import com.tss.springsecurity.entity.FraudRuleAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FraudRuleAuditRepository extends JpaRepository<FraudRuleAudit, Long> {
    
    List<FraudRuleAudit> findByRule_RuleIdOrderByChangedAtDesc(Long ruleId);
    
    List<FraudRuleAudit> findByChangedByOrderByChangedAtDesc(String changedBy);
    
    List<FraudRuleAudit> findByChangedAtBetweenOrderByChangedAtDesc(LocalDateTime start, LocalDateTime end);
    
    List<FraudRuleAudit> findByChangeTypeOrderByChangedAtDesc(String changeType);
}
