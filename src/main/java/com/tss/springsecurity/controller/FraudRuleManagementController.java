package com.tss.springsecurity.controller;

import com.tss.springsecurity.entity.FraudRuleDefinition;
import com.tss.springsecurity.repository.FraudRuleDefinitionRepository;
import com.tss.springsecurity.service.ActivityLogService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/fraud-rules")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"}, allowCredentials = "true")
@RequiredArgsConstructor
public class FraudRuleManagementController {
    
    private final FraudRuleDefinitionRepository ruleRepository;
    private final ActivityLogService activityLogService;
    
    /**
     * Get all fraud rules
     */
    @GetMapping
    public ResponseEntity<List<FraudRuleDefinition>> getAllRules() {
        List<FraudRuleDefinition> rules = ruleRepository.findAll();
        return ResponseEntity.ok(rules);
    }
    
    /**
     * Get all active rules
     */
    @GetMapping("/active")
    public ResponseEntity<List<FraudRuleDefinition>> getActiveRules() {
        List<FraudRuleDefinition> rules = ruleRepository.findByIsActiveTrueOrderByExecutionOrderAsc();
        return ResponseEntity.ok(rules);
    }
    
    /**
     * Get rules by category
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<FraudRuleDefinition>> getRulesByCategory(@PathVariable String category) {
        List<FraudRuleDefinition> rules = ruleRepository.findByRuleCategory(category);
        return ResponseEntity.ok(rules);
    }
    
    /**
     * Get rule by ID
     */
    @GetMapping("/{ruleId}")
    public ResponseEntity<?> getRuleById(@PathVariable Long ruleId) {
        Optional<FraudRuleDefinition> rule = ruleRepository.findById(ruleId);
        if (rule.isPresent()) {
            return ResponseEntity.ok(rule.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Rule not found with id: " + ruleId));
    }
    
    /**
     * Get rule by code
     */
    @GetMapping("/code/{ruleCode}")
    public ResponseEntity<?> getRuleByCode(@PathVariable String ruleCode) {
        Optional<FraudRuleDefinition> rule = ruleRepository.findByRuleCode(ruleCode);
        if (rule.isPresent()) {
            return ResponseEntity.ok(rule.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Rule not found with code: " + ruleCode));
    }
    
    /**
     * Create new fraud rule
     */
    @PostMapping
    public ResponseEntity<?> createRule(@Valid @RequestBody CreateRuleRequest request, Principal principal) {
        try {
            // Check if rule code already exists
            if (ruleRepository.findByRuleCode(request.getRuleCode()).isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
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
            rule.setCreatedBy(principal.getName());
            rule.setUpdatedBy(principal.getName());
            
            FraudRuleDefinition savedRule = ruleRepository.save(rule);
            
            // Log activity
            activityLogService.logActivity(
                    principal.getName(),
                    "ADMIN",
                    "CREATE",
                    "FRAUD_RULE",
                    savedRule.getRuleId(),
                    "Created new fraud rule: " + savedRule.getRuleName()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(savedRule);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create rule: " + e.getMessage()));
        }
    }
    
    /**
     * Update fraud rule
     */
    @PutMapping("/{ruleId}")
    public ResponseEntity<?> updateRule(@PathVariable Long ruleId,
                                       @Valid @RequestBody UpdateRuleRequest request,
                                       Principal principal) {
        try {
            FraudRuleDefinition rule = ruleRepository.findById(ruleId)
                    .orElseThrow(() -> new RuntimeException("Rule not found with id: " + ruleId));
            
            String oldValue = rule.toString();
            
            if (request.getRuleName() != null) rule.setRuleName(request.getRuleName());
            if (request.getRuleDescription() != null) rule.setRuleDescription(request.getRuleDescription());
            if (request.getRuleCategory() != null) rule.setRuleCategory(request.getRuleCategory());
            if (request.getSeverity() != null) rule.setSeverity(request.getSeverity());
            if (request.getFraudPoints() != null) rule.setFraudPoints(request.getFraudPoints());
            if (request.getIsActive() != null) rule.setIsActive(request.getIsActive());
            if (request.getRuleType() != null) rule.setRuleType(request.getRuleType());
            if (request.getExecutionOrder() != null) rule.setExecutionOrder(request.getExecutionOrder());
            
            rule.setUpdatedBy(principal.getName());
            
            FraudRuleDefinition updatedRule = ruleRepository.save(rule);
            
            // Log activity
            activityLogService.logActivityWithValues(
                    principal.getName(),
                    "ADMIN",
                    "UPDATE",
                    "FRAUD_RULE",
                    updatedRule.getRuleId(),
                    "Updated fraud rule: " + updatedRule.getRuleName(),
                    oldValue,
                    updatedRule.toString()
            );
            
            return ResponseEntity.ok(updatedRule);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update rule: " + e.getMessage()));
        }
    }
    
    /**
     * Toggle rule status (activate/deactivate)
     */
    @PatchMapping("/{ruleId}/toggle")
    public ResponseEntity<?> toggleRuleStatus(@PathVariable Long ruleId, Principal principal) {
        try {
            FraudRuleDefinition rule = ruleRepository.findById(ruleId)
                    .orElseThrow(() -> new RuntimeException("Rule not found with id: " + ruleId));
            
            boolean oldStatus = rule.getIsActive();
            rule.setIsActive(!oldStatus);
            rule.setUpdatedBy(principal.getName());
            
            FraudRuleDefinition updatedRule = ruleRepository.save(rule);
            
            // Log activity
            activityLogService.logActivity(
                    principal.getName(),
                    "ADMIN",
                    "UPDATE",
                    "FRAUD_RULE",
                    updatedRule.getRuleId(),
                    (updatedRule.getIsActive() ? "Activated" : "Deactivated") + " fraud rule: " + updatedRule.getRuleName()
            );
            
            return ResponseEntity.ok(updatedRule);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to toggle rule status: " + e.getMessage()));
        }
    }
    
    /**
     * Delete fraud rule
     */
    @DeleteMapping("/{ruleId}")
    public ResponseEntity<?> deleteRule(@PathVariable Long ruleId, Principal principal) {
        try {
            FraudRuleDefinition rule = ruleRepository.findById(ruleId)
                    .orElseThrow(() -> new RuntimeException("Rule not found with id: " + ruleId));
            
            String ruleName = rule.getRuleName();
            ruleRepository.delete(rule);
            
            // Log activity
            activityLogService.logActivity(
                    principal.getName(),
                    "ADMIN",
                    "DELETE",
                    "FRAUD_RULE",
                    ruleId,
                    "Deleted fraud rule: " + ruleName
            );
            
            return ResponseEntity.ok(Map.of("message", "Rule deleted successfully", "ruleName", ruleName));
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete rule: " + e.getMessage()));
        }
    }
    
    /**
     * Get rule statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getRuleStatistics() {
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
        
        return ResponseEntity.ok(stats);
    }
    
    // DTOs
    @Data
    public static class CreateRuleRequest {
        private String ruleCode;
        private String ruleName;
        private String ruleDescription;
        private String ruleCategory;
        private String severity;
        private Integer fraudPoints;
        private Boolean isActive;
        private String ruleType;
        private Integer executionOrder;
    }
    
    @Data
    public static class UpdateRuleRequest {
        private String ruleName;
        private String ruleDescription;
        private String ruleCategory;
        private String severity;
        private Integer fraudPoints;
        private Boolean isActive;
        private String ruleType;
        private Integer executionOrder;
    }
}
