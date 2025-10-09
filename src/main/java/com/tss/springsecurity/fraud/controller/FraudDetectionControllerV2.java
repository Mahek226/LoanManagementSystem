package com.tss.springsecurity.fraud.controller;

import com.tss.springsecurity.fraud.FraudDetectionResult;
import com.tss.springsecurity.fraud.model.FraudDetectionRequest;
import com.tss.springsecurity.fraud.service.FraudValidationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Optimized MVC Controller for fraud detection
 * Handles HTTP requests and delegates to service layer
 */
@RestController("fraudDetectionControllerV2")
@RequestMapping("/api/v2/fraud-detection")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class FraudDetectionControllerV2 {
    
    private final FraudValidationService fraudValidationService;
    
    public FraudDetectionControllerV2(FraudValidationService fraudValidationService) {
        this.fraudValidationService = fraudValidationService;
    }
    
    /**
     * Run complete fraud detection for all categories
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateFraud(@RequestBody FraudDetectionRequest request) {
        try {
            FraudDetectionResult result = fraudValidationService.validateAll(request.getApplicantId());
            
            Map<String, Object> response = createSuccessResponse(result);
            response.put("requestDetails", request);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return createErrorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Run fraud detection for specific category
     */
    @PostMapping("/validate/{category}")
    public ResponseEntity<Map<String, Object>> validateFraudByCategory(
            @PathVariable String category,
            @RequestBody FraudDetectionRequest request) {
        
        try {
            FraudDetectionResult result = fraudValidationService.validateCategory(category, request.getApplicantId());
            
            Map<String, Object> response = createSuccessResponse(result);
            response.put("category", category);
            response.put("requestDetails", request);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * Quick fraud check (GET endpoint for backward compatibility)
     */
    @GetMapping("/check/{applicantId}")
    public ResponseEntity<Map<String, Object>> quickFraudCheck(@PathVariable Long applicantId) {
        try {
            FraudDetectionResult result = fraudValidationService.validateAll(applicantId);
            return ResponseEntity.ok(createSuccessResponse(result));
            
        } catch (Exception e) {
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * Create standardized success response
     */
    private Map<String, Object> createSuccessResponse(FraudDetectionResult result) {
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
        response.put("fraudScoreBreakdown", result.getFraudScoreBreakdown());
        response.put("scoringExplanation", result.getScoringExplanation());
        return response;
    }
    
    /**
     * Create standardized error response
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", message);
        errorResponse.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(status).body(errorResponse);
    }
}
