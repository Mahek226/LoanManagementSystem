package com.tss.springsecurity.service;

import com.tss.springsecurity.controller.FraudRuleManagementController.CreateRuleRequest;
import com.tss.springsecurity.controller.FraudRuleManagementController.UpdateRuleRequest;
import com.tss.springsecurity.entity.FraudRuleDefinition;
import com.tss.springsecurity.repository.FraudRuleDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FraudRuleManagementService {

    private final FraudRuleDefinitionRepository ruleRepository;

    public List<FraudRuleDefinition> getAllRules() {
        return ruleRepository.findAll();
    }

    public List<FraudRuleDefinition> getActiveRules() {
        return ruleRepository.findByIsActiveTrueOrderByExecutionOrderAsc();
    }

    public List<FraudRuleDefinition> getRulesByCategory(String category) {
        return ruleRepository.findByRuleCategory(category);
    }

    public FraudRuleDefinition getRuleById(Long ruleId) {
        return ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Rule not found with id: " + ruleId));
    }

    public FraudRuleDefinition getRuleByCode(String ruleCode) {
        return ruleRepository.findByRuleCode(ruleCode)
                .orElseThrow(() -> new RuntimeException("Rule not found with code: " + ruleCode));
    }

    public FraudRuleDefinition createRule(CreateRuleRequest request, String createdBy) {
        // Check if rule code already exists
        if (ruleRepository.findByRuleCode(request.getRuleCode()).isPresent()) {
            throw new RuntimeException("Rule code already exists: " + request.getRuleCode());
        }
        
        FraudRuleDefinition rule = new FraudRuleDefinition();
        rule.setRuleCode(request.getRuleCode());
        rule.setRuleName(request.getRuleName());
        rule.setRuleDescription(request.getRuleDescription());
        rule.setRuleCategory(request.getRuleCategory());
        rule.setSeverity(request.getSeverity());
        rule.setFraudPoints(request.getFraudPoints());
        rule.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        rule.setRuleType(request.getRuleType());
        rule.setExecutionOrder(request.getExecutionOrder() != null ? request.getExecutionOrder() : 100);
        rule.setCreatedBy(createdBy);
        rule.setUpdatedBy(createdBy);
        
        return ruleRepository.save(rule);
    }

    public FraudRuleDefinition updateRule(Long ruleId, UpdateRuleRequest request, String updatedBy) {
        FraudRuleDefinition rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Rule not found with id: " + ruleId));
        
        if (request.getRuleName() != null) rule.setRuleName(request.getRuleName());
        if (request.getRuleDescription() != null) rule.setRuleDescription(request.getRuleDescription());
        if (request.getRuleCategory() != null) rule.setRuleCategory(request.getRuleCategory());
        if (request.getSeverity() != null) rule.setSeverity(request.getSeverity());
        if (request.getFraudPoints() != null) rule.setFraudPoints(request.getFraudPoints());
        if (request.getIsActive() != null) rule.setIsActive(request.getIsActive());
        if (request.getRuleType() != null) rule.setRuleType(request.getRuleType());
        if (request.getExecutionOrder() != null) rule.setExecutionOrder(request.getExecutionOrder());
        
        rule.setUpdatedBy(updatedBy);
        
        return ruleRepository.save(rule);
    }

    public FraudRuleDefinition toggleRuleStatus(Long ruleId, String updatedBy) {
        FraudRuleDefinition rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Rule not found with id: " + ruleId));
        
        rule.setIsActive(!rule.getIsActive());
        rule.setUpdatedBy(updatedBy);
        
        return ruleRepository.save(rule);
    }

    public String deleteRule(Long ruleId) {
        FraudRuleDefinition rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Rule not found with id: " + ruleId));
        
        String ruleName = rule.getRuleName();
        ruleRepository.delete(rule);
        
        return ruleName;
    }

    public Map<String, Object> getRuleStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalRules = ruleRepository.count();
        long activeRules = ruleRepository.findByIsActiveTrueOrderByExecutionOrderAsc().size();
        long identityRules = ruleRepository.countByRuleCategoryAndIsActiveTrue("IDENTITY");
        long financialRules = ruleRepository.countByRuleCategoryAndIsActiveTrue("FINANCIAL");
        long employmentRules = ruleRepository.countByRuleCategoryAndIsActiveTrue("EMPLOYMENT");
        long crossVerificationRules = ruleRepository.countByRuleCategoryAndIsActiveTrue("CROSS_VERIFICATION");
        
        stats.put("totalRules", totalRules);
        stats.put("activeRules", activeRules);
        stats.put("inactiveRules", totalRules - activeRules);
        stats.put("identityRules", identityRules);
        stats.put("financialRules", financialRules);
        stats.put("employmentRules", employmentRules);
        stats.put("crossVerificationRules", crossVerificationRules);
        
        // Severity distribution
        stats.put("criticalRules", ruleRepository.countBySeverityAndIsActiveTrue("CRITICAL"));
        stats.put("highRules", ruleRepository.countBySeverityAndIsActiveTrue("HIGH"));
        stats.put("mediumRules", ruleRepository.countBySeverityAndIsActiveTrue("MEDIUM"));
        stats.put("lowRules", ruleRepository.countBySeverityAndIsActiveTrue("LOW"));
        
        return stats;
    }
}
