package com.tss.springsecurity.externalfraud.controller;

import com.tss.springsecurity.externalfraud.entity.Person;
import com.tss.springsecurity.externalfraud.model.ExternalFraudCheckResult;
import com.tss.springsecurity.externalfraud.repository.PersonRepository;
import com.tss.springsecurity.externalfraud.service.CombinedFraudScreeningService;
import com.tss.springsecurity.externalfraud.service.ExternalFraudScreeningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/external-fraud/test")
@Slf4j
public class ExternalFraudTestController {
    
    @Autowired
    private ExternalFraudScreeningService externalFraudScreeningService;
    
    @Autowired
    private CombinedFraudScreeningService combinedFraudScreeningService;
    
    @Autowired
    private PersonRepository personRepository;
    
    /**
     * Test endpoint to check external database connectivity
     */
    @GetMapping("/connectivity")
    public ResponseEntity<Map<String, Object>> testConnectivity() {
        try {
            long personCount = personRepository.count();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "External database connectivity successful");
            response.put("totalPersonsInDB", personCount);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("External database connectivity test failed", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "External database connectivity failed: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Get all persons in external database for testing
     */
    @GetMapping("/persons")
    public ResponseEntity<Map<String, Object>> getAllPersons() {
        try {
            List<Person> persons = personRepository.findAll();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalPersons", persons.size());
            response.put("persons", persons);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching persons from external database", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error fetching persons: " + e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Test screening for all risk profiles
     */
    @GetMapping("/risk-profiles")
    public ResponseEntity<Map<String, Object>> testAllRiskProfiles() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Risk profile testing completed");
            
            Map<String, Object> profiles = new HashMap<>();
            
            // Test Clean Profile (Amit Patel - PAN: ABCDE1234F)
            try {
                ExternalFraudCheckResult cleanResult = externalFraudScreeningService
                        .screenByIdentifiers("ABCDE1234F", "123456789012", null, null);
                profiles.put("CLEAN_PROFILE", Map.of(
                    "name", "Amit Patel",
                    "pan", "ABCDE1234F",
                    "riskLevel", cleanResult.getRiskLevel(),
                    "fraudScore", cleanResult.getTotalFraudScore(),
                    "recommendation", cleanResult.getRecommendation(),
                    "personFound", cleanResult.isPersonFound()
                ));
            } catch (Exception e) {
                profiles.put("CLEAN_PROFILE", Map.of("error", e.getMessage()));
            }
            
            // Test Medium Risk Profile (Priya Sharma - PAN: FGHIJ5678K)
            try {
                ExternalFraudCheckResult mediumResult = externalFraudScreeningService
                        .screenByIdentifiers("FGHIJ5678K", "234567890123", null, null);
                profiles.put("MEDIUM_RISK_PROFILE", Map.of(
                    "name", "Priya Sharma",
                    "pan", "FGHIJ5678K",
                    "riskLevel", mediumResult.getRiskLevel(),
                    "fraudScore", mediumResult.getTotalFraudScore(),
                    "recommendation", mediumResult.getRecommendation(),
                    "personFound", mediumResult.isPersonFound(),
                    "defaultedLoans", mediumResult.getDefaultedLoans()
                ));
            } catch (Exception e) {
                profiles.put("MEDIUM_RISK_PROFILE", Map.of("error", e.getMessage()));
            }
            
            // Test High Risk Profile (Rajesh Kumar - PAN: KLMNO9012P)
            try {
                ExternalFraudCheckResult highResult = externalFraudScreeningService
                        .screenByIdentifiers("KLMNO9012P", "345678901234", null, null);
                profiles.put("HIGH_RISK_PROFILE", Map.of(
                    "name", "Rajesh Kumar",
                    "pan", "KLMNO9012P",
                    "riskLevel", highResult.getRiskLevel(),
                    "fraudScore", highResult.getTotalFraudScore(),
                    "recommendation", highResult.getRecommendation(),
                    "personFound", highResult.isPersonFound(),
                    "hasCriminalRecord", highResult.isHasCriminalRecord(),
                    "defaultedLoans", highResult.getDefaultedLoans(),
                    "convictedCases", highResult.getConvictedCases()
                ));
            } catch (Exception e) {
                profiles.put("HIGH_RISK_PROFILE", Map.of("error", e.getMessage()));
            }
            
            // Test Over-leveraged Profile (Sunita Singh - PAN: PQRST3456U)
            try {
                ExternalFraudCheckResult overleveragedResult = externalFraudScreeningService
                        .screenByIdentifiers("PQRST3456U", "456789012345", null, null);
                profiles.put("OVERLEVERAGED_PROFILE", Map.of(
                    "name", "Sunita Singh",
                    "pan", "PQRST3456U",
                    "riskLevel", overleveragedResult.getRiskLevel(),
                    "fraudScore", overleveragedResult.getTotalFraudScore(),
                    "recommendation", overleveragedResult.getRecommendation(),
                    "personFound", overleveragedResult.isPersonFound(),
                    "activeLoans", overleveragedResult.getActiveLoans(),
                    "totalOutstanding", overleveragedResult.getTotalOutstandingAmount()
                ));
            } catch (Exception e) {
                profiles.put("OVERLEVERAGED_PROFILE", Map.of("error", e.getMessage()));
            }
            
            response.put("riskProfiles", profiles);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error testing risk profiles", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Risk profile testing failed: " + e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Test fraud rule engine with specific PAN
     */
    @GetMapping("/test-pan/{panNumber}")
    public ResponseEntity<Map<String, Object>> testByPan(@PathVariable String panNumber) {
        try {
            log.info("Testing fraud screening for PAN: {}", maskPan(panNumber));
            
            ExternalFraudCheckResult result = externalFraudScreeningService
                    .screenByIdentifiers(panNumber, null, null, null);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("panNumber", maskPan(panNumber));
            response.put("screeningResult", result);
            
            // Add detailed breakdown
            Map<String, Object> breakdown = new HashMap<>();
            breakdown.put("personFound", result.isPersonFound());
            breakdown.put("riskLevel", result.getRiskLevel());
            breakdown.put("fraudScore", result.getTotalFraudScore());
            breakdown.put("recommendation", result.getRecommendation());
            breakdown.put("fraudFlagsCount", result.getFraudFlags().size());
            breakdown.put("screeningDuration", result.getScreeningDurationMs() + "ms");
            
            if (result.isPersonFound()) {
                breakdown.put("externalPersonId", result.getExternalPersonId());
                breakdown.put("matchedBy", result.getMatchedBy());
                breakdown.put("hasCriminalRecord", result.isHasCriminalRecord());
                breakdown.put("hasLoanHistory", result.isHasLoanHistory());
                breakdown.put("hasBankRecords", result.isHasBankRecords());
                breakdown.put("hasDocumentIssues", result.isHasDocumentIssues());
            }
            
            response.put("breakdown", breakdown);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error testing PAN: {}", maskPan(panNumber), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("panNumber", maskPan(panNumber));
            errorResponse.put("message", "Testing failed: " + e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Performance test - screen multiple identifiers
     */
    @PostMapping("/performance-test")
    public ResponseEntity<Map<String, Object>> performanceTest(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> panNumbers = (List<String>) request.get("panNumbers");
            
            if (panNumbers == null || panNumbers.isEmpty()) {
                panNumbers = List.of("ABCDE1234F", "FGHIJ5678K", "KLMNO9012P", "PQRST3456U", "UVWXY7890Z");
            }
            
            long startTime = System.currentTimeMillis();
            Map<String, Object> results = new HashMap<>();
            
            for (String pan : panNumbers) {
                try {
                    long panStartTime = System.currentTimeMillis();
                    ExternalFraudCheckResult result = externalFraudScreeningService
                            .screenByIdentifiers(pan, null, null, null);
                    long panEndTime = System.currentTimeMillis();
                    
                    results.put(maskPan(pan), Map.of(
                        "riskLevel", result.getRiskLevel(),
                        "fraudScore", result.getTotalFraudScore(),
                        "personFound", result.isPersonFound(),
                        "screeningTimeMs", panEndTime - panStartTime
                    ));
                    
                } catch (Exception e) {
                    results.put(maskPan(pan), Map.of("error", e.getMessage()));
                }
            }
            
            long totalTime = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("totalScreenings", panNumbers.size());
            response.put("totalTimeMs", totalTime);
            response.put("averageTimeMs", totalTime / panNumbers.size());
            response.put("results", results);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Performance test failed", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Performance test failed: " + e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Test combined screening (Internal + External)
     */
    @PostMapping("/combined/{applicantId}")
    public ResponseEntity<Map<String, Object>> testCombinedScreening(@PathVariable Long applicantId) {
        try {
            log.info("Testing combined screening for applicant ID: {}", applicantId);
            
            CombinedFraudScreeningService.CombinedFraudResult result = 
                    combinedFraudScreeningService.performCombinedScreening(applicantId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("applicantId", applicantId);
            response.put("combinedResult", result);
            
            // Add test summary
            Map<String, Object> testSummary = new HashMap<>();
            testSummary.put("finalRiskLevel", result.getFinalRiskLevel());
            testSummary.put("finalRecommendation", result.getFinalRecommendation());
            testSummary.put("combinedScore", result.getCombinedFraudScore());
            testSummary.put("totalFlags", result.getAllFraudFlags().size());
            testSummary.put("screeningTimeMs", result.getTotalScreeningTimeMs());
            testSummary.put("hasErrors", result.isHasErrors());
            
            if (result.getInternalResult() != null) {
                testSummary.put("internalScore", result.getInternalResult().getTotalFraudScore());
                testSummary.put("internalRisk", result.getInternalResult().getRiskLevel());
            }
            
            if (result.getExternalResult() != null) {
                testSummary.put("externalScore", result.getExternalResult().getTotalFraudScore());
                testSummary.put("externalRisk", result.getExternalResult().getRiskLevel());
                testSummary.put("personFoundExternal", result.getExternalResult().isPersonFound());
            }
            
            response.put("testSummary", testSummary);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Combined screening test failed for applicant ID: {}", applicantId, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("applicantId", applicantId);
            errorResponse.put("message", "Combined screening test failed: " + e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Health check for external fraud system
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Test database connectivity
            long personCount = personRepository.count();
            health.put("database", Map.of(
                "status", "UP",
                "personCount", personCount
            ));
            
            // Test fraud engine
            ExternalFraudCheckResult testResult = externalFraudScreeningService
                    .screenByIdentifiers("TEST_PAN_123", null, null, null);
            health.put("fraudEngine", Map.of(
                "status", "UP",
                "testScreeningTime", testResult.getScreeningDurationMs() + "ms"
            ));
            
            health.put("overall", "UP");
            health.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            health.put("overall", "DOWN");
            health.put("error", e.getMessage());
            health.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(503).body(health);
        }
    }
    
    private String maskPan(String pan) {
        if (pan == null || pan.length() < 4) {
            return "****";
        }
        return "****" + pan.substring(pan.length() - 4);
    }
}
