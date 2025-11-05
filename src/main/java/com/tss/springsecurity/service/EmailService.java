package com.tss.springsecurity.service;

public interface EmailService {
    
    // Authentication & Account Management
    void sendOtpEmail(String to, String otp);
    void sendApprovalEmail(String to, String applicantName, boolean isApproved);
    void sendPasswordResetEmail(String to, String resetToken);
    void sendPasswordChangeConfirmationEmail(String to);
    void sendWelcomeEmail(String to, String applicantName);
    
    // Loan Application Process
    void sendLoanApplicationSubmittedEmail(String to, String applicantName, String loanType, String loanAmount, String loanId);
    void sendLoanApplicationStatusUpdateEmail(String to, String applicantName, String loanId, String status, String remarks);
    void sendLoanApprovedEmail(String to, String applicantName, String loanId, String loanType, String loanAmount, String interestRate, String tenure);
    void sendLoanRejectedEmail(String to, String applicantName, String loanId, String rejectionReason);
    void sendLoanDisbursedEmail(String to, String applicantName, String loanId, String disbursedAmount, String accountNumber);
    
    // Document Management
    void sendDocumentVerificationEmail(String to, String applicantName, String documentType, String status);
    void sendDocumentResubmissionRequestEmail(String to, String applicantName, String documentType, String reason, String instructions);
    
    // Officer Notifications
    void sendLoanAssignmentEmail(String to, String officerName, String loanId, String applicantName, String loanType);
    void sendComplianceEscalationEmail(String to, String officerName, String loanId, String applicantName, String escalationReason);
    void sendComplianceVerdictEmail(String to, String officerName, String loanId, String verdict, String remarks);
    
    // System Notifications
    void sendSystemMaintenanceEmail(String to, String userName, String maintenanceDate, String duration);
    void sendSecurityAlertEmail(String to, String userName, String alertType, String details);
}
