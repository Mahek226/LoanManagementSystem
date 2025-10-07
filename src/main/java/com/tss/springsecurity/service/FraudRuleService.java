//package com.tss.springsecurity.service;
//
//import com.tss.springsecurity.entity.*;
//import com.tss.springsecurity.repository.*;
//import jakarta.transaction.Transactional;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.cache.annotation.CacheEvict;
//import org.springframework.cache.annotation.Cacheable;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@Service
//@Slf4j
//public class FraudRuleService {
//    
//    private final FraudRuleDefinitionRepository ruleRepository;
//    private final FraudRuleParameterRepository parameterRepository;
//    private final FraudRuleAuditRepository auditRepository;
//    private final FraudRuleExecutionLogRepository executionLogRepository;
//    
//    public FraudRuleService(
//            FraudRuleDefinitionRepository ruleRepository,
//            FraudRuleParameterRepository parameterRepository,
//            FraudRuleAuditRepository auditRepository,
//            FraudRuleExecutionLogRepository executionLogRepository) {
//        this.ruleRepository = ruleRepository;
//        this.parameterRepository = parameterRepository;
//        this.auditRepository = auditRepository;
//        this.executionLogRepository = executionLogRepository;
//    }
//    
//    /**
//     * Get all active rules (cached for performance)
//     */
//    @Cacheable(value = "fraudRules", key = "'all'")
//    public List<FraudRuleDefinition> getAllActiveRules() {
//        log.debug("Fetching all active fraud rules from database");
//        return ruleRepository.findByIsActiveTrueOrderByExecutionOrderAsc();
//    }
//    
//    /**
//     * Get active rules by category (cached)
//     */
//    @Cacheable(value = "fraudRules", key = "#category")
//    public List<FraudRuleDefinition> getActiveRulesByCategory(String category) {
//        log.debug("Fetching active fraud rules for category: {}", category);
//        return ruleRepository.findByCategoryWithAllRelations(category);
//    }
//    
//    /**
//     * Get rule by code
//     */
//    @Cacheable(value = "fraudRules", key = "'code_' + #ruleCode")
//    public FraudRuleDefinition getRuleByCode(String ruleCode) {
//        return ruleRepository.findByRuleCode(ruleCode)
//                .orElseThrow(() -> new RuntimeException("Rule not found: " + ruleCode));
//    }
//    
//    /**
//     * Get rules as a map for quick lookup
//     */
//    @Cacheable(value = "fraudRules", key = "'map_' + #category")
//    public Map<String, FraudRuleDefinition> getRulesMapByCategory(String category) {
//        return getActiveRulesByCategory(category).stream()
//                .collect(Collectors.toMap(FraudRuleDefinition::getRuleCode, rule -> rule));
//    }
//    
//    /**
//     * Update fraud points for a rule
//     */
//    @Transactional
//    @CacheEvict(value = "fraudRules", allEntries = true)
//    public FraudRuleDefinition updateFraudPoints(Long ruleId, Integer newPoints, String updatedBy, String reason) {
//        FraudRuleDefinition rule = ruleRepository.findById(ruleId)
//                .orElseThrow(() -> new RuntimeException("Rule not found: " + ruleId));
//        
//        Integer oldPoints = rule.getFraudPoints();
//        rule.setFraudPoints(newPoints);
//        rule.setUpdatedBy(updatedBy);
//        
//        // Save audit trail
//        FraudRuleAudit audit = new FraudRuleAudit();
//        audit.setRule(rule);
//        audit.setChangeType("POINTS_CHANGED");
//        audit.setFieldName("fraud_points");
//        audit.setOldValue(oldPoints.toString());
//        audit.setNewValue(newPoints.toString());
//        audit.setChangedBy(updatedBy);
//        audit.setChangeReason(reason);
//        auditRepository.save(audit);
//        
//        log.info("Updated fraud points for rule {} from {} to {} by {}", 
//                rule.getRuleCode(), oldPoints, newPoints, updatedBy);
//        
//        return ruleRepository.save(rule);
//    }
//    
//    /**
//     * Update rule parameter
//     */
//    @Transactional
//    @CacheEvict(value = "fraudRules", allEntries = true)
//    public FraudRuleParameter updateParameter(Long ruleId, String paramName, String newValue, 
//                                              String updatedBy, String reason) {
//        FraudRuleParameter parameter = parameterRepository
//                .findByRule_RuleIdAndParamName(ruleId, paramName)
//                .orElseThrow(() -> new RuntimeException("Parameter not found: " + paramName));
//        
//        String oldValue = parameter.getParamValue();
//        parameter.setParamValue(newValue);
//        
//        // Save audit trail
//        FraudRuleDefinition rule = ruleRepository.findById(ruleId)
//                .orElseThrow(() -> new RuntimeException("Rule not found: " + ruleId));
//        
//        FraudRuleAudit audit = new FraudRuleAudit();
//        audit.setRule(rule);
//        audit.setChangeType("PARAMETER_UPDATED");
//        audit.setFieldName(paramName);
//        audit.setOldValue(oldValue);
//        audit.setNewValue(newValue);
//        audit.setChangedBy(updatedBy);
//        audit.setChangeReason(reason);
//        auditRepository.save(audit);
//        
//        log.info("Updated parameter {} for rule {} from {} to {} by {}", 
//                paramName, rule.getRuleCode(), oldValue, newValue, updatedBy);
//        
//        return parameterRepository.save(parameter);
//    }
//    
//    /**
//     * Activate/Deactivate rule
//     */
//    @Transactional
//    @CacheEvict(value = "fraudRules", allEntries = true)
//    public FraudRuleDefinition toggleRuleStatus(Long ruleId, boolean activate, String updatedBy, String reason) {
//        FraudRuleDefinition rule = ruleRepository.findById(ruleId)
//                .orElseThrow(() -> new RuntimeException("Rule not found: " + ruleId));
//        
//        rule.setIsActive(activate);
//        rule.setUpdatedBy(updatedBy);
//        
//        // Save audit trail
//        FraudRuleAudit audit = new FraudRuleAudit();
//        audit.setRule(rule);
//        audit.setChangeType(activate ? "ACTIVATED" : "DEACTIVATED");
//        audit.setFieldName("is_active");
//        audit.setOldValue(String.valueOf(!activate));
//        audit.setNewValue(String.valueOf(activate));
//        audit.setChangedBy(updatedBy);
//        audit.setChangeReason(reason);
//        auditRepository.save(audit);
//        
//        log.info("Rule {} {} by {}", rule.getRuleCode(), 
//                activate ? "activated" : "deactivated", updatedBy);
//        
//        return ruleRepository.save(rule);
//    }
//    
//    /**
//     * Log rule execution
//     */
//    @Transactional
//    public void logRuleExecution(Long ruleId, Long applicantId, Long loanId, 
//                                 boolean wasTriggered, Integer fraudPoints, 
//                                 String details, Integer executionTimeMs) {
//        FraudRuleDefinition rule = ruleRepository.findById(ruleId)
//                .orElseThrow(() -> new RuntimeException("Rule not found: " + ruleId));
//        
//        FraudRuleExecutionLog log = new FraudRuleExecutionLog();
//        log.setRule(rule);
//        log.setApplicantId(applicantId);
//        log.setLoanId(loanId);
//        log.setWasTriggered(wasTriggered);
//        log.setFraudPointsAssigned(fraudPoints);
//        log.setRuleDetails(details);
//        log.setExecutionTimeMs(executionTimeMs);
//        
//        executionLogRepository.save(log);
//    }
//    
//    /**
//     * Get audit history for a rule
//     */
//    public List<FraudRuleAudit> getRuleAuditHistory(Long ruleId) {
//        return auditRepository.findByRule_RuleIdOrderByChangedAtDesc(ruleId);
//    }
//    
//    /**
//     * Get execution logs for an applicant
//     */
//    public List<FraudRuleExecutionLog> getApplicantExecutionLogs(Long applicantId) {
//        return executionLogRepository.findByApplicantIdOrderByExecutedAtDesc(applicantId);
//    }
//    
//    /**
//     * Get triggered rules for an applicant
//     */
//    public List<FraudRuleExecutionLog> getTriggeredRulesForApplicant(Long applicantId) {
//        return executionLogRepository.findByApplicantIdAndWasTriggeredTrue(applicantId);
//    }
//    
//    /**
//     * Clear cache manually
//     */
//    @CacheEvict(value = "fraudRules", allEntries = true)
//    public void clearCache() {
//        log.info("Fraud rules cache cleared");
//    }
//}
