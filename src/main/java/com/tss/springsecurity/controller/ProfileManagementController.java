package com.tss.springsecurity.controller;

import com.tss.springsecurity.dto.ComplianceOfficerProfileResponse;
import com.tss.springsecurity.dto.ProfileUpdateRequest;
import com.tss.springsecurity.entity.Applicant;
import com.tss.springsecurity.entity.ComplianceOfficer;
import com.tss.springsecurity.entity.LoanOfficer;
import com.tss.springsecurity.payload.response.MessageResponse;
import com.tss.springsecurity.service.ProfileManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"}, allowCredentials = "true")
@RequiredArgsConstructor
public class ProfileManagementController {

    private final ProfileManagementService profileManagementService;

    // ==================== APPLICANT PROFILE ====================
    
    @GetMapping("/applicant/{applicantId}")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<?> getApplicantProfile(@PathVariable Long applicantId) {
        Applicant applicant = profileManagementService.getApplicantById(applicantId);
        return ResponseEntity.ok(applicant);
    }

    @PutMapping("/applicant/{applicantId}")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<?> updateApplicantProfile(
            @PathVariable Long applicantId,
            @Valid @RequestBody ProfileUpdateRequest request) {
        
        profileManagementService.updateApplicantProfile(applicantId, request);
        return ResponseEntity.ok(new MessageResponse("Profile updated successfully"));
    }

    // ==================== LOAN OFFICER PROFILE ====================
    
    @GetMapping("/loan-officer/{officerId}")
    @PreAuthorize("hasRole('LOAN_OFFICER')")
    public ResponseEntity<?> getLoanOfficerProfile(@PathVariable Long officerId) {
        LoanOfficer officer = profileManagementService.getLoanOfficerById(officerId);
        return ResponseEntity.ok(officer);
    }

    @PutMapping("/loan-officer/{officerId}")
    @PreAuthorize("hasRole('LOAN_OFFICER')")
    public ResponseEntity<?> updateLoanOfficerProfile(
            @PathVariable Long officerId,
            @Valid @RequestBody ProfileUpdateRequest request) {
        
        try {
            profileManagementService.updateLoanOfficerProfile(officerId, request);
            return ResponseEntity.ok(new MessageResponse("Profile updated successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(e.getMessage()));
        }
    }

    // ==================== COMPLIANCE OFFICER PROFILE ====================
    
    @GetMapping("/compliance-officer/{officerId}")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<?> getComplianceOfficerProfile(@PathVariable Long officerId) {
        ComplianceOfficer officer = profileManagementService.getComplianceOfficerById(officerId);
        
        // Convert to DTO to avoid circular reference
        ComplianceOfficerProfileResponse response = ComplianceOfficerProfileResponse.builder()
                .officerId(officer.getOfficerId())
                .username(officer.getUsername())
                .email(officer.getEmail())
                .firstName(officer.getFirstName())
                .lastName(officer.getLastName())
                .loanType(officer.getLoanType())
                .createdAt(officer.getCreatedAt())
                .updatedAt(officer.getUpdatedAt())
                .build();
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/compliance-officer/{officerId}")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<?> updateComplianceOfficerProfile(
            @PathVariable Long officerId,
            @Valid @RequestBody ProfileUpdateRequest request) {
        
        try {
            profileManagementService.updateComplianceOfficerProfile(officerId, request);
            return ResponseEntity.ok(new MessageResponse("Profile updated successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(e.getMessage()));
        }
    }

    // ==================== PASSWORD CHANGE (ALL USERS) ====================
    
    @PostMapping("/change-password/{userType}/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(
            @PathVariable String userType,
            @PathVariable Long userId,
            @RequestBody PasswordChangeRequest request) {
        
        try {
            profileManagementService.changePassword(userType, userId, request.getCurrentPassword(), request.getNewPassword());
            return ResponseEntity.ok(new MessageResponse("Password changed successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(e.getMessage()));
        }
    }


    // ==================== INNER CLASSES ====================
    
    @lombok.Data
    public static class PasswordChangeRequest {
        private String currentPassword;
        private String newPassword;
    }
}
