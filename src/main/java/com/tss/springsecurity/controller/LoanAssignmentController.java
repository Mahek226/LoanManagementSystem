package com.tss.springsecurity.controller;

import com.tss.springsecurity.dto.LoanAssignmentRequest;
import com.tss.springsecurity.dto.LoanAssignmentResponse;
import com.tss.springsecurity.dto.OfficerSummary;
import com.tss.springsecurity.service.LoanAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/loan-assignments")
@RequiredArgsConstructor
public class LoanAssignmentController {
    
    private final LoanAssignmentService loanAssignmentService;
    
    @PostMapping("/assign")
    public ResponseEntity<?> assignLoanToOfficer(@Valid @RequestBody LoanAssignmentRequest request) {
        try {
            LoanAssignmentResponse response = loanAssignmentService.assignLoanToOfficer(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @PutMapping("/{assignmentId}/reassign/{newOfficerId}")
    public ResponseEntity<?> reassignLoan(
            @PathVariable Long assignmentId,
            @PathVariable Long newOfficerId,
            @RequestParam(required = false) String remarks) {
        try {
            LoanAssignmentResponse response = loanAssignmentService.reassignLoan(assignmentId, newOfficerId, remarks);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @PutMapping("/{assignmentId}/status")
    public ResponseEntity<?> updateAssignmentStatus(
            @PathVariable Long assignmentId,
            @RequestParam String status,
            @RequestParam(required = false) String remarks) {
        try {
            LoanAssignmentResponse response = loanAssignmentService.updateAssignmentStatus(assignmentId, status, remarks);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @GetMapping
    public ResponseEntity<List<LoanAssignmentResponse>> getAllAssignments() {
        List<LoanAssignmentResponse> assignments = loanAssignmentService.getAllAssignments();
        return ResponseEntity.ok(assignments);
    }
    
    @GetMapping("/{assignmentId}")
    public ResponseEntity<?> getAssignmentById(@PathVariable Long assignmentId) {
        try {
            LoanAssignmentResponse response = loanAssignmentService.getAssignmentById(assignmentId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }
    
    @GetMapping("/officer/{officerId}")
    public ResponseEntity<List<LoanAssignmentResponse>> getAssignmentsByOfficer(@PathVariable Long officerId) {
        List<LoanAssignmentResponse> assignments = loanAssignmentService.getAssignmentsByOfficer(officerId);
        return ResponseEntity.ok(assignments);
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<LoanAssignmentResponse>> getAssignmentsByStatus(@PathVariable String status) {
        List<LoanAssignmentResponse> assignments = loanAssignmentService.getAssignmentsByStatus(status);
        return ResponseEntity.ok(assignments);
    }
    
    @GetMapping("/available-officers/{loanType}")
    public ResponseEntity<List<OfficerSummary>> getAvailableOfficersByLoanType(@PathVariable String loanType) {
        List<OfficerSummary> officers = loanAssignmentService.getAvailableOfficersByLoanType(loanType);
        return ResponseEntity.ok(officers);
    }
    
    // ==================== Response Classes ====================
    
    private record ErrorResponse(String message) {}
    
    private record SuccessResponse(String message) {}
}
