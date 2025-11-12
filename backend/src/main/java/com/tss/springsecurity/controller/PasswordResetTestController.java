package com.tss.springsecurity.controller;

import com.tss.springsecurity.dto.ResetPasswordRequest;
import com.tss.springsecurity.entity.*;
import com.tss.springsecurity.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/test")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
@RequiredArgsConstructor
@Slf4j
public class PasswordResetTestController {

    private final AdminRepository adminRepository;
    private final ApplicantRepository applicantRepository;
    private final LoanOfficerRepository loanOfficerRepository;
    private final ComplianceOfficerRepository complianceOfficerRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/reset-password-simple")
    @Transactional
    public ResponseEntity<?> resetPasswordSimple(@RequestBody ResetPasswordRequest request) {
        try {
            log.info("Starting simple password reset for email: {}", request.getEmail());
            
            // Validate password confirmation
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.badRequest().body(createErrorResponse("Passwords do not match"));
            }
            
            // Find valid reset token
            PasswordResetToken resetToken = passwordResetTokenRepository
                    .findByResetTokenAndIsUsedFalseAndExpiresAtAfter(request.getResetToken(), LocalDateTime.now())
                    .orElse(null);
            
            if (resetToken == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("Invalid or expired reset token"));
            }
            
            // Verify email matches
            if (!resetToken.getEmail().equals(request.getEmail())) {
                return ResponseEntity.badRequest().body(createErrorResponse("Email mismatch"));
            }
            
            log.info("Found valid reset token for user type: {}", resetToken.getUserType());
            
            // Update password based on user type
            String encodedPassword = passwordEncoder.encode(request.getNewPassword());
            boolean userUpdated = false;
            
            switch (resetToken.getUserType()) {
                case "ADMIN":
                    var admin = adminRepository.findByEmail(request.getEmail()).orElse(null);
                    if (admin != null) {
                        admin.setPassword(encodedPassword);
                        adminRepository.save(admin);
                        userUpdated = true;
                        log.info("Updated admin password");
                    }
                    break;
                    
                case "APPLICANT":
                    var applicant = applicantRepository.findByEmail(request.getEmail()).orElse(null);
                    if (applicant != null) {
                        applicant.setPassword(encodedPassword);
                        applicantRepository.save(applicant);
                        userUpdated = true;
                        log.info("Updated applicant password");
                    }
                    break;
                    
                case "LOAN_OFFICER":
                    var loanOfficer = loanOfficerRepository.findByEmail(request.getEmail()).orElse(null);
                    if (loanOfficer != null) {
                        loanOfficer.setPassword(encodedPassword);
                        loanOfficerRepository.save(loanOfficer);
                        userUpdated = true;
                        log.info("Updated loan officer password");
                    }
                    break;
                    
                case "COMPLIANCE_OFFICER":
                    var complianceOfficer = complianceOfficerRepository.findByEmail(request.getEmail()).orElse(null);
                    if (complianceOfficer != null) {
                        complianceOfficer.setPassword(encodedPassword);
                        complianceOfficerRepository.save(complianceOfficer);
                        userUpdated = true;
                        log.info("Updated compliance officer password");
                    }
                    break;
                    
                default:
                    return ResponseEntity.badRequest().body(createErrorResponse("Invalid user type: " + resetToken.getUserType()));
            }
            
            if (!userUpdated) {
                return ResponseEntity.badRequest().body(createErrorResponse("User not found"));
            }
            
            // Mark token as used
            resetToken.setIsUsed(true);
            resetToken.setUsedAt(LocalDateTime.now());
            passwordResetTokenRepository.save(resetToken);
            
            log.info("Password reset completed successfully for: {}", request.getEmail());
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Password reset successfully. You can now login with your new password.");
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error during password reset", e);
            return ResponseEntity.status(500).body(createErrorResponse("Internal server error: " + e.getMessage()));
        }
    }
    
    @GetMapping("/check-token/{token}")
    public ResponseEntity<?> checkToken(@PathVariable String token) {
        try {
            PasswordResetToken resetToken = passwordResetTokenRepository
                    .findByResetTokenAndIsUsedFalseAndExpiresAtAfter(token, LocalDateTime.now())
                    .orElse(null);
            
            if (resetToken == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("Invalid or expired token"));
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("email", resetToken.getEmail());
            response.put("userType", resetToken.getUserType());
            response.put("expiresAt", resetToken.getExpiresAt());
            response.put("isValid", true);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error checking token", e);
            return ResponseEntity.status(500).body(createErrorResponse("Error: " + e.getMessage()));
        }
    }
    
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        error.put("status", "failed");
        error.put("timestamp", LocalDateTime.now().toString());
        return error;
    }
}
