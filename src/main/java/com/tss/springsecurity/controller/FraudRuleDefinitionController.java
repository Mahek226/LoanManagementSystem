package com.tss.springsecurity.controller;

import com.tss.springsecurity.entity.FraudRuleDefinition;
import com.tss.springsecurity.repository.FraudRuleDefinitionRepository;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for managing Fraud Rule Definitions
 * Allows admin to insert and manage fraud rules in the database
 */
@RestController
@RequestMapping("/api/admin/fraud-rule-definitions")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class FraudRuleDefinitionController {
    
    private final FraudRuleDefinitionRepository ruleRepository;
    
    public FraudRuleDefinitionController(FraudRuleDefinitionRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }
    
    /**
     * Insert a single fraud rule definition
     */
    @PostMapping
    public ResponseEntity<?> createFraudRule(
            @RequestBody FraudRuleDefinitionRequest request,
            Principal principal) {
        
        try {
            // Check if rule code already exists
            if (ruleRepository.existsByRuleCode(request.getRuleCode())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Rule code already exists: " + request.getRuleCode()));
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
            rule.setCreatedBy(principal != null ? principal.getName() : "SYSTEM");
            
            FraudRuleDefinition saved = ruleRepository.save(rule);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create rule: " + e.getMessage()));
        }
    }
    
    /**
     * Insert multiple fraud rule definitions in bulk
     */
    @PostMapping("/bulk")
    public ResponseEntity<?> createFraudRulesBulk(
            @RequestBody List<FraudRuleDefinitionRequest> requests,
            Principal principal) {
        
        try {
            List<FraudRuleDefinition> savedRules = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            
            for (FraudRuleDefinitionRequest request : requests) {
                try {
                    // Check if rule code already exists
                    if (ruleRepository.existsByRuleCode(request.getRuleCode())) {
                        errors.add("Rule code already exists: " + request.getRuleCode());
                        continue;
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
                    rule.setCreatedBy(principal != null ? principal.getName() : "SYSTEM");
                    
                    FraudRuleDefinition saved = ruleRepository.save(rule);
                    savedRules.add(saved);
                    
                } catch (Exception e) {
                    errors.add("Failed to create rule " + request.getRuleCode() + ": " + e.getMessage());
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("successCount", savedRules.size());
            response.put("errorCount", errors.size());
            response.put("savedRules", savedRules);
            if (!errors.isEmpty()) {
                response.put("errors", errors);
            }
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create rules: " + e.getMessage()));
        }
    }
    
    /**
     * Get all fraud rule definitions
     */
    @GetMapping
    public ResponseEntity<List<FraudRuleDefinition>> getAllRules() {
        return ResponseEntity.ok(ruleRepository.findAll());
    }
    
    /**
     * Get fraud rule by ID
     */
    @GetMapping("/{ruleId}")
    public ResponseEntity<?> getRuleById(@PathVariable Long ruleId) {
        return ruleRepository.findById(ruleId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get fraud rule by code
     */
    @GetMapping("/code/{ruleCode}")
    public ResponseEntity<?> getRuleByCode(@PathVariable String ruleCode) {
        return ruleRepository.findByRuleCode(ruleCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Update fraud rule definition
     */
    @PutMapping("/{ruleId}")
    public ResponseEntity<?> updateFraudRule(
            @PathVariable Long ruleId,
            @RequestBody FraudRuleDefinitionRequest request,
            Principal principal) {
        
        try {
            FraudRuleDefinition rule = ruleRepository.findById(ruleId)
                    .orElseThrow(() -> new RuntimeException("Rule not found with ID: " + ruleId));
            
            // Update fields
            if (request.getRuleName() != null) rule.setRuleName(request.getRuleName());
            if (request.getRuleDescription() != null) rule.setRuleDescription(request.getRuleDescription());
            if (request.getRuleCategory() != null) rule.setRuleCategory(request.getRuleCategory());
            if (request.getSeverity() != null) rule.setSeverity(request.getSeverity());
            if (request.getFraudPoints() != null) rule.setFraudPoints(request.getFraudPoints());
            if (request.getIsActive() != null) rule.setIsActive(request.getIsActive());
            if (request.getRuleType() != null) rule.setRuleType(request.getRuleType());
            if (request.getExecutionOrder() != null) rule.setExecutionOrder(request.getExecutionOrder());
            rule.setUpdatedBy(principal != null ? principal.getName() : "SYSTEM");
            
            FraudRuleDefinition updated = ruleRepository.save(rule);
            return ResponseEntity.ok(updated);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update rule: " + e.getMessage()));
        }
    }
    
    /**
     * Delete fraud rule definition
     */
    @DeleteMapping("/{ruleId}")
    public ResponseEntity<?> deleteFraudRule(@PathVariable Long ruleId) {
        try {
            if (!ruleRepository.existsById(ruleId)) {
                return ResponseEntity.notFound().build();
            }
            
            ruleRepository.deleteById(ruleId);
            return ResponseEntity.ok(Map.of("message", "Rule deleted successfully"));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete rule: " + e.getMessage()));
        }
    }
    
    // Request DTO
    @Data
    public static class FraudRuleDefinitionRequest {
        private String ruleCode;
        private String ruleName;
        private String ruleDescription;
        private String ruleCategory; // IDENTITY, FINANCIAL, EMPLOYMENT, CROSS_VERIFICATION
        private String severity; // LOW, MEDIUM, HIGH, CRITICAL
        private Integer fraudPoints;
        private Boolean isActive;
        private String ruleType; // THRESHOLD, PATTERN_MATCH, DUPLICATE_CHECK, CROSS_CHECK
        private Integer executionOrder;
    }
}
