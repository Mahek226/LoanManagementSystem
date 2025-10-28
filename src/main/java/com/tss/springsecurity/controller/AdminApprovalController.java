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
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"}, allowCredentials = "true")
@RequiredArgsConstructor
public class AdminApprovalController {

    private final ApplicantAuthService applicantAuthService;

    @GetMapping("/pending")
    public ResponseEntity<List<ApprovalResponse>> getPendingApprovals() {
        List<ApprovalResponse> pendingApprovals = applicantAuthService.getPendingApprovals();
        return ResponseEntity.ok(pendingApprovals);
    }

    @PutMapping("/{applicantId}/approve")
    public ResponseEntity<?> approveApplicant(@PathVariable Long applicantId, 
                                            @RequestBody(required = false) CommentsRequest request) {
        try {
            String comments = request != null ? request.comments() : null;
            ApprovalResponse response = applicantAuthService.approveApplicant(applicantId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/{applicantId}/reject")
    public ResponseEntity<?> rejectApplicant(@PathVariable Long applicantId,
                                           @RequestBody(required = false) CommentsRequest request) {
        try {
            String comments = request != null ? request.comments() : null;
            ApprovalResponse response = applicantAuthService.rejectApplicant(applicantId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    private record ErrorResponse(String message) {}
    private record CommentsRequest(String comments) {}
}
