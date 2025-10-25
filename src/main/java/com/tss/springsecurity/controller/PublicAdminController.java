package com.tss.springsecurity.controller;

import com.tss.springsecurity.dto.DashboardStatsResponse;
import com.tss.springsecurity.entity.Applicant;
import com.tss.springsecurity.service.AdminService;
import com.tss.springsecurity.service.ApplicantService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/admin")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"}, allowCredentials = "true")
@RequiredArgsConstructor
public class PublicAdminController {

    private final AdminService adminService;
    private final ApplicantService applicantService;

    @GetMapping("/dashboard/stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        try {
            System.out.println("Getting dashboard stats (public endpoint)...");
            DashboardStatsResponse stats = adminService.getDashboardStats();
            System.out.println("Dashboard stats: " + stats);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            System.err.println("Error getting dashboard stats: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @GetMapping("/applicants")
    public ResponseEntity<Page<Applicant>> getAllApplicants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            System.out.println("Getting all applicants (public endpoint)...");
            Pageable pageable = PageRequest.of(page, size);
            Page<Applicant> applicants = applicantService.getAllApplicants(pageable);
            System.out.println("Found " + applicants.getTotalElements() + " applicants");
            return ResponseEntity.ok(applicants);
        } catch (Exception e) {
            System.err.println("Error getting applicants: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @PutMapping("/applicants/{id}/approve")
    public ResponseEntity<?> approveApplicant(@PathVariable Long id, 
                                            @RequestBody(required = false) ApprovalRequest request) {
        try {
            System.out.println("Approving applicant with ID: " + id);
            String comments = request != null ? request.comments() : null;
            applicantService.approveApplicant(id, comments);
            return ResponseEntity.ok(new SuccessResponse("Applicant approved successfully"));
        } catch (Exception e) {
            System.err.println("Error approving applicant: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @PutMapping("/applicants/{id}/reject")
    public ResponseEntity<?> rejectApplicant(@PathVariable Long id, 
                                           @RequestBody ApprovalRequest request) {
        try {
            System.out.println("Rejecting applicant with ID: " + id);
            applicantService.rejectApplicant(id, request.comments());
            return ResponseEntity.ok(new SuccessResponse("Applicant rejected successfully"));
        } catch (Exception e) {
            System.err.println("Error rejecting applicant: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // Inner classes for responses
    private record SuccessResponse(String message) {}
    private record ApprovalRequest(String comments) {}
}
