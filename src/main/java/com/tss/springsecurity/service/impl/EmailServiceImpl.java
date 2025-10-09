package com.tss.springsecurity.service.impl;

import com.tss.springsecurity.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendOtpEmail(String to, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Email Verification OTP - Loan Management System");
            message.setText(
                "Dear User,\n\n" +
                "Your OTP for email verification is: " + otp + "\n\n" +
                "This OTP is valid for 10 minutes.\n\n" +
                "If you did not request this, please ignore this email.\n\n" +
                "Best regards,\n" +
                "Loan Management System Team"
            );
            
            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", to, e);
            // Log to console as fallback
            log.info("=================================================");
            log.info("FALLBACK - OTP for {}: {}", to, otp);
            log.info("=================================================");
            throw new RuntimeException("Failed to send OTP email. Please check email configuration.");
        }
    }

    @Override
    public void sendApprovalEmail(String to, String applicantName, boolean isApproved) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            
            if (isApproved) {
                message.setSubject("Application Approved - Loan Management System");
                message.setText(
                    "Dear " + applicantName + ",\n\n" +
                    "Congratulations! Your application has been approved by our admin team.\n\n" +
                    "You can now login to your account and proceed with your loan application.\n\n" +
                    "Best regards,\n" +
                    "Loan Management System Team"
                );
            } else {
                message.setSubject("Application Status Update - Loan Management System");
                message.setText(
                    "Dear " + applicantName + ",\n\n" +
                    "We regret to inform you that your application has been rejected.\n\n" +
                    "For more information, please contact our support team.\n\n" +
                    "Best regards,\n" +
                    "Loan Management System Team"
                );
            }
            
            mailSender.send(message);
            log.info("Approval email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send approval email to: {}", to, e);
            log.info("Approval status ({}) for {} not sent via email", isApproved ? "APPROVED" : "REJECTED", to);
        }
    }
    
    @Override
    public void sendPasswordResetEmail(String to, String resetToken) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Password Reset Request - Loan Management System");
            
            // Secure reset URL that doesn't expose sensitive information
            String resetUrl = "http://localhost:4200/auth/reset-password/" + resetToken;
            
            message.setText(
                "Dear User,\n\n" +
                "You have requested to reset your password for the Loan Management System.\n\n" +
                "Please click the following link to reset your password:\n" +
                resetUrl + "\n\n" +
                "This link is valid for 1 hour and can only be used once.\n\n" +
                "If you did not request this password reset, please ignore this email and your password will remain unchanged.\n\n" +
                "For security reasons, do not share this link with anyone.\n\n" +
                "Best regards,\n" +
                "Loan Management System Team"
            );
            
            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", to, e);
            // Log to console as fallback
            log.info("=================================================");
            log.info("FALLBACK - Password reset token for {}: {}", to, resetToken);
            log.info("Reset URL: http://localhost:4200/auth/reset-password/{}", resetToken);
            log.info("=================================================");
            throw new RuntimeException("Failed to send password reset email. Please check email configuration.");
        }
    }
    
    @Override
    public void sendPasswordChangeConfirmationEmail(String to) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Password Successfully Changed - Loan Management System");
            
            message.setText(
                "Dear User,\n\n" +
                "Your password has been successfully changed for your Loan Management System account.\n\n" +
                "If you did not make this change, please contact our support team immediately.\n\n" +
                "For your security:\n" +
                "- Never share your password with anyone\n" +
                "- Use a strong, unique password\n" +
                "- Log out from shared devices\n\n" +
                "Best regards,\n" +
                "Loan Management System Team"
            );
            
            mailSender.send(message);
            log.info("Password change confirmation email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send password change confirmation email to: {}", to, e);
            log.info("Password change confirmation for {} not sent via email", to);
        }
    }
}
