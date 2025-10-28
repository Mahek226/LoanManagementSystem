package com.tss.springsecurity.repository;

import com.tss.springsecurity.entity.FraudRuleDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FraudRuleDefinitionRepository extends JpaRepository<FraudRuleDefinition, Long> {
    
    /**
     * Find all active rules
     */
    List<FraudRuleDefinition> findByIsActiveTrueOrderByExecutionOrderAsc();
    
    /**
     * Find active rules by category
     */
    List<FraudRuleDefinition> findByRuleCategoryAndIsActiveTrueOrderByExecutionOrderAsc(String ruleCategory);
    
    /**
     * Find rule by code
     */
    Optional<FraudRuleDefinition> findByRuleCode(String ruleCode);
    
    /**
     * Find rules by severity
     */
    List<FraudRuleDefinition> findBySeverityAndIsActiveTrue(String severity);
    
    /**
     * Find rules by type
     */
    List<FraudRuleDefinition> findByRuleTypeAndIsActiveTrue(String ruleType);
    
    /**
     * Count active rules by category
     */
    long countByRuleCategoryAndIsActiveTrue(String ruleCategory);
    
    /**
     * Check if rule code exists
     */
    boolean existsByRuleCode(String ruleCode);
    
    /**
     * Find rules by category
     */
    List<FraudRuleDefinition> findByRuleCategory(String ruleCategory);
    
    /**
     * Count rules by severity
     */
    long countBySeverityAndIsActiveTrue(String severity);
}
