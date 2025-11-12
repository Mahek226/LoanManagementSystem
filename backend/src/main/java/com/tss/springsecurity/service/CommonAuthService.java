//package com.tss.springsecurity.service;
//
//import com.tss.springsecurity.dto.CommonAuthResponse;
//import com.tss.springsecurity.dto.CommonLoginRequest;
//import com.tss.springsecurity.dto.ForgotPasswordRequest;
//import com.tss.springsecurity.dto.ResetPasswordRequest;
//import com.tss.springsecurity.entity.Admin;
//import com.tss.springsecurity.entity.Applicant;
//import com.tss.springsecurity.entity.LoanOfficer;
//import com.tss.springsecurity.entity.ComplianceOfficer;
//import com.tss.springsecurity.repository.AdminRepository;
//import com.tss.springsecurity.repository.ApplicantRepository;
//import com.tss.springsecurity.repository.LoanOfficerRepository;
//import com.tss.springsecurity.repository.ComplianceOfficerRepository;
//import com.tss.springsecurity.repository.PasswordResetTokenRepository;
//import com.tss.springsecurity.entity.PasswordResetToken;
//import com.tss.springsecurity.service.EmailService;
//import com.tss.springsecurity.util.JwtUtil;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Service;
//
//import java.util.Optional;
//import java.util.UUID;
//import java.time.LocalDateTime;
//
//@Service
//@RequiredArgsConstructor
//public class CommonAuthService {
//
//    private final AdminRepository adminRepository;
//    private final ApplicantRepository applicantRepository;
//    private final LoanOfficerRepository loanOfficerRepository;
//    private final ComplianceOfficerRepository complianceOfficerRepository;
//    private final PasswordResetTokenRepository passwordResetTokenRepository;
//    private final PasswordEncoder passwordEncoder;
//    private final JwtUtil jwtUtil;
//    private final EmailService emailService;
//
//    public CommonAuthResponse login(CommonLoginRequest request) {
//        String username = request.getUsername();
//        String password = request.getPassword();
//        String userType = request.getUserType();
//
//        // Try Admin login first
//        Optional<Admin> admin = adminRepository.findByUsernameOrEmail(username, username);
//        if (admin.isPresent() && passwordEncoder.matches(password, admin.get().getPassword())) {
//            String token = jwtUtil.generateToken(admin.get().getUsername(), "ADMIN");
//            return new CommonAuthResponse(
//                admin.get().getAdminId(),
//                null, // Admin has no officerId
//                admin.get().getUsername(),
//                admin.get().getEmail(),
//                "ADMIN",
//                token
//            );
//        }
//
//        // Try Applicant login
//        Optional<Applicant> applicant = applicantRepository.findByEmail(usernameOrEmail);
//        if (applicant.isPresent() && passwordEncoder.matches(password, applicant.get().getPassword())) {
//            Applicant app = applicant.get();
//            
//            // Check if applicant is approved and email verified
//            if (!app.getIsEmailVerified()) {
//                throw new RuntimeException("Please verify your email before logging in");
//            }
//            if (!app.getIsApproved()) {
//                throw new RuntimeException("Your account is pending admin approval");
//            }
//            
//            String token = jwtUtil.generateToken(app.getEmail(), "APPLICANT");
//            return new CommonAuthResponse(
//                app.getApplicantId(), // userId
//                app.getApplicantId(), // applicantId
//                null, // Applicants don't have username
//                app.getFirstName(),
//                app.getLastName(),
//                app.getEmail(),
//                "APPLICANT",
//                token
//            );
//        }
//
//        // Try Loan Officer login
//        Optional<LoanOfficer> loanOfficer = loanOfficerRepository.findByUsernameOrEmail(username, username);
//        if (loanOfficer.isPresent() && passwordEncoder.matches(password, loanOfficer.get().getPassword())) {
//            String token = jwtUtil.generateToken(loanOfficer.get().getUsername(), "LOAN_OFFICER");
//            return new CommonAuthResponse(
//                loanOfficer.get().getOfficerId(), // userId
//                loanOfficer.get().getOfficerId(), // officerId
//                loanOfficer.get().getUsername(),
//                loanOfficer.get().getEmail(),
//                "LOAN_OFFICER",
//                token
//            );
//        }
//
//        // Try Compliance Officer login
//        Optional<ComplianceOfficer> complianceOfficer = complianceOfficerRepository.findByUsernameOrEmail(username, username);
//        if (complianceOfficer.isPresent() && passwordEncoder.matches(password, complianceOfficer.get().getPassword())) {
//            String token = jwtUtil.generateToken(complianceOfficer.get().getUsername(), "COMPLIANCE_OFFICER");
//            return new CommonAuthResponse(
//                complianceOfficer.get().getOfficerId(), // userId
//                complianceOfficer.get().getOfficerId(), // officerId
//                complianceOfficer.get().getUsername(),
//                complianceOfficer.get().getEmail(),
//                "COMPLIANCE_OFFICER",
//                token
//            );
//        }
//
//        // Try Applicant login
//        Optional<Applicant> applicant = applicantRepository.findByEmail(username);
//        if (applicant.isPresent() && passwordEncoder.matches(password, applicant.get().getPassword())) {
//            Applicant app = applicant.get();
//            
//            // Check if applicant is approved and email verified
//            if (!app.getIsEmailVerified()) {
//                throw new RuntimeException("Please verify your email before logging in");
//            }
//            if (!app.getIsApproved()) {
//                throw new RuntimeException("Your account is pending admin approval");
//            }
//            
//            String token = jwtUtil.generateToken(app.getEmail(), "APPLICANT");
//            return new CommonAuthResponse(
//                app.getApplicantId(),
//                null, // Applicants don't have username
//                app.getFirstName(),
//                app.getLastName(),
//                app.getEmail(),
//                "APPLICANT",
//                token
//            );
//        }
//
//        // If no user found or password doesn't match
//        throw new RuntimeException("Invalid username or password");
//    }
//    
//    public String forgotPassword(ForgotPasswordRequest request) {
//        String email = request.getEmail();
//        String userType = null;
//        
//        // Check which type of user exists with this email
//        if (adminRepository.findByEmail(email).isPresent()) {
//            userType = "ADMIN";
//        } else if (applicantRepository.findByEmail(email).isPresent()) {
//            userType = "APPLICANT";
//        } else if (loanOfficerRepository.findByEmail(email).isPresent()) {
//            userType = "LOAN_OFFICER";
//        } else if (complianceOfficerRepository.findByEmail(email).isPresent()) {
//            userType = "COMPLIANCE_OFFICER";
//        }
//        
//        // Only send email if user exists
//        if (userType != null) {
//            // Generate reset token
//            String resetToken = UUID.randomUUID().toString();
//            
//            // Save reset token
//            PasswordResetToken passwordResetToken = new PasswordResetToken();
//            passwordResetToken.setEmail(email);
//            passwordResetToken.setUserType(userType);
//            passwordResetToken.setResetToken(resetToken);
//            passwordResetToken.setExpiresAt(LocalDateTime.now().plusHours(1));
//            passwordResetTokenRepository.save(passwordResetToken);
//            
//            // Send reset email
//            emailService.sendPasswordResetEmail(email, resetToken);
//        }
//        
//        // Always return the same message regardless of whether email exists
//        return "Reset password link has been shared to you in email.";
//    }
//    
//    public String resetPassword(ResetPasswordRequest request) {
//        // Validate password confirmation
//        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
//            throw new RuntimeException("New password and confirm password do not match");
//        }
//        
//        // Find valid reset token
//        PasswordResetToken resetToken = passwordResetTokenRepository
//                .findByResetTokenAndIsUsedFalseAndExpiresAtAfter(request.getResetToken(), LocalDateTime.now())
//                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));
//        
//        // Verify email matches
//        if (!resetToken.getEmail().equals(request.getEmail())) {
//            throw new RuntimeException("Invalid reset token");
//        }
//        
//        // Update password based on user type
//        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
//        
//        switch (resetToken.getUserType()) {
//            case "ADMIN":
//                Admin admin = adminRepository.findByEmail(request.getEmail())
//                        .orElseThrow(() -> new RuntimeException("User not found"));
//                admin.setPasswordHash(encodedPassword);
//                adminRepository.save(admin);
//                break;
//                
//            case "APPLICANT":
//                Applicant applicant = applicantRepository.findByEmail(request.getEmail())
//                        .orElseThrow(() -> new RuntimeException("User not found"));
//                applicant.setPasswordHash(encodedPassword);
//                applicantRepository.save(applicant);
//                break;
//                
//            case "LOAN_OFFICER":
//                LoanOfficer loanOfficer = loanOfficerRepository.findByEmail(request.getEmail())
//                        .orElseThrow(() -> new RuntimeException("User not found"));
//                loanOfficer.setPasswordHash(encodedPassword);
//                loanOfficerRepository.save(loanOfficer);
//                break;
//                
//            case "COMPLIANCE_OFFICER":
//                ComplianceOfficer complianceOfficer = complianceOfficerRepository.findByEmail(request.getEmail())
//                        .orElseThrow(() -> new RuntimeException("User not found"));
//                complianceOfficer.setPasswordHash(encodedPassword);
//                complianceOfficerRepository.save(complianceOfficer);
//                break;
//                
//            default:
//                throw new RuntimeException("Invalid user type");
//        }
//        
//        // Mark token as used
//        resetToken.setIsUsed(true);
//        resetToken.setUsedAt(LocalDateTime.now());
//        passwordResetTokenRepository.save(resetToken);
//        
//        // Send password change confirmation email
//        emailService.sendPasswordChangeConfirmationEmail(resetToken.getEmail());
//        
//        return "Password reset successfully. You can now login with your new password.";
//    }
//    
//    public String getEmailFromResetToken(String token) {
//        PasswordResetToken resetToken = passwordResetTokenRepository
//                .findByResetTokenAndIsUsedFalseAndExpiresAtAfter(token, LocalDateTime.now())
//                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));
//        
//        return resetToken.getEmail();
//    }
//}


