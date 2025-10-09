package com.tss.springsecurity.service;

public interface EmailService {
    
    void sendOtpEmail(String to, String otp);
    
    void sendApprovalEmail(String to, String applicantName, boolean isApproved);
    
    void sendPasswordResetEmail(String to, String resetToken);
    
    void sendPasswordChangeConfirmationEmail(String to);
}
