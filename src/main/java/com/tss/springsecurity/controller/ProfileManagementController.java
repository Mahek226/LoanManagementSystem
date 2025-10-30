package com.tss.springsecurity.controller;

import com.tss.springsecurity.dto.ProfileUpdateRequest;
import com.tss.springsecurity.entity.Applicant;
import com.tss.springsecurity.entity.ComplianceOfficer;
import com.tss.springsecurity.entity.LoanOfficer;
import com.tss.springsecurity.payload.response.MessageResponse;
import com.tss.springsecurity.repository.ApplicantRepository;
import com.tss.springsecurity.repository.ComplianceOfficerRepository;
import com.tss.springsecurity.repository.LoanOfficerRepository;
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

    private final ApplicantRepository applicantRepository;
    private final LoanOfficerRepository loanOfficerRepository;
    private final ComplianceOfficerRepository complianceOfficerRepository;
    private final PasswordEncoder passwordEncoder;

    // ==================== APPLICANT PROFILE ====================
    
    @GetMapping("/applicant/{applicantId}")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<?> getApplicantProfile(@PathVariable Long applicantId) {
        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("Applicant not found"));
        
        return ResponseEntity.ok(applicant);
    }

    @PutMapping("/applicant/{applicantId}")
    @PreAuthorize("hasRole('APPLICANT')")
    public ResponseEntity<?> updateApplicantProfile(
            @PathVariable Long applicantId,
            @Valid @RequestBody ProfileUpdateRequest request) {
        
        Applicant applicant = applicantRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("Applicant not found"));

        // Update allowed fields
        if (request.getFirstName() != null) {
            applicant.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            applicant.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            applicant.setPhone(request.getPhone());
        }
        if (request.getAddress() != null) {
            applicant.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            applicant.setCity(request.getCity());
        }
        if (request.getState() != null) {
            applicant.setState(request.getState());
        }
        if (request.getCountry() != null) {
            applicant.setCountry(request.getCountry());
        }

        applicantRepository.save(applicant);
        
        return ResponseEntity.ok(new MessageResponse("Profile updated successfully"));
    }

    // ==================== LOAN OFFICER PROFILE ====================
    
    @GetMapping("/loan-officer/{officerId}")
    @PreAuthorize("hasRole('LOAN_OFFICER')")
    public ResponseEntity<?> getLoanOfficerProfile(@PathVariable Long officerId) {
        LoanOfficer officer = loanOfficerRepository.findById(officerId)
                .orElseThrow(() -> new RuntimeException("Loan Officer not found"));
        
        return ResponseEntity.ok(officer);
    }

    @PutMapping("/loan-officer/{officerId}")
    @PreAuthorize("hasRole('LOAN_OFFICER')")
    public ResponseEntity<?> updateLoanOfficerProfile(
            @PathVariable Long officerId,
            @Valid @RequestBody ProfileUpdateRequest request) {
        
        LoanOfficer officer = loanOfficerRepository.findById(officerId)
                .orElseThrow(() -> new RuntimeException("Loan Officer not found"));

        // Update allowed fields
        if (request.getFirstName() != null) {
            officer.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            officer.setLastName(request.getLastName());
        }
        if (request.getEmail() != null && !request.getEmail().equals(officer.getEmail())) {
            // Check if email already exists
            if (loanOfficerRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Email already in use"));
            }
            officer.setEmail(request.getEmail());
        }

        loanOfficerRepository.save(officer);
        
        return ResponseEntity.ok(new MessageResponse("Profile updated successfully"));
    }

    // ==================== COMPLIANCE OFFICER PROFILE ====================
    
    @GetMapping("/compliance-officer/{officerId}")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<?> getComplianceOfficerProfile(@PathVariable Long officerId) {
        ComplianceOfficer officer = complianceOfficerRepository.findById(officerId)
                .orElseThrow(() -> new RuntimeException("Compliance Officer not found"));
        
        return ResponseEntity.ok(officer);
    }

    @PutMapping("/compliance-officer/{officerId}")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<?> updateComplianceOfficerProfile(
            @PathVariable Long officerId,
            @Valid @RequestBody ProfileUpdateRequest request) {
        
        ComplianceOfficer officer = complianceOfficerRepository.findById(officerId)
                .orElseThrow(() -> new RuntimeException("Compliance Officer not found"));

        // Update allowed fields
        if (request.getFirstName() != null) {
            officer.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            officer.setLastName(request.getLastName());
        }
        if (request.getEmail() != null && !request.getEmail().equals(officer.getEmail())) {
            // Check if email already exists
            if (complianceOfficerRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Email already in use"));
            }
            officer.setEmail(request.getEmail());
        }

        complianceOfficerRepository.save(officer);
        
        return ResponseEntity.ok(new MessageResponse("Profile updated successfully"));
    }

    // ==================== PASSWORD CHANGE (ALL USERS) ====================
    
    @PostMapping("/change-password/{userType}/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(
            @PathVariable String userType,
            @PathVariable Long userId,
            @RequestBody PasswordChangeRequest request) {
        
        switch (userType.toUpperCase()) {
            case "APPLICANT":
                return changeApplicantPassword(userId, request);
            case "LOAN_OFFICER":
                return changeLoanOfficerPassword(userId, request);
            case "COMPLIANCE_OFFICER":
                return changeComplianceOfficerPassword(userId, request);
            default:
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Invalid user type"));
        }
    }

    private ResponseEntity<?> changeApplicantPassword(Long userId, PasswordChangeRequest request) {
        Applicant applicant = applicantRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Applicant not found"));
        
        if (!passwordEncoder.matches(request.getCurrentPassword(), applicant.getPasswordHash())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Current password is incorrect"));
        }
        
        applicant.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        applicantRepository.save(applicant);
        
        return ResponseEntity.ok(new MessageResponse("Password changed successfully"));
    }

    private ResponseEntity<?> changeLoanOfficerPassword(Long userId, PasswordChangeRequest request) {
        LoanOfficer officer = loanOfficerRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Loan Officer not found"));
        
        if (!passwordEncoder.matches(request.getCurrentPassword(), officer.getPasswordHash())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Current password is incorrect"));
        }
        
        officer.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        loanOfficerRepository.save(officer);
        
        return ResponseEntity.ok(new MessageResponse("Password changed successfully"));
    }

    private ResponseEntity<?> changeComplianceOfficerPassword(Long userId, PasswordChangeRequest request) {
        ComplianceOfficer officer = complianceOfficerRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Compliance Officer not found"));
        
        if (!passwordEncoder.matches(request.getCurrentPassword(), officer.getPasswordHash())) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Current password is incorrect"));
        }
        
        officer.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        complianceOfficerRepository.save(officer);
        
        return ResponseEntity.ok(new MessageResponse("Password changed successfully"));
    }

    // ==================== INNER CLASSES ====================
    
    @lombok.Data
    public static class PasswordChangeRequest {
        private String currentPassword;
        private String newPassword;
    }
}
