package com.tss.springsecurity.controller;

import com.tss.springsecurity.dto.ApprovalResponse;
import com.tss.springsecurity.service.ApplicantAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/applicant-approvals")
@RequiredArgsConstructor
public class AdminApprovalController {

    private final ApplicantAuthService applicantAuthService;

    @GetMapping("/pending")
    public ResponseEntity<List<ApprovalResponse>> getPendingApprovals() {
        List<ApprovalResponse> pendingApprovals = applicantAuthService.getPendingApprovals();
        return ResponseEntity.ok(pendingApprovals);
    }

    @PutMapping("/{applicantId}/approve")
    public ResponseEntity<?> approveApplicant(@PathVariable Long applicantId) {
        try {
            ApprovalResponse response = applicantAuthService.approveApplicant(applicantId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/{applicantId}/reject")
    public ResponseEntity<?> rejectApplicant(@PathVariable Long applicantId) {
        try {
            ApprovalResponse response = applicantAuthService.rejectApplicant(applicantId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    private record ErrorResponse(String message) {}
}
