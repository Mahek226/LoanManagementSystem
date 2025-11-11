package com.tss.springsecurity.externalfraud.controller;

import com.tss.springsecurity.externalfraud.model.ExternalFraudCheckResult;
import com.tss.springsecurity.externalfraud.service.ExternalFraudScreeningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/external-fraud")
@Slf4j
public class ExternalFraudScreeningController {
    
    @Autowired
    private ExternalFraudScreeningService externalFraudScreeningService;
    
    /**
     * Screen applicant using external fraud database
     */
    @PostMapping("/screen/{applicantId}")
    public ResponseEntity<Map<String, Object>> screenApplicant(@PathVariable Long applicantId) {
        try {
            log.info("External fraud screening request received for applicant ID: {}", applicantId);
            
            ExternalFraudCheckResult result = externalFraudScreeningService.screenApplicant(applicantId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("applicantId", applicantId);
            response.put("externalScreeningResult", result);
            response.put("message", "External fraud screening completed successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error during external fraud screening for applicant ID: {}", applicantId, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("applicantId", applicantId);
            errorResponse.put("message", "External fraud screening failed: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Screen applicant with deep screening option
     */
    @PostMapping("/screen/{applicantId}/deep")
    public ResponseEntity<Map<String, Object>> screenApplicantDeep(@PathVariable Long applicantId) {
        try {
            log.info("Deep external fraud screening request received for applicant ID: {}", applicantId);
            
            ExternalFraudCheckResult result = externalFraudScreeningService.screenApplicant(applicantId, true);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("applicantId", applicantId);
            response.put("screeningType", "DEEP");
            response.put("externalScreeningResult", result);
            response.put("message", "Deep external fraud screening completed successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error during deep external fraud screening for applicant ID: {}", applicantId, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("applicantId", applicantId);
            errorResponse.put("screeningType", "DEEP");
            errorResponse.put("message", "Deep external fraud screening failed: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Screen by specific identifiers
     */
    @PostMapping("/screen/identifiers")
    public ResponseEntity<Map<String, Object>> screenByIdentifiers(
            @RequestParam(required = false) String panNumber,
            @RequestParam(required = false) String aadhaarNumber,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String email) {
        
        try {
            log.info("External fraud screening by identifiers - PAN: {}, Aadhaar: {}", 
                    maskIdentifier(panNumber), maskIdentifier(aadhaarNumber));
            
            if (panNumber == null && aadhaarNumber == null && phoneNumber == null && email == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "At least one identifier (PAN, Aadhaar, Phone, Email) is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            ExternalFraudCheckResult result = externalFraudScreeningService.screenByIdentifiers(
                    panNumber, aadhaarNumber, phoneNumber, email);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("screeningType", "IDENTIFIER_BASED");
            response.put("externalScreeningResult", result);
            response.put("message", "External fraud screening by identifiers completed successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error during external fraud screening by identifiers", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("screeningType", "IDENTIFIER_BASED");
            errorResponse.put("message", "External fraud screening by identifiers failed: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Get external fraud screening summary for applicant
     */
    @GetMapping("/summary/{applicantId}")
    public ResponseEntity<Map<String, Object>> getScreeningSummary(@PathVariable Long applicantId) {
        try {
            ExternalFraudCheckResult result = externalFraudScreeningService.screenApplicant(applicantId);
            
            Map<String, Object> summary = new HashMap<>();
            summary.put("applicantId", applicantId);
            summary.put("personFoundInExternalDB", result.isPersonFound());
            summary.put("riskLevel", result.getRiskLevel());
            summary.put("recommendation", result.getRecommendation());
            summary.put("totalFraudScore", result.getTotalFraudScore());
            summary.put("fraudFlagsCount", result.getFraudFlags().size());
            summary.put("hasCriminalRecord", result.isHasCriminalRecord());
            summary.put("hasLoanHistory", result.isHasLoanHistory());
            summary.put("screeningTimestamp", result.getScreeningTimestamp());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("summary", summary);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting external fraud screening summary for applicant ID: {}", applicantId, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("applicantId", applicantId);
            errorResponse.put("message", "Failed to get screening summary: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.length() < 4) {
            return "****";
        }
        return "****" + identifier.substring(identifier.length() - 4);
    }
}
