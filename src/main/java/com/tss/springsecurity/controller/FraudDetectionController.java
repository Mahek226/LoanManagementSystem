package com.tss.springsecurity.controller;

import com.tss.springsecurity.dto.FraudFlagResponse;
import com.tss.springsecurity.entity.FraudFlag;
import com.tss.springsecurity.fraud.FraudDetectionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fraud-detection")
public class FraudDetectionController {
    
    private final FraudDetectionService fraudDetectionService;
    
    public FraudDetectionController(FraudDetectionService fraudDetectionService) {
        this.fraudDetectionService = fraudDetectionService;
    }
    
    /**
     * Run complete fraud detection (Identity + Financial + Employment + Cross-Verification) for an applicant
     */
    @GetMapping("/check/{applicantId}")
    public ResponseEntity<Map<String, Object>> checkFraud(@PathVariable Long applicantId) {
        try {
            FraudDetectionResult result = fraudDetectionService.runFraudDetection(applicantId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("applicantId", result.getApplicantId());
            response.put("applicantName", result.getApplicantName());
            response.put("totalFraudScore", result.getTotalFraudScore());
            response.put("riskLevel", result.getRiskLevel());
            response.put("isFraudulent", result.isFraudulent());
            response.put("recommendation", result.getRecommendation());
            response.put("triggeredRulesCount", result.getTriggeredRules().size());
            response.put("triggeredRules", result.getTriggeredRules());
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            errorResponse.put("details", e.getCause() != null ? e.getCause().getMessage() : "No additional details");
            
            // Log the full stack trace
            e.printStackTrace();
            
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Run only identity fraud detection
     */
    @PostMapping("/check-identity/{applicantId}")
    public ResponseEntity<Map<String, Object>> checkIdentityFraud(@PathVariable Long applicantId) {
        try {
            FraudDetectionResult result = fraudDetectionService.runIdentityFraudDetection(applicantId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("category", "IDENTITY");
            response.put("applicantId", result.getApplicantId());
            response.put("applicantName", result.getApplicantName());
            response.put("totalFraudScore", result.getTotalFraudScore());
            response.put("riskLevel", result.getRiskLevel());
            response.put("isFraudulent", result.isFraudulent());
            response.put("recommendation", result.getRecommendation());
            response.put("triggeredRulesCount", result.getTriggeredRules().size());
            response.put("triggeredRules", result.getTriggeredRules());
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * Run only financial fraud detection
     */
    @PostMapping("/check-financial/{applicantId}")
    public ResponseEntity<Map<String, Object>> checkFinancialFraud(@PathVariable Long applicantId) {
        try {
            FraudDetectionResult result = fraudDetectionService.runFinancialFraudDetection(applicantId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("category", "FINANCIAL");
            response.put("applicantId", result.getApplicantId());
            response.put("applicantName", result.getApplicantName());
            response.put("totalFraudScore", result.getTotalFraudScore());
            response.put("riskLevel", result.getRiskLevel());
            response.put("isFraudulent", result.isFraudulent());
            response.put("recommendation", result.getRecommendation());
            response.put("triggeredRulesCount", result.getTriggeredRules().size());
            response.put("triggeredRules", result.getTriggeredRules());
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * Run only employment fraud detection
     */
    @PostMapping("/check-employment/{applicantId}")
    public ResponseEntity<Map<String, Object>> checkEmploymentFraud(@PathVariable Long applicantId) {
        try {
            FraudDetectionResult result = fraudDetectionService.runEmploymentFraudDetection(applicantId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("category", "EMPLOYMENT");
            response.put("applicantId", result.getApplicantId());
            response.put("applicantName", result.getApplicantName());
            response.put("totalFraudScore", result.getTotalFraudScore());
            response.put("riskLevel", result.getRiskLevel());
            response.put("isFraudulent", result.isFraudulent());
            response.put("recommendation", result.getRecommendation());
            response.put("triggeredRulesCount", result.getTriggeredRules().size());
            response.put("triggeredRules", result.getTriggeredRules());
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * Run only cross-verification fraud detection
     */
    @PostMapping("/check-cross-verification/{applicantId}")
    public ResponseEntity<Map<String, Object>> checkCrossVerificationFraud(@PathVariable Long applicantId) {
        try {
            FraudDetectionResult result = fraudDetectionService.runCrossVerificationFraudDetection(applicantId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("category", "CROSS_VERIFICATION");
            response.put("applicantId", result.getApplicantId());
            response.put("applicantName", result.getApplicantName());
            response.put("totalFraudScore", result.getTotalFraudScore());
            response.put("riskLevel", result.getRiskLevel());
            response.put("isFraudulent", result.isFraudulent());
            response.put("recommendation", result.getRecommendation());
            response.put("triggeredRulesCount", result.getTriggeredRules().size());
            response.put("triggeredRules", result.getTriggeredRules());
            
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * Get fraud flags for an applicant
     */
    @GetMapping("/flags/applicant/{applicantId}")
    public ResponseEntity<List<FraudFlagResponse>> getApplicantFraudFlags(@PathVariable Long applicantId) {
        List<FraudFlag> flags = fraudDetectionService.getFraudFlags(applicantId);
        List<FraudFlagResponse> response = flags.stream()
                .map(this::mapToFraudFlagResponse)
                .toList();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * Get fraud flags for a loan
     */
    @GetMapping("/flags/loan/{loanId}")
    public ResponseEntity<List<FraudFlagResponse>> getLoanFraudFlags(@PathVariable Long loanId) {
        List<FraudFlag> flags = fraudDetectionService.getLoanFraudFlags(loanId);
        List<FraudFlagResponse> response = flags.stream()
                .map(this::mapToFraudFlagResponse)
                .toList();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * Get all high severity fraud flags
     */
    @GetMapping("/flags/high-severity")
    public ResponseEntity<Map<String, Object>> getHighSeverityFlags() {
        List<FraudFlag> flags = fraudDetectionService.getHighSeverityFlags();
        
        Map<String, Object> response = new HashMap<>();
        response.put("count", flags.size());
        response.put("flags", flags);
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * Get all critical fraud flags
     */
    @GetMapping("/flags/critical")
    public ResponseEntity<Map<String, Object>> getCriticalFlags() {
        List<FraudFlag> flags = fraudDetectionService.getCriticalFlags();
        
        Map<String, Object> response = new HashMap<>();
        response.put("count", flags.size());
        response.put("flags", flags);
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
    /**
     * Map FraudFlag entity to FraudFlagResponse DTO
     */
    private FraudFlagResponse mapToFraudFlagResponse(FraudFlag flag) {
        return FraudFlagResponse.builder()
                .id(flag.getId())
                .ruleName(flag.getRuleName())
                .severity(flag.getSeverity())
                .flagNotes(flag.getFlagNotes())
                .createdAt(flag.getCreatedAt())
                .applicantId(flag.getApplicant() != null ? flag.getApplicant().getApplicantId() : null)
                .loanId(flag.getLoan() != null ? flag.getLoan().getLoanId() : null)
                .build();
    }
}
