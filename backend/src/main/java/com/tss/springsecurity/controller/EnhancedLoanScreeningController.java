package com.tss.springsecurity.controller;

import com.tss.springsecurity.dto.EnhancedLoanScreeningResponse;
import com.tss.springsecurity.dto.EnhancedLoanScreeningResponse.*;
import com.tss.springsecurity.dto.LoanScreeningRequest;
import com.tss.springsecurity.dto.LoanScreeningResponse;
import com.tss.springsecurity.entity.*;
import com.tss.springsecurity.repository.*;
import com.tss.springsecurity.service.EnhancedLoanScreeningService;
import com.tss.springsecurity.service.EnhancedLoanScreeningService.EnhancedScoringResult;
import com.tss.springsecurity.service.LoanOfficerScreeningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/enhanced-screening")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class EnhancedLoanScreeningController {
    
    private final EnhancedLoanScreeningService enhancedScreeningService;
    private final LoanOfficerScreeningService loanOfficerScreeningService;
    private final OfficerApplicationAssignmentRepository assignmentRepository;
    private final ComplianceOfficerApplicationAssignmentRepository complianceAssignmentRepository;
    private final ApplicantLoanDetailsRepository loanRepository;
    
    @Value("${loan.risk-score.threshold:70}")
    private Integer riskScoreThreshold;
    
    /**
     * Get enhanced loan details with detailed scoring breakdown for screening
     */
    @GetMapping("/loan/{assignmentId}")
    @PreAuthorize("hasRole('LOAN_OFFICER') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<EnhancedLoanScreeningResponse> getEnhancedLoanDetails(@PathVariable Long assignmentId) {
        log.info("Getting enhanced loan details for assignment: {}", assignmentId);
        
        try {
            // Get basic loan screening response first
            LoanScreeningResponse basicResponse = loanOfficerScreeningService.getLoanDetailsForScreening(assignmentId);
            
            // Perform enhanced screening
            EnhancedScoringResult enhancedResult = enhancedScreeningService.performEnhancedScreening(basicResponse.getApplicantId());
            
            // Convert to enhanced response
            EnhancedLoanScreeningResponse response = convertToEnhancedResponse(basicResponse, enhancedResult);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting enhanced loan details for assignment: {}", assignmentId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get all assigned loans for an officer with enhanced scoring
     */
    @GetMapping("/officer/{officerId}/loans")
    @PreAuthorize("hasRole('LOAN_OFFICER') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<List<EnhancedLoanScreeningResponse>> getEnhancedAssignedLoans(@PathVariable Long officerId) {
        log.info("Getting enhanced assigned loans for officer: {}", officerId);
        
        try {
            // Get basic loan screening responses
            List<LoanScreeningResponse> basicResponses = loanOfficerScreeningService.getAssignedLoansForOfficer(officerId);
            
            // Convert to enhanced responses
            List<EnhancedLoanScreeningResponse> enhancedResponses = basicResponses.stream()
                    .map(basicResponse -> {
                        try {
                            EnhancedScoringResult enhancedResult = enhancedScreeningService.performEnhancedScreening(basicResponse.getApplicantId());
                            return convertToEnhancedResponse(basicResponse, enhancedResult);
                        } catch (Exception e) {
                            log.error("Error performing enhanced screening for applicant: {}", basicResponse.getApplicantId(), e);
                            return convertToEnhancedResponseWithError(basicResponse, e.getMessage());
                        }
                    })
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(enhancedResponses);
            
        } catch (Exception e) {
            log.error("Error getting enhanced assigned loans for officer: {}", officerId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Process loan screening with enhanced scoring
     */
    @PostMapping("/officer/{officerId}/process")
    @PreAuthorize("hasRole('LOAN_OFFICER')")
    public ResponseEntity<EnhancedLoanScreeningResponse> processEnhancedLoanScreening(
            @PathVariable Long officerId,
            @RequestBody LoanScreeningRequest request) {
        
        log.info("Processing enhanced loan screening by officer {} for assignment {}", officerId, request.getAssignmentId());
        
        try {
            // First perform enhanced scoring
            OfficerApplicationAssignment assignment = assignmentRepository.findById(request.getAssignmentId())
                    .orElseThrow(() -> new RuntimeException("Assignment not found with ID: " + request.getAssignmentId()));
            
            EnhancedScoringResult enhancedResult = enhancedScreeningService.performEnhancedScreening(assignment.getApplicant().getApplicantId());
            
            // Check if officer can approve/reject based on normalized score
            boolean canApproveReject = enhancedResult.getNormalizedScore() < riskScoreThreshold;
            
            // Override request action if score is too high
            if (!canApproveReject && ("APPROVE".equals(request.getAction()) || "REJECT".equals(request.getAction()))) {
                request.setAction("ESCALATE_TO_COMPLIANCE");
                request.setRemarks("Auto-escalated due to high risk score: " + enhancedResult.getNormalizedScore() + "%");
            }
            
            // Process using existing service
            LoanScreeningResponse basicResponse = loanOfficerScreeningService.processLoanScreening(officerId, request);
            
            // Convert to enhanced response
            EnhancedLoanScreeningResponse response = convertToEnhancedResponse(basicResponse, enhancedResult);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error processing enhanced loan screening for officer {} and assignment {}", officerId, request.getAssignmentId(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Process compliance decision with enhanced scoring
     */
    @PostMapping("/compliance/{complianceOfficerId}/process")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<EnhancedLoanScreeningResponse> processEnhancedComplianceDecision(
            @PathVariable Long complianceOfficerId,
            @RequestBody LoanScreeningRequest request) {
        
        log.info("Processing enhanced compliance decision by officer {} for assignment {}", complianceOfficerId, request.getAssignmentId());
        
        try {
            // Get compliance assignment
            ComplianceOfficerApplicationAssignment assignment = complianceAssignmentRepository.findById(request.getAssignmentId())
                    .orElseThrow(() -> new RuntimeException("Compliance assignment not found with ID: " + request.getAssignmentId()));
            
            // Perform enhanced scoring
            EnhancedScoringResult enhancedResult = enhancedScreeningService.performEnhancedScreening(assignment.getApplicant().getApplicantId());
            
            // Process using existing service
            LoanScreeningResponse basicResponse = loanOfficerScreeningService.processComplianceDecision(complianceOfficerId, request);
            
            // Convert to enhanced response
            EnhancedLoanScreeningResponse response = convertToEnhancedResponse(basicResponse, enhancedResult);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error processing enhanced compliance decision for officer {} and assignment {}", complianceOfficerId, request.getAssignmentId(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get detailed scoring breakdown for a specific applicant
     */
    @GetMapping("/applicant/{applicantId}/scoring-details")
    @PreAuthorize("hasRole('LOAN_OFFICER') or hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<EnhancedScoringResult> getScoringDetails(@PathVariable Long applicantId) {
        log.info("Getting detailed scoring breakdown for applicant: {}", applicantId);
        
        try {
            EnhancedScoringResult result = enhancedScreeningService.performEnhancedScreening(applicantId);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error getting scoring details for applicant: {}", applicantId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    private EnhancedLoanScreeningResponse convertToEnhancedResponse(LoanScreeningResponse basicResponse, EnhancedScoringResult enhancedResult) {
        EnhancedLoanScreeningResponse response = new EnhancedLoanScreeningResponse();
        
        // Copy basic information
        response.setAssignmentId(basicResponse.getAssignmentId());
        response.setLoanId(basicResponse.getLoanId());
        response.setApplicantId(basicResponse.getApplicantId());
        response.setApplicantName(basicResponse.getApplicantName());
        response.setLoanType(basicResponse.getLoanType());
        response.setLoanAmount(basicResponse.getLoanAmount());
        response.setStatus(basicResponse.getStatus());
        response.setRemarks(basicResponse.getRemarks());
        response.setAssignedAt(basicResponse.getAssignedAt());
        response.setProcessedAt(basicResponse.getProcessedAt());
        response.setOfficerId(basicResponse.getOfficerId());
        response.setOfficerName(basicResponse.getOfficerName());
        response.setOfficerType(basicResponse.getOfficerType());
        
        // Copy compliance verdict information if available
        response.setComplianceVerdict(basicResponse.getComplianceVerdict());
        response.setComplianceVerdictReason(basicResponse.getComplianceVerdictReason());
        response.setComplianceRemarks(basicResponse.getComplianceRemarks());
        response.setComplianceOfficerName(basicResponse.getComplianceOfficerName());
        response.setComplianceVerdictTimestamp(basicResponse.getComplianceVerdictTimestamp());
        response.setNextAction(basicResponse.getNextAction());
        response.setHasComplianceVerdict(basicResponse.getHasComplianceVerdict());
        
        // Set enhanced scoring information
        NormalizedRiskScore normalizedScore = new NormalizedRiskScore();
        normalizedScore.setFinalScore(enhancedResult.getNormalizedScore());
        normalizedScore.setRiskLevel(enhancedResult.getFinalRiskLevel());
        normalizedScore.setScoreInterpretation(generateScoreInterpretation(enhancedResult.getNormalizedScore(), enhancedResult.getFinalRiskLevel()));
        response.setNormalizedRiskScore(normalizedScore);
        
        response.setScoringBreakdown(enhancedResult.getScoringBreakdown());
        response.setRuleViolations(enhancedResult.getRuleViolations());
        response.setFinalRecommendation(enhancedResult.getFinalRecommendation());
        
        // Set canApproveReject based on risk score OR compliance verdict availability
        boolean canApproveReject = enhancedResult.getNormalizedScore() < riskScoreThreshold;
        if (basicResponse.getHasComplianceVerdict() != null && basicResponse.getHasComplianceVerdict()) {
            canApproveReject = true; // Enable buttons when compliance verdict is available
        }
        response.setCanApproveReject(canApproveReject);
        
        return response;
    }
    
    private EnhancedLoanScreeningResponse convertToEnhancedResponseWithError(LoanScreeningResponse basicResponse, String errorMessage) {
        EnhancedLoanScreeningResponse response = new EnhancedLoanScreeningResponse();
        
        // Copy basic information
        response.setAssignmentId(basicResponse.getAssignmentId());
        response.setLoanId(basicResponse.getLoanId());
        response.setApplicantId(basicResponse.getApplicantId());
        response.setApplicantName(basicResponse.getApplicantName());
        response.setLoanType(basicResponse.getLoanType());
        response.setLoanAmount(basicResponse.getLoanAmount());
        response.setStatus(basicResponse.getStatus());
        response.setRemarks("Enhanced scoring error: " + errorMessage);
        response.setAssignedAt(basicResponse.getAssignedAt());
        response.setProcessedAt(basicResponse.getProcessedAt());
        response.setOfficerId(basicResponse.getOfficerId());
        response.setOfficerName(basicResponse.getOfficerName());
        response.setOfficerType(basicResponse.getOfficerType());
        
        // Copy compliance verdict information if available
        response.setComplianceVerdict(basicResponse.getComplianceVerdict());
        response.setComplianceVerdictReason(basicResponse.getComplianceVerdictReason());
        response.setComplianceRemarks(basicResponse.getComplianceRemarks());
        response.setComplianceOfficerName(basicResponse.getComplianceOfficerName());
        response.setComplianceVerdictTimestamp(basicResponse.getComplianceVerdictTimestamp());
        response.setNextAction(basicResponse.getNextAction());
        response.setHasComplianceVerdict(basicResponse.getHasComplianceVerdict());
        
        // Set default enhanced scoring information
        NormalizedRiskScore normalizedScore = new NormalizedRiskScore();
        normalizedScore.setFinalScore(75.0); // Default to medium-high risk
        normalizedScore.setRiskLevel("HIGH");
        normalizedScore.setScoreInterpretation("Unable to calculate enhanced score due to error. Defaulting to high risk for safety.");
        response.setNormalizedRiskScore(normalizedScore);
        
        response.setFinalRecommendation("REVIEW");
        response.setCanApproveReject(false);
        
        return response;
    }
    
    private String generateScoreInterpretation(Double score, String riskLevel) {
        if (score >= 80) {
            return String.format("Critical Risk (%.1f%%) - Multiple high-severity fraud indicators detected. Immediate rejection recommended.", score);
        } else if (score >= 60) {
            return String.format("High Risk (%.1f%%) - Significant fraud indicators present. Rejection or thorough review required.", score);
        } else if (score >= 35) {
            return String.format("Medium Risk (%.1f%%) - Some fraud indicators detected. Manual review recommended before approval.", score);
        } else if (score >= 15) {
            return String.format("Low Risk (%.1f%%) - Minor fraud indicators present. Standard approval process can proceed.", score);
        } else {
            return String.format("Clean (%.1f%%) - No significant fraud indicators detected. Safe for approval.", score);
        }
    }
}
