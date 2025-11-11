package com.tss.springsecurity.externalfraud.controller;

import com.tss.springsecurity.externalfraud.service.CombinedFraudScreeningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/combined-fraud")
@Slf4j
public class CombinedFraudScreeningController {
    
    @Autowired
    private CombinedFraudScreeningService combinedFraudScreeningService;
    
    /**
     * Perform combined fraud screening (Internal + External)
     */
    @PostMapping("/screen/{applicantId}")
    public ResponseEntity<Map<String, Object>> performCombinedScreening(@PathVariable Long applicantId) {
        try {
            log.info("Combined fraud screening request received for applicant ID: {}", applicantId);
            
            CombinedFraudScreeningService.CombinedFraudResult result = 
                    combinedFraudScreeningService.performCombinedScreening(applicantId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("applicantId", applicantId);
            response.put("screeningType", "COMBINED");
            response.put("combinedResult", result);
            response.put("message", "Combined fraud screening completed successfully");
            
            // Add summary for quick reference
            Map<String, Object> summary = new HashMap<>();
            summary.put("finalRiskLevel", result.getFinalRiskLevel());
            summary.put("finalRecommendation", result.getFinalRecommendation());
            summary.put("combinedFraudScore", result.getCombinedFraudScore());
            summary.put("totalFraudFlags", result.getAllFraudFlags().size());
            summary.put("screeningTimeMs", result.getTotalScreeningTimeMs());
            summary.put("hasErrors", result.isHasErrors());
            
            if (result.getInternalResult() != null) {
                summary.put("internalRiskLevel", result.getInternalResult().getRiskLevel());
                summary.put("internalScore", result.getInternalResult().getTotalFraudScore());
            }
            
            if (result.getExternalResult() != null) {
                summary.put("externalRiskLevel", result.getExternalResult().getRiskLevel());
                summary.put("externalScore", result.getExternalResult().getTotalFraudScore());
                summary.put("personFoundInExternalDB", result.getExternalResult().isPersonFound());
            }
            
            response.put("summary", summary);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error during combined fraud screening for applicant ID: {}", applicantId, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("applicantId", applicantId);
            errorResponse.put("screeningType", "COMBINED");
            errorResponse.put("message", "Combined fraud screening failed: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get combined fraud screening summary only
     */
    @GetMapping("/summary/{applicantId}")
    public ResponseEntity<Map<String, Object>> getCombinedScreeningSummary(@PathVariable Long applicantId) {
        try {
            CombinedFraudScreeningService.CombinedFraudResult result = 
                    combinedFraudScreeningService.performCombinedScreening(applicantId);
            
            Map<String, Object> summary = new HashMap<>();
            summary.put("applicantId", applicantId);
            summary.put("finalRiskLevel", result.getFinalRiskLevel());
            summary.put("finalRecommendation", result.getFinalRecommendation());
            summary.put("combinedFraudScore", result.getCombinedFraudScore());
            summary.put("totalFraudFlags", result.getAllFraudFlags().size());
            summary.put("screeningTimestamp", result.getScreeningTimestamp());
            summary.put("hasErrors", result.isHasErrors());
            
            // Internal screening summary
            if (result.getInternalResult() != null) {
                Map<String, Object> internalSummary = new HashMap<>();
                internalSummary.put("riskLevel", result.getInternalResult().getRiskLevel());
                internalSummary.put("fraudScore", result.getInternalResult().getTotalFraudScore());
                internalSummary.put("triggeredRulesCount", result.getInternalResult().getTriggeredRules().size());
                internalSummary.put("recommendation", result.getInternalResult().getRecommendation());
                summary.put("internal", internalSummary);
            }
            
            // External screening summary
            if (result.getExternalResult() != null) {
                Map<String, Object> externalSummary = new HashMap<>();
                externalSummary.put("personFound", result.getExternalResult().isPersonFound());
                externalSummary.put("riskLevel", result.getExternalResult().getRiskLevel());
                externalSummary.put("fraudScore", result.getExternalResult().getTotalFraudScore());
                externalSummary.put("fraudFlagsCount", result.getExternalResult().getFraudFlags().size());
                externalSummary.put("hasCriminalRecord", result.getExternalResult().isHasCriminalRecord());
                externalSummary.put("hasLoanHistory", result.getExternalResult().isHasLoanHistory());
                externalSummary.put("defaultedLoans", result.getExternalResult().getDefaultedLoans());
                externalSummary.put("recommendation", result.getExternalResult().getRecommendation());
                summary.put("external", externalSummary);
            }
            
            // Key insights
            summary.put("insights", result.getInsights());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("summary", summary);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting combined fraud screening summary for applicant ID: {}", applicantId, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("applicantId", applicantId);
            errorResponse.put("message", "Failed to get combined screening summary: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get fraud flags breakdown by source
     */
    @GetMapping("/flags/{applicantId}")
    public ResponseEntity<Map<String, Object>> getFraudFlagsBreakdown(@PathVariable Long applicantId) {
        try {
            CombinedFraudScreeningService.CombinedFraudResult result = 
                    combinedFraudScreeningService.performCombinedScreening(applicantId);
            
            Map<String, Object> breakdown = new HashMap<>();
            breakdown.put("applicantId", applicantId);
            breakdown.put("totalFlags", result.getAllFraudFlags().size());
            
            // Separate flags by source
            Map<String, Object> internalFlags = new HashMap<>();
            Map<String, Object> externalFlags = new HashMap<>();
            
            int internalCount = 0;
            int externalCount = 0;
            int internalPoints = 0;
            int externalPoints = 0;
            
            for (CombinedFraudScreeningService.CombinedFraudFlag flag : result.getAllFraudFlags()) {
                if ("INTERNAL".equals(flag.getSource())) {
                    internalCount++;
                    internalPoints += flag.getPoints();
                } else if ("EXTERNAL".equals(flag.getSource())) {
                    externalCount++;
                    externalPoints += flag.getPoints();
                }
            }
            
            internalFlags.put("count", internalCount);
            internalFlags.put("totalPoints", internalPoints);
            internalFlags.put("flags", result.getAllFraudFlags().stream()
                    .filter(flag -> "INTERNAL".equals(flag.getSource()))
                    .toList());
            
            externalFlags.put("count", externalCount);
            externalFlags.put("totalPoints", externalPoints);
            externalFlags.put("flags", result.getAllFraudFlags().stream()
                    .filter(flag -> "EXTERNAL".equals(flag.getSource()))
                    .toList());
            
            breakdown.put("internal", internalFlags);
            breakdown.put("external", externalFlags);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("breakdown", breakdown);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting fraud flags breakdown for applicant ID: {}", applicantId, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("applicantId", applicantId);
            errorResponse.put("message", "Failed to get fraud flags breakdown: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
