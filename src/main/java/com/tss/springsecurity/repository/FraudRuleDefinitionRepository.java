//package com.tss.springsecurity.repository;
//
//import com.tss.springsecurity.entity.FraudRuleDefinition;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//import java.util.Optional;
//
//@Repository
//public interface FraudRuleDefinitionRepository extends JpaRepository<FraudRuleDefinition, Long> {
//    
//    /**
//     * Find all active rules
//     */
//    List<FraudRuleDefinition> findByIsActiveTrueOrderByExecutionOrderAsc();
//    
//    /**
//     * Find active rules by category
//     */
//    List<FraudRuleDefinition> findByRuleCategoryAndIsActiveTrueOrderByExecutionOrderAsc(String ruleCategory);
//    
//    /**
//     * Find rule by code
//     */
//    Optional<FraudRuleDefinition> findByRuleCode(String ruleCode);
//    
//    /**
//     * Find rules by severity
//     */
//    List<FraudRuleDefinition> findBySeverityAndIsActiveTrue(String severity);
//    
//    /**
//     * Find rules by type
//     */
//    List<FraudRuleDefinition> findByRuleTypeAndIsActiveTrue(String ruleType);
//    
//    /**
//     * Find all active rules with their parameters
//     */
//    @Query("SELECT DISTINCT r FROM FraudRuleDefinition r " +
//           "LEFT JOIN FETCH r.parameters " +
//           "WHERE r.isActive = true " +
//           "ORDER BY r.executionOrder")
//    List<FraudRuleDefinition> findAllActiveRulesWithParameters();
//    
//    /**
//     * Find all active rules with parameters, patterns, and lists
//     */
//    @Query("SELECT DISTINCT r FROM FraudRuleDefinition r " +
//           "LEFT JOIN FETCH r.parameters " +
//           "LEFT JOIN FETCH r.patterns " +
//           "LEFT JOIN FETCH r.lists " +
//           "WHERE r.isActive = true " +
//           "ORDER BY r.executionOrder")
//    List<FraudRuleDefinition> findAllActiveRulesWithAllRelations();
//    
//    /**
//     * Find rules by category with all relations
//     */
//    @Query("SELECT DISTINCT r FROM FraudRuleDefinition r " +
//           "LEFT JOIN FETCH r.parameters " +
//           "LEFT JOIN FETCH r.patterns " +
//           "LEFT JOIN FETCH r.lists " +
//           "WHERE r.ruleCategory = :category AND r.isActive = true " +
//           "ORDER BY r.executionOrder")
//    List<FraudRuleDefinition> findByCategoryWithAllRelations(@Param("category") String category);
//    
//    /**
//     * Count active rules by category
//     */
//    long countByRuleCategoryAndIsActiveTrue(String ruleCategory);
//    
//    /**
//     * Check if rule code exists
//     */
//    boolean existsByRuleCode(String ruleCode);
//}
