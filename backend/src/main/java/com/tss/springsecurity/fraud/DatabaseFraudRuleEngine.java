package com.tss.springsecurity.fraud;

import com.tss.springsecurity.entity.FraudRuleDefinition;
import com.tss.springsecurity.repository.FraudRuleDefinitionRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Database-driven fraud rule engine
 * Fetches fraud rules from database instead of hardcoded values
 */
@Service
public class DatabaseFraudRuleEngine {
    
    private final FraudRuleDefinitionRepository ruleRepository;
    
    public DatabaseFraudRuleEngine(FraudRuleDefinitionRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }
    
    /**
     * Get all active rules for a specific category
     */
    public List<FraudRuleDefinition> getActiveRulesByCategory(String category) {
        return ruleRepository.findByRuleCategoryAndIsActiveTrueOrderByExecutionOrderAsc(category);
    }
    
    /**
     * Get a specific rule by code
     */
    public Optional<FraudRuleDefinition> getRuleByCode(String ruleCode) {
        return ruleRepository.findByRuleCode(ruleCode);
    }
    
    /**
     * Get all active rules
     */
    public List<FraudRuleDefinition> getAllActiveRules() {
        return ruleRepository.findByIsActiveTrueOrderByExecutionOrderAsc();
    }
    
    /**
     * Create a FraudRule from FraudRuleDefinition
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
     * Create a FraudRule with custom description
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
     * Check if a rule exists and is active
     */
    public boolean isRuleActive(String ruleCode) {
        return ruleRepository.findByRuleCode(ruleCode)
                .map(FraudRuleDefinition::getIsActive)
                .orElse(false);
    }
    
    /**
     * Get rule configuration as map for easy access
     */
    public Map<String, FraudRuleDefinition> getRulesAsMap(String category) {
        List<FraudRuleDefinition> rules = getActiveRulesByCategory(category);
        Map<String, FraudRuleDefinition> ruleMap = new HashMap<>();
        for (FraudRuleDefinition rule : rules) {
            ruleMap.put(rule.getRuleCode(), rule);
        }
        return ruleMap;
    }
    
    /**
     * Get all rules as map (code -> definition)
     */
    public Map<String, FraudRuleDefinition> getAllRulesAsMap() {
        List<FraudRuleDefinition> rules = getAllActiveRules();
        Map<String, FraudRuleDefinition> ruleMap = new HashMap<>();
        for (FraudRuleDefinition rule : rules) {
            ruleMap.put(rule.getRuleCode(), rule);
        }
        return ruleMap;
    }
}