package com.tss.springsecurity.service;

import com.tss.springsecurity.dto.CommonAuthResponse;
import com.tss.springsecurity.dto.CommonLoginRequest;
import com.tss.springsecurity.dto.ForgotPasswordRequest;
import com.tss.springsecurity.dto.ResetPasswordRequest;
import com.tss.springsecurity.entity.Admin;
import com.tss.springsecurity.entity.Applicant;
import com.tss.springsecurity.entity.LoanOfficer;
import com.tss.springsecurity.entity.ComplianceOfficer;
import com.tss.springsecurity.repository.AdminRepository;
import com.tss.springsecurity.repository.ApplicantRepository;
import com.tss.springsecurity.repository.LoanOfficerRepository;
import com.tss.springsecurity.repository.ComplianceOfficerRepository;
import com.tss.springsecurity.repository.PasswordResetTokenRepository;
import com.tss.springsecurity.entity.PasswordResetToken;
import com.tss.springsecurity.service.EmailService;
import com.tss.springsecurity.service.CaptchaService;
import com.tss.springsecurity.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommonAuthService {

    private final AdminRepository adminRepository;
    private final ApplicantRepository applicantRepository;
    private final LoanOfficerRepository loanOfficerRepository;
    private final ComplianceOfficerRepository complianceOfficerRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final CaptchaService captchaService;

    public CommonAuthResponse login(CommonLoginRequest request) {
        String usernameOrEmail = request.getUsername();
        String password = request.getPassword();
        
        // Verify CAPTCHA first
        if (!captchaService.verifyCaptcha(request.getCaptchaToken())) {
            throw new RuntimeException("CAPTCHA verification failed. Please try again.");
        }

        // Try Admin login first
        Optional<Admin> admin = adminRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);
        if (admin.isPresent() && passwordEncoder.matches(password, admin.get().getPassword())) {
            String token = jwtUtil.generateToken(admin.get().getUsername(), "ADMIN");
            return new CommonAuthResponse(
                admin.get().getAdminId(),
                null,
                admin.get().getUsername(),
                admin.get().getEmail(),
                "ADMIN",
                token
            );
        }

        // Try Applicant login
        Optional<Applicant> applicant = applicantRepository.findByEmail(usernameOrEmail);
        if (applicant.isPresent() && passwordEncoder.matches(password, applicant.get().getPassword())) {
            Applicant app = applicant.get();
            
            // Check if applicant is approved and email verified
            if (!app.getIsEmailVerified()) {
                throw new RuntimeException("Please verify your email before logging in");
            }
            if (!app.getIsApproved()) {
                throw new RuntimeException("Your account is pending admin approval");
            }
            
            String token = jwtUtil.generateToken(app.getEmail(), "APPLICANT");
            return new CommonAuthResponse(
                app.getApplicantId(),
                app.getApplicantId(),
                null, // Applicants don't have username
                app.getFirstName(),
                app.getLastName(),
                app.getEmail(),
                "APPLICANT",
                token
            );
        }

        // Try Loan Officer login
        Optional<LoanOfficer> loanOfficer = loanOfficerRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);
        if (loanOfficer.isPresent() && passwordEncoder.matches(password, loanOfficer.get().getPassword())) {
            String token = jwtUtil.generateToken(loanOfficer.get().getUsername(), "LOAN_OFFICER");
            return new CommonAuthResponse(
                loanOfficer.get().getOfficerId(), // userId
                loanOfficer.get().getOfficerId(), // officerId
                loanOfficer.get().getUsername(),
                loanOfficer.get().getEmail(),
                "LOAN_OFFICER",
                token
            );
        }

        // Try Compliance Officer login
        Optional<ComplianceOfficer> complianceOfficer = complianceOfficerRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);
        if (complianceOfficer.isPresent() && passwordEncoder.matches(password, complianceOfficer.get().getPassword())) {
            String token = jwtUtil.generateToken(complianceOfficer.get().getUsername(), "COMPLIANCE_OFFICER");
            return new CommonAuthResponse(
                complianceOfficer.get().getOfficerId(), // userId
                complianceOfficer.get().getOfficerId(), // officerId
                complianceOfficer.get().getUsername(),
                complianceOfficer.get().getEmail(),
                "COMPLIANCE_OFFICER",
                token
            );
        }

        // If no user found or password doesn't match
        throw new RuntimeException("Invalid username/email or password");
    }
    
    public String forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail();
        String userType = null;
        
        // Check which type of user exists with this email
        if (adminRepository.findByEmail(email).isPresent()) {
            userType = "ADMIN";
        } else if (applicantRepository.findByEmail(email).isPresent()) {
            userType = "APPLICANT";
        } else if (loanOfficerRepository.findByEmail(email).isPresent()) {
            userType = "LOAN_OFFICER";
        } else if (complianceOfficerRepository.findByEmail(email).isPresent()) {
            userType = "COMPLIANCE_OFFICER";
        }
        
        // Only send email if user exists
        if (userType != null) {
            // Generate reset token
            String resetToken = UUID.randomUUID().toString();
            
            // Save reset token
            PasswordResetToken passwordResetToken = new PasswordResetToken();
            passwordResetToken.setEmail(email);
            passwordResetToken.setUserType(userType);
            passwordResetToken.setResetToken(resetToken);
            passwordResetToken.setExpiresAt(LocalDateTime.now().plusHours(1));
            passwordResetTokenRepository.save(passwordResetToken);
            
            // Send reset email
            emailService.sendPasswordResetEmail(email, resetToken);
        }
        
        // Always return the same message regardless of whether email exists
        return "Reset password link has been shared to you in email.";
    }
    
    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        // Validate password confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("New password and confirm password do not match");
        }
        
        // Find valid reset token
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByResetTokenAndIsUsedFalseAndExpiresAtAfter(request.getResetToken(), LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));
        
        // Verify email matches
        if (!resetToken.getEmail().equals(request.getEmail())) {
            throw new RuntimeException("Invalid reset token");
        }
        
        // Update password based on user type
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        
        switch (resetToken.getUserType()) {
            case "ADMIN":
                Admin admin = adminRepository.findByEmail(request.getEmail())
                        .orElseThrow(() -> new RuntimeException("User not found"));
                admin.setPassword(encodedPassword);
                adminRepository.save(admin);
                break;
                
            case "APPLICANT":
                Applicant applicant = applicantRepository.findByEmail(request.getEmail())
                        .orElseThrow(() -> new RuntimeException("User not found"));
                applicant.setPassword(encodedPassword);
                applicantRepository.save(applicant);
                break;
                
            case "LOAN_OFFICER":
                LoanOfficer loanOfficer = loanOfficerRepository.findByEmail(request.getEmail())
                        .orElseThrow(() -> new RuntimeException("User not found"));
                loanOfficer.setPassword(encodedPassword);
                loanOfficerRepository.save(loanOfficer);
                break;
                
            case "COMPLIANCE_OFFICER":
                ComplianceOfficer complianceOfficer = complianceOfficerRepository.findByEmail(request.getEmail())
                        .orElseThrow(() -> new RuntimeException("User not found"));
                complianceOfficer.setPassword(encodedPassword);
                complianceOfficerRepository.save(complianceOfficer);
                break;
                
            default:
                throw new RuntimeException("Invalid user type");
        }
        
        // Mark token as used
        resetToken.setIsUsed(true);
        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);
        
        // Send password change confirmation email (non-blocking)
        try {
            emailService.sendPasswordChangeConfirmationEmail(resetToken.getEmail());
        } catch (Exception e) {
            // Log email error but don't fail the password reset
            log.error("Failed to send password change confirmation email, but password reset was successful", e);
        }
        
        return "Password reset successfully. You can now login with your new password.";
    }
    
    public String getEmailFromResetToken(String token) {
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByResetTokenAndIsUsedFalseAndExpiresAtAfter(token, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));
        
        return resetToken.getEmail();
    }
}

