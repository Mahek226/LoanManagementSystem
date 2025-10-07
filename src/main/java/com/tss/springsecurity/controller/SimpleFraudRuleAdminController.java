//package com.tss.springsecurity.controller;
//
//import com.tss.springsecurity.entity.FraudRuleDefinitionSimple;
//import com.tss.springsecurity.repository.FraudRuleDefinitionSimpleRepository;
//import com.tss.springsecurity.service.SimpleFraudRuleService;
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
// * SIMPLIFIED: Admin controller for managing fraud rules (single table)
// */
//@RestController
//@RequestMapping("/api/admin/fraud-rules-simple")
//@PreAuthorize("hasRole('ADMIN')")
//@CrossOrigin(origins = "*")
//public class SimpleFraudRuleAdminController {
//    
//    private final SimpleFraudRuleService fraudRuleService;
//    private final FraudRuleDefinitionSimpleRepository ruleRepository;
//    
//    public SimpleFraudRuleAdminController(SimpleFraudRuleService fraudRuleService,
//                                          FraudRuleDefinitionSimpleRepository ruleRepository) {
//        this.fraudRuleService = fraudRuleService;
//        this.ruleRepository = ruleRepository;
//    }
//    
//    /**
//     * Get all rules
//     */
//    @GetMapping
//    public ResponseEntity<List<FraudRuleDefinitionSimple>> getAllRules() {
//        return ResponseEntity.ok(ruleRepository.findAll());
//    }
//    
//    /**
//     * Get all active rules
//     */
//    @GetMapping("/active")
//    public ResponseEntity<List<FraudRuleDefinitionSimple>> getActiveRules() {
//        return ResponseEntity.ok(fraudRuleService.getAllActiveRules());
//    }
//    
//    /**
//     * Get rules by category
//     */
//    @GetMapping("/category/{category}")
//    public ResponseEntity<List<FraudRuleDefinitionSimple>> getRulesByCategory(@PathVariable String category) {
//        return ResponseEntity.ok(fraudRuleService.getActiveRulesByCategory(category));
//    }
//    
//    /**
//     * Get rule by ID
//     */
//    @GetMapping("/{ruleId}")
//    public ResponseEntity<FraudRuleDefinitionSimple> getRuleById(@PathVariable Long ruleId) {
//        return ruleRepository.findById(ruleId)
//                .map(ResponseEntity::ok)
//                .orElse(ResponseEntity.notFound().build());
//    }
//    
//    /**
//     * Update fraud points
//     */
//    @PutMapping("/{ruleId}/fraud-points")
//    public ResponseEntity<FraudRuleDefinitionSimple> updateFraudPoints(
//            @PathVariable Long ruleId,
//            @RequestBody UpdateFraudPointsRequest request,
//            Principal principal) {
//        
//        FraudRuleDefinitionSimple updated = fraudRuleService.updateFraudPoints(
//                ruleId,
//                request.getNewPoints(),
//                principal.getName()
//        );
//        return ResponseEntity.ok(updated);
//    }
//    
//    /**
//     * Update parameter (e.g., MIN_AGE, MAX_DTI_RATIO)
//     */
//    @PutMapping("/{ruleId}/parameters/{paramName}")
//    public ResponseEntity<FraudRuleDefinitionSimple> updateParameter(
//            @PathVariable Long ruleId,
//            @PathVariable String paramName,
//            @RequestBody UpdateParameterRequest request,
//            Principal principal) {
//        
//        FraudRuleDefinitionSimple updated = fraudRuleService.updateParameter(
//                ruleId,
//                paramName,
//                request.getNewValue(),
//                principal.getName()
//        );
//        return ResponseEntity.ok(updated);
//    }
//    
//    /**
//     * Update severity
//     */
//    @PutMapping("/{ruleId}/severity")
//    public ResponseEntity<FraudRuleDefinitionSimple> updateSeverity(
//            @PathVariable Long ruleId,
//            @RequestBody UpdateSeverityRequest request,
//            Principal principal) {
//        
//        FraudRuleDefinitionSimple updated = fraudRuleService.updateSeverity(
//                ruleId,
//                request.getNewSeverity(),
//                principal.getName()
//        );
//        return ResponseEntity.ok(updated);
//    }
//    
//    /**
//     * Activate a rule
//     */
//    @PatchMapping("/{ruleId}/activate")
//    public ResponseEntity<FraudRuleDefinitionSimple> activateRule(
//            @PathVariable Long ruleId,
//            Principal principal) {
//        
//        FraudRuleDefinitionSimple updated = fraudRuleService.toggleRuleStatus(
//                ruleId,
//                true,
//                principal.getName()
//        );
//        return ResponseEntity.ok(updated);
//    }
//    
//    /**
//     * Deactivate a rule
//     */
//    @PatchMapping("/{ruleId}/deactivate")
//    public ResponseEntity<FraudRuleDefinitionSimple> deactivateRule(
//            @PathVariable Long ruleId,
//            Principal principal) {
//        
//        FraudRuleDefinitionSimple updated = fraudRuleService.toggleRuleStatus(
//                ruleId,
//                false,
//                principal.getName()
//        );
//        return ResponseEntity.ok(updated);
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
//    }
//    
//    @Data
//    public static class UpdateParameterRequest {
//        private Object newValue;
//    }
//    
//    @Data
//    public static class UpdateSeverityRequest {
//        private String newSeverity;
//    }
//}
