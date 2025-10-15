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
@RequestMapping("/api/loan-officer")
@RequiredArgsConstructor
public class LoanOfficerScreeningController {
    
    private final LoanOfficerScreeningService screeningService;
    
    @GetMapping("/{officerId}/assigned-loans")
    public ResponseEntity<List<LoanScreeningResponse>> getAssignedLoans(@PathVariable Long officerId) {
        List<LoanScreeningResponse> loans = screeningService.getAssignedLoansForOfficer(officerId);
        return ResponseEntity.ok(loans);
    }
    
    @GetMapping("/assignment/{assignmentId}/details")
    public ResponseEntity<?> getLoanDetailsForScreening(@PathVariable Long assignmentId) {
        try {
            LoanScreeningResponse response = screeningService.getLoanDetailsForScreening(assignmentId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @PostMapping("/{officerId}/process-screening")
    public ResponseEntity<?> processLoanScreening(
            @PathVariable Long officerId,
            @Valid @RequestBody LoanScreeningRequest request) {
        try {
            LoanScreeningResponse response = screeningService.processLoanScreening(officerId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @PostMapping("/assignment/{assignmentId}/escalate")
    public ResponseEntity<?> escalateToCompliance(
            @PathVariable Long assignmentId,
            @RequestParam(required = false) String remarks) {
        try {
            LoanScreeningResponse response = screeningService.escalateToCompliance(assignmentId, remarks);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    // ==================== Response Classes ====================
    
    private record ErrorResponse(String message) {}
    
    private record SuccessResponse(String message) {}
}
