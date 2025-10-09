package com.tss.springsecurity.fraud.service;

import com.tss.springsecurity.entity.FraudRuleDefinition;
import com.tss.springsecurity.fraud.FraudRule;
import com.tss.springsecurity.repository.FraudRuleDefinitionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer for fraud rule management
 * Handles all database operations related to fraud rules
 */
@Service
public class FraudRuleService {
    
    private final FraudRuleDefinitionRepository ruleRepository;
    
    public FraudRuleService(FraudRuleDefinitionRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }
    
    /**
     * Get all active rules by category
     */
    public List<FraudRuleDefinition> getActiveRulesByCategory(String category) {
        return ruleRepository.findByRuleCategoryAndIsActiveTrueOrderByExecutionOrderAsc(category);
    }
    
    /**
     * Get rules as map for efficient lookup
     */
    public Map<String, FraudRuleDefinition> getRulesAsMap(String category) {
        return getActiveRulesByCategory(category).stream()
                .collect(Collectors.toMap(FraudRuleDefinition::getRuleCode, rule -> rule));
    }
    
    /**
     * Get all active rules across all categories
     */
    public List<FraudRuleDefinition> getAllActiveRules() {
        return ruleRepository.findByIsActiveTrueOrderByExecutionOrderAsc();
    }
    
    /**
     * Get rule by code
     */
    public Optional<FraudRuleDefinition> getRuleByCode(String ruleCode) {
        return ruleRepository.findByRuleCode(ruleCode);
    }
    
    /**
     * Create FraudRule from FraudRuleDefinition
     */
    public FraudRule createFraudRule(FraudRuleDefinition definition, String flagDetails) {
        return new FraudRule(
            definition.getRuleName(),
            definition.getRuleDescription(),
            definition.getFraudPoints(),
            definition.getSeverity(),
            definition.getRuleCategory(),
            true,
            flagDetails
        );
    }
    
    /**
     * Create FraudRule with custom description
     */
    public FraudRule createFraudRule(FraudRuleDefinition definition, String customDescription, String flagDetails) {
        return new FraudRule(
            definition.getRuleName(),
            customDescription,
            definition.getFraudPoints(),
            definition.getSeverity(),
            definition.getRuleCategory(),
            true,
            flagDetails
        );
    }
    
    /**
     * Check if rule is active
     */
    public boolean isRuleActive(String ruleCode) {
        return getRuleByCode(ruleCode)
                .map(FraudRuleDefinition::getIsActive)
                .orElse(false);
    }
}
