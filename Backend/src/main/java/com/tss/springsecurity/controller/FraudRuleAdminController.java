//package com.tss.springsecurity.controller;
//
//import com.tss.springsecurity.entity.*;
//import com.tss.springsecurity.repository.FraudRuleDefinitionRepository;
//import com.tss.springsecurity.service.FraudRuleService;
//import lombok.Data;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//
//import java.security.Principal;
//import java.util.List;
//import java.util.Map;
//
///**
// * Admin controller for managing fraud detection rules
// * Only accessible by ADMIN role
// */
//@RestController
//@RequestMapping("/api/admin/fraud-rules")
//@PreAuthorize("hasRole('ADMIN')")
//@CrossOrigin(origins = "*")
//public class FraudRuleAdminController {
//    
//    private final FraudRuleService fraudRuleService;
//    private final FraudRuleDefinitionRepository ruleRepository;
//    
//    public FraudRuleAdminController(FraudRuleService fraudRuleService,
//                                    FraudRuleDefinitionRepository ruleRepository) {
//        this.fraudRuleService = fraudRuleService;
//        this.ruleRepository = ruleRepository;
//    }
//    
//    /**
//     * Get all fraud rules
//     */
//    @GetMapping
//    public ResponseEntity<List<FraudRuleDefinition>> getAllRules() {
//        return ResponseEntity.ok(ruleRepository.findAll());
//    }
//    
//    /**
//     * Get all active rules
//     */
//    @GetMapping("/active")
//    public ResponseEntity<List<FraudRuleDefinition>> getActiveRules() {
//        return ResponseEntity.ok(fraudRuleService.getAllActiveRules());
//    }
//    
//    /**
//     * Get rules by category
//     */
//    @GetMapping("/category/{category}")
//    public ResponseEntity<List<FraudRuleDefinition>> getRulesByCategory(@PathVariable String category) {
//        return ResponseEntity.ok(fraudRuleService.getActiveRulesByCategory(category));
//    }
//    
//    /**
//     * Get rule by ID
//     */
//    @GetMapping("/{ruleId}")
//    public ResponseEntity<FraudRuleDefinition> getRuleById(@PathVariable Long ruleId) {
//        return ruleRepository.findById(ruleId)
//                .map(ResponseEntity::ok)
//                .orElse(ResponseEntity.notFound().build());
//    }
//    
//    /**
//     * Get rule by code
//     */
//    @GetMapping("/code/{ruleCode}")
//    public ResponseEntity<FraudRuleDefinition> getRuleByCode(@PathVariable String ruleCode) {
//        return ResponseEntity.ok(fraudRuleService.getRuleByCode(ruleCode));
//    }
//    
//    /**
//     * Update fraud points for a rule
//     */
//    @PutMapping("/{ruleId}/fraud-points")
//    public ResponseEntity<FraudRuleDefinition> updateFraudPoints(
//            @PathVariable Long ruleId,
//            @RequestBody UpdateFraudPointsRequest request,
//            Principal principal) {
//        
//        FraudRuleDefinition updated = fraudRuleService.updateFraudPoints(
//                ruleId,
//                request.getNewPoints(),
//                principal.getName(),
//                request.getReason()
//        );
//        return ResponseEntity.ok(updated);
//    }
//    
//    /**
//     * Update rule parameter
//     */
//    @PutMapping("/{ruleId}/parameters/{paramName}")
//    public ResponseEntity<FraudRuleParameter> updateParameter(
//            @PathVariable Long ruleId,
//            @PathVariable String paramName,
//            @RequestBody UpdateParameterRequest request,
//            Principal principal) {
//        
//        FraudRuleParameter updated = fraudRuleService.updateParameter(
//                ruleId,
//                paramName,
//                request.getNewValue(),
//                principal.getName(),
//                request.getReason()
//        );
//        return ResponseEntity.ok(updated);
//    }
//    
//    /**
//     * Activate a rule
//     */
//    @PatchMapping("/{ruleId}/activate")
//    public ResponseEntity<FraudRuleDefinition> activateRule(
//            @PathVariable Long ruleId,
//            @RequestBody ToggleRuleRequest request,
//            Principal principal) {
//        
//        FraudRuleDefinition updated = fraudRuleService.toggleRuleStatus(
//                ruleId,
//                true,
//                principal.getName(),
//                request.getReason()
//        );
//        return ResponseEntity.ok(updated);
//    }
//    
//    /**
//     * Deactivate a rule
//     */
//    @PatchMapping("/{ruleId}/deactivate")
//    public ResponseEntity<FraudRuleDefinition> deactivateRule(
//            @PathVariable Long ruleId,
//            @RequestBody ToggleRuleRequest request,
//            Principal principal) {
//        
//        FraudRuleDefinition updated = fraudRuleService.toggleRuleStatus(
//                ruleId,
//                false,
//                principal.getName(),
//                request.getReason()
//        );
//        return ResponseEntity.ok(updated);
//    }
//    
//    /**
//     * Get audit history for a rule
//     */
//    @GetMapping("/{ruleId}/audit")
//    public ResponseEntity<List<FraudRuleAudit>> getRuleAuditHistory(@PathVariable Long ruleId) {
//        return ResponseEntity.ok(fraudRuleService.getRuleAuditHistory(ruleId));
//    }
//    
//    /**
//     * Get execution logs for an applicant
//     */
//    @GetMapping("/executions/applicant/{applicantId}")
//    public ResponseEntity<List<FraudRuleExecutionLog>> getApplicantExecutionLogs(@PathVariable Long applicantId) {
//        return ResponseEntity.ok(fraudRuleService.getApplicantExecutionLogs(applicantId));
//    }
//    
//    /**
//     * Get triggered rules for an applicant
//     */
//    @GetMapping("/executions/applicant/{applicantId}/triggered")
//    public ResponseEntity<List<FraudRuleExecutionLog>> getTriggeredRules(@PathVariable Long applicantId) {
//        return ResponseEntity.ok(fraudRuleService.getTriggeredRulesForApplicant(applicantId));
//    }
//    
//    /**
//     * Get rule statistics
//     */
//    @GetMapping("/statistics")
//    public ResponseEntity<Map<String, Object>> getRuleStatistics() {
//        long totalRules = ruleRepository.count();
//        long activeRules = ruleRepository.findByIsActiveTrueOrderByExecutionOrderAsc().size();
//        long identityRules = ruleRepository.countByRuleCategoryAndIsActiveTrue("IDENTITY");
//        long financialRules = ruleRepository.countByRuleCategoryAndIsActiveTrue("FINANCIAL");
//        long employmentRules = ruleRepository.countByRuleCategoryAndIsActiveTrue("EMPLOYMENT");
//        long crossVerificationRules = ruleRepository.countByRuleCategoryAndIsActiveTrue("CROSS_VERIFICATION");
//        
//        return ResponseEntity.ok(Map.of(
//                "totalRules", totalRules,
//                "activeRules", activeRules,
//                "inactiveRules", totalRules - activeRules,
//                "identityRules", identityRules,
//                "financialRules", financialRules,
//                "employmentRules", employmentRules,
//                "crossVerificationRules", crossVerificationRules
//        ));
//    }
//    
//    /**
//     * Clear cache
//     */
//    @PostMapping("/cache/clear")
//    public ResponseEntity<String> clearCache() {
//        fraudRuleService.clearCache();
//        return ResponseEntity.ok("Cache cleared successfully");
//    }
//    
//    // Request DTOs
//    @Data
//    public static class UpdateFraudPointsRequest {
//        private Integer newPoints;
//        private String reason;
//    }
//    
//    @Data
//    public static class UpdateParameterRequest {
//        private String newValue;
//        private String reason;
//    }
//    
//    @Data
//    public static class ToggleRuleRequest {
//        private String reason;
//    }
//}
