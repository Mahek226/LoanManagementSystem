package com.tss.springsecurity.controller;

import com.tss.springsecurity.dto.AdminAuthResponse;
import com.tss.springsecurity.dto.AdminLoginRequest;
import com.tss.springsecurity.dto.AdminRegisterRequest;
import com.tss.springsecurity.dto.DashboardStatsResponse;
import com.tss.springsecurity.entity.Applicant;
import com.tss.springsecurity.service.AdminService;
import com.tss.springsecurity.service.ApplicantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"}, allowCredentials = "true")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final ApplicantService applicantService;

    // Auth endpoints
    @PostMapping("/api/admin/auth/register")
    public ResponseEntity<?> register(@Valid @RequestBody AdminRegisterRequest registerRequest) {
        try {
            AdminAuthResponse response = adminService.register(registerRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/api/admin/auth/login")
    public ResponseEntity<?> login(@Valid @RequestBody AdminLoginRequest loginRequest) {
        try {
            AdminAuthResponse response = adminService.login(loginRequest);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    // Dashboard endpoints
    @GetMapping("/api/admin/dashboard/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        try {
            System.out.println("Getting dashboard stats...");
            DashboardStatsResponse stats = adminService.getDashboardStats();
            System.out.println("Dashboard stats: " + stats);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            System.err.println("Error getting dashboard stats: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // Applicant management endpoints
    @GetMapping("/api/admin/applicants")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Applicant>> getAllApplicants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Applicant> applicants = applicantService.getAllApplicants(pageable);
            return ResponseEntity.ok(applicants);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @GetMapping("/api/admin/applicants/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Applicant> getApplicantById(@PathVariable Long id) {
        try {
            Applicant applicant = applicantService.getApplicantById(id);
            return ResponseEntity.ok(applicant);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        }
    }

    @PutMapping("/api/admin/applicants/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approveApplicant(@PathVariable Long id, 
                                            @RequestBody(required = false) ApprovalRequest request) {
        try {
            String comments = request != null ? request.comments() : null;
            applicantService.approveApplicant(id, comments);
            return ResponseEntity.ok(new SuccessResponse("Applicant approved successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/api/admin/applicants/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rejectApplicant(@PathVariable Long id, 
                                           @RequestBody ApprovalRequest request) {
        try {
            applicantService.rejectApplicant(id, request.comments());
            return ResponseEntity.ok(new SuccessResponse("Applicant rejected successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/api/admin/applicants/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Applicant>> searchApplicants(@RequestParam String q) {
        try {
            List<Applicant> applicants = applicantService.searchApplicants(q);
            return ResponseEntity.ok(applicants);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @GetMapping("/api/admin/applicants/filter")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Applicant>> filterApplicants(@RequestParam String status) {
        try {
            List<Applicant> applicants = applicantService.filterApplicantsByStatus(status);
            return ResponseEntity.ok(applicants);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    // Inner classes for responses
    private record ErrorResponse(String message) {}
    private record SuccessResponse(String message) {}
    private record ApprovalRequest(String comments) {}
}
