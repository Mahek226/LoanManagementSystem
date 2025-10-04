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
}
