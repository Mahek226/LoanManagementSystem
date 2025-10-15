package com.tss.springsecurity.controller;

import com.tss.springsecurity.dto.LoanScreeningRequest;
import com.tss.springsecurity.dto.LoanScreeningResponse;
import com.tss.springsecurity.service.LoanOfficerScreeningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/compliance-officer")
@RequiredArgsConstructor
public class ComplianceOfficerController {
    
    private final LoanOfficerScreeningService screeningService;
    
    @GetMapping("/escalations")
    public ResponseEntity<List<LoanScreeningResponse>> getComplianceEscalations() {
        List<LoanScreeningResponse> escalations = screeningService.getComplianceEscalations();
        return ResponseEntity.ok(escalations);
    }
    
    @PostMapping("/{complianceOfficerId}/process-decision")
    public ResponseEntity<?> processComplianceDecision(
            @PathVariable Long complianceOfficerId,
            @Valid @RequestBody LoanScreeningRequest request) {
        try {
            LoanScreeningResponse response = screeningService.processComplianceDecision(complianceOfficerId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @GetMapping("/assignment/{assignmentId}/details")
    public ResponseEntity<?> getComplianceAssignmentDetails(@PathVariable Long assignmentId) {
        try {
            // This will be handled by the same service method but for compliance assignments
            LoanScreeningResponse response = screeningService.getLoanDetailsForScreening(assignmentId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    // ==================== Response Classes ====================
    
    private record ErrorResponse(String message) {}
    
    private record SuccessResponse(String message) {}
}
