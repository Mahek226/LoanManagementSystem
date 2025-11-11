package com.tss.springsecurity.controller;

import com.tss.springsecurity.entity.FraudRuleDefinition;
import com.tss.springsecurity.service.FraudRuleManagementService;
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
    
    private final FraudRuleManagementService fraudRuleManagementService;
    private final ActivityLogService activityLogService;
    
    /**
     * Get all fraud rules
     */
    @GetMapping
    public ResponseEntity<List<FraudRuleDefinition>> getAllRules() {
        List<FraudRuleDefinition> rules = fraudRuleManagementService.getAllRules();
        return ResponseEntity.ok(rules);
    }
    
    /**
     * Get all active rules
     */
    @GetMapping("/active")
    public ResponseEntity<List<FraudRuleDefinition>> getActiveRules() {
        List<FraudRuleDefinition> rules = fraudRuleManagementService.getActiveRules();
        return ResponseEntity.ok(rules);
    }
    
    /**
     * Get rules by category
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<FraudRuleDefinition>> getRulesByCategory(@PathVariable String category) {
        List<FraudRuleDefinition> rules = fraudRuleManagementService.getRulesByCategory(category);
        return ResponseEntity.ok(rules);
    }
    
    /**
     * Get rule by ID
     */
    @GetMapping("/{ruleId}")
    public ResponseEntity<?> getRuleById(@PathVariable Long ruleId) {
        try {
            FraudRuleDefinition rule = fraudRuleManagementService.getRuleById(ruleId);
            return ResponseEntity.ok(rule);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get rule by code
     */
    @GetMapping("/code/{ruleCode}")
    public ResponseEntity<?> getRuleByCode(@PathVariable String ruleCode) {
        try {
            FraudRuleDefinition rule = fraudRuleManagementService.getRuleByCode(ruleCode);
            return ResponseEntity.ok(rule);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Create new fraud rule
     */
    @PostMapping
    public ResponseEntity<?> createRule(@Valid @RequestBody CreateRuleRequest request, Principal principal) {
        try {
            FraudRuleDefinition savedRule = fraudRuleManagementService.createRule(request, principal.getName());
            
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
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
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
            FraudRuleDefinition updatedRule = fraudRuleManagementService.updateRule(ruleId, request, principal.getName());
            
            // Log activity
            activityLogService.logActivity(
                    principal.getName(),
                    "ADMIN",
                    "UPDATE",
                    "FRAUD_RULE",
                    updatedRule.getRuleId(),
                    "Updated fraud rule: " + updatedRule.getRuleName()
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
            FraudRuleDefinition updatedRule = fraudRuleManagementService.toggleRuleStatus(ruleId, principal.getName());
            
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
            String ruleName = fraudRuleManagementService.deleteRule(ruleId);
            
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
        Map<String, Object> stats = fraudRuleManagementService.getRuleStatistics();
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
