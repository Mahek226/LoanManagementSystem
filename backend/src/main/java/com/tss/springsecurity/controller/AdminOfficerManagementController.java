package com.tss.springsecurity.controller;

import com.tss.springsecurity.dto.ComplianceOfficerRequest;
import com.tss.springsecurity.dto.LoanOfficerRequest;
import com.tss.springsecurity.dto.OfficerResponse;
import com.tss.springsecurity.service.OfficerManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/officers")
@RequiredArgsConstructor
public class AdminOfficerManagementController {

    private final OfficerManagementService officerManagementService;

    // ==================== Loan Officer Endpoints ====================
    
    @PostMapping("/loan-officers")
    public ResponseEntity<?> addLoanOfficer(@Valid @RequestBody LoanOfficerRequest request) {
        try {
            OfficerResponse response = officerManagementService.addLoanOfficer(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/loan-officers")
    public ResponseEntity<List<OfficerResponse>> getAllLoanOfficers() {
        List<OfficerResponse> officers = officerManagementService.getAllLoanOfficers();
        return ResponseEntity.ok(officers);
    }

    @GetMapping("/loan-officers/{id}")
    public ResponseEntity<?> getLoanOfficerById(@PathVariable Long id) {
        try {
            OfficerResponse response = officerManagementService.getLoanOfficerById(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/loan-officers/{id}")
    public ResponseEntity<?> deleteLoanOfficer(@PathVariable Long id) {
        try {
            officerManagementService.deleteLoanOfficer(id);
            return ResponseEntity.ok(new SuccessResponse("Loan Officer deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    // ==================== Compliance Officer Endpoints ====================
    
    @PostMapping("/compliance-officers")
    public ResponseEntity<?> addComplianceOfficer(@Valid @RequestBody ComplianceOfficerRequest request) {
        try {
            OfficerResponse response = officerManagementService.addComplianceOfficer(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/compliance-officers")
    public ResponseEntity<List<OfficerResponse>> getAllComplianceOfficers() {
        List<OfficerResponse> officers = officerManagementService.getAllComplianceOfficers();
        return ResponseEntity.ok(officers);
    }

    @GetMapping("/compliance-officers/{id}")
    public ResponseEntity<?> getComplianceOfficerById(@PathVariable Long id) {
        try {
            OfficerResponse response = officerManagementService.getComplianceOfficerById(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/compliance-officers/{id}")
    public ResponseEntity<?> deleteComplianceOfficer(@PathVariable Long id) {
        try {
            officerManagementService.deleteComplianceOfficer(id);
            return ResponseEntity.ok(new SuccessResponse("Compliance Officer deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    // ==================== Response Classes ====================
    
    private record ErrorResponse(String message) {}
    
    private record SuccessResponse(String message) {}
}
