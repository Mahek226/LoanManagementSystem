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

    @Override
    public void sendWelcomeEmail(String to, String applicantName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Welcome to Loan Management System!");
            
            message.setText(
                "Dear " + applicantName + ",\n\n" +
                "Welcome to our Loan Management System!\n\n" +
                "Your account has been successfully created and verified. You can now:\n" +
                "- Apply for various types of loans\n" +
                "- Track your application status\n" +
                "- Upload required documents\n" +
                "- Communicate with our loan officers\n\n" +
                "To get started, please log in to your account at: http://localhost:4200/auth/login\n\n" +
                "If you have any questions, our support team is here to help.\n\n" +
                "Best regards,\n" +
                "Loan Management System Team"
            );
            
            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", to, e);
        }
    }

    @Override
    public void sendLoanApplicationSubmittedEmail(String to, String applicantName, String loanType, String loanAmount, String loanId) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Loan Application Submitted Successfully - LMS");
            
            message.setText(
                "Dear " + applicantName + ",\n\n" +
                "Your loan application has been successfully submitted!\n\n" +
                "Application Details:\n" +
                "- Loan ID: " + loanId + "\n" +
                "- Loan Type: " + loanType + "\n" +
                "- Loan Amount: ₹" + loanAmount + "\n" +
                "- Submission Date: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")) + "\n\n" +
                "What happens next?\n" +
                "1. Our team will review your application\n" +
                "2. You may be contacted for additional documents\n" +
                "3. A loan officer will be assigned to your case\n" +
                "4. You'll receive updates on your application status\n\n" +
                "You can track your application status by logging into your account.\n\n" +
                "Best regards,\n" +
                "Loan Management System Team"
            );
            
            mailSender.send(message);
            log.info("Loan application submitted email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send loan application submitted email to: {}", to, e);
        }
    }

    @Override
    public void sendLoanApplicationStatusUpdateEmail(String to, String applicantName, String loanId, String status, String remarks) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Loan Application Status Update - LMS");
            
            String statusMessage = getStatusMessage(status);
            
            message.setText(
                "Dear " + applicantName + ",\n\n" +
                "Your loan application status has been updated.\n\n" +
                "Application Details:\n" +
                "- Loan ID: " + loanId + "\n" +
                "- Current Status: " + status + "\n" +
                "- Update Date: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")) + "\n\n" +
                statusMessage + "\n\n" +
                (remarks != null && !remarks.isEmpty() ? "Additional Notes: " + remarks + "\n\n" : "") +
                "You can view detailed information by logging into your account.\n\n" +
                "Best regards,\n" +
                "Loan Management System Team"
            );
            
            mailSender.send(message);
            log.info("Loan status update email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send loan status update email to: {}", to, e);
        }
    }

    @Override
    public void sendLoanApprovedEmail(String to, String applicantName, String loanId, String loanType, String loanAmount, String interestRate, String tenure) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("🎉 Loan Application Approved! - LMS");
            
            message.setText(
                "Dear " + applicantName + ",\n\n" +
                "Congratulations! Your loan application has been APPROVED!\n\n" +
                "Approved Loan Details:\n" +
                "- Loan ID: " + loanId + "\n" +
                "- Loan Type: " + loanType + "\n" +
                "- Approved Amount: ₹" + loanAmount + "\n" +
                "- Interest Rate: " + interestRate + "% per annum\n" +
                "- Tenure: " + tenure + " months\n" +
                "- Approval Date: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")) + "\n\n" +
                "Next Steps:\n" +
                "1. Our team will contact you for loan agreement signing\n" +
                "2. Complete any remaining documentation\n" +
                "3. Loan disbursement will be processed\n" +
                "4. You'll receive disbursement confirmation\n\n" +
                "Please log in to your account to download your loan agreement.\n\n" +
                "Thank you for choosing our services!\n\n" +
                "Best regards,\n" +
                "Loan Management System Team"
            );
            
            mailSender.send(message);
            log.info("Loan approved email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send loan approved email to: {}", to, e);
        }
    }

    @Override
    public void sendLoanRejectedEmail(String to, String applicantName, String loanId, String rejectionReason) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Loan Application Status Update - LMS");
            
            message.setText(
                "Dear " + applicantName + ",\n\n" +
                "We regret to inform you that your loan application has been declined.\n\n" +
                "Application Details:\n" +
                "- Loan ID: " + loanId + "\n" +
                "- Status: REJECTED\n" +
                "- Decision Date: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")) + "\n\n" +
                (rejectionReason != null && !rejectionReason.isEmpty() ? 
                    "Reason for Decline:\n" + rejectionReason + "\n\n" : "") +
                "What you can do:\n" +
                "- Review the reason for decline\n" +
                "- Improve your financial profile\n" +
                "- Consider applying for a different loan type\n" +
                "- Contact our support team for guidance\n\n" +
                "You can download your rejection letter from your account.\n\n" +
                "We appreciate your interest in our services.\n\n" +
                "Best regards,\n" +
                "Loan Management System Team"
            );
            
            mailSender.send(message);
            log.info("Loan rejected email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send loan rejected email to: {}", to, e);
        }
    }

    @Override
    public void sendLoanDisbursedEmail(String to, String applicantName, String loanId, String disbursedAmount, String accountNumber) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("🎉 Loan Disbursed Successfully! - LMS");
            
            message.setText(
                "Dear " + applicantName + ",\n\n" +
                "Great news! Your loan has been successfully disbursed.\n\n" +
                "Disbursement Details:\n" +
                "- Loan ID: " + loanId + "\n" +
                "- Disbursed Amount: ₹" + disbursedAmount + "\n" +
                "- Account Number: " + accountNumber + "\n" +
                "- Disbursement Date: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")) + "\n\n" +
                "Important Information:\n" +
                "- Your first EMI will be due next month\n" +
                "- You'll receive EMI schedule details separately\n" +
                "- Set up auto-debit for hassle-free payments\n" +
                "- Keep all loan documents safe\n\n" +
                "You can view your loan details and EMI schedule in your account.\n\n" +
                "Thank you for choosing our services!\n\n" +
                "Best regards,\n" +
                "Loan Management System Team"
            );
            
            mailSender.send(message);
            log.info("Loan disbursed email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send loan disbursed email to: {}", to, e);
        }
    }

    @Override
    public void sendDocumentVerificationEmail(String to, String applicantName, String documentType, String status) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Document Verification Update - LMS");
            
            String statusMessage = status.equalsIgnoreCase("VERIFIED") ? 
                "has been successfully verified" : 
                "requires attention";
            
            message.setText(
                "Dear " + applicantName + ",\n\n" +
                "Your document verification status has been updated.\n\n" +
                "Document Details:\n" +
                "- Document Type: " + documentType + "\n" +
                "- Status: " + status + "\n" +
                "- Verification Date: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")) + "\n\n" +
                "Your " + documentType + " " + statusMessage + ".\n\n" +
                (status.equalsIgnoreCase("REJECTED") ? 
                    "Please log in to your account to see the reason and resubmit if necessary.\n\n" : 
                    "No action required from your side at this time.\n\n") +
                "You can view all document statuses in your account.\n\n" +
                "Best regards,\n" +
                "Loan Management System Team"
            );
            
            mailSender.send(message);
            log.info("Document verification email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send document verification email to: {}", to, e);
        }
    }

    @Override
    public void sendDocumentResubmissionRequestEmail(String to, String applicantName, String documentType, String reason, String instructions) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Document Resubmission Required - LMS");
            
            message.setText(
                "Dear " + applicantName + ",\n\n" +
                "We need you to resubmit a document for your loan application.\n\n" +
                "Document Details:\n" +
                "- Document Type: " + documentType + "\n" +
                "- Request Date: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy")) + "\n\n" +
                "Reason for Resubmission:\n" + reason + "\n\n" +
                (instructions != null && !instructions.isEmpty() ? 
                    "Instructions:\n" + instructions + "\n\n" : "") +
                "How to Resubmit:\n" +
                "1. Log in to your account\n" +
                "2. Go to the Documents section\n" +
                "3. Upload the corrected document\n" +
                "4. Wait for verification confirmation\n\n" +
                "Please resubmit the document as soon as possible to avoid delays in processing.\n\n" +
                "Best regards,\n" +
                "Loan Management System Team"
            );
            
            mailSender.send(message);
            log.info("Document resubmission request email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send document resubmission request email to: {}", to, e);
        }
    }

    @Override
    public void sendLoanAssignmentEmail(String to, String officerName, String loanId, String applicantName, String loanType) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("New Loan Assignment - LMS");
            
            message.setText(
                "Dear " + officerName + ",\n\n" +
                "A new loan application has been assigned to you for review.\n\n" +
                "Assignment Details:\n" +
                "- Loan ID: " + loanId + "\n" +
                "- Applicant Name: " + applicantName + "\n" +
                "- Loan Type: " + loanType + "\n" +
                "- Assignment Date: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")) + "\n\n" +
                "Please log in to the system to review the application and take appropriate action.\n\n" +
                "Best regards,\n" +
                "Loan Management System"
            );
            
            mailSender.send(message);
            log.info("Loan assignment email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send loan assignment email to: {}", to, e);
        }
    }

    @Override
    public void sendComplianceEscalationEmail(String to, String officerName, String loanId, String applicantName, String escalationReason) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Compliance Escalation - High Priority - LMS");
            
            message.setText(
                "Dear " + officerName + ",\n\n" +
                "A loan application has been escalated to compliance for review.\n\n" +
                "Escalation Details:\n" +
                "- Loan ID: " + loanId + "\n" +
                "- Applicant Name: " + applicantName + "\n" +
                "- Escalation Reason: " + escalationReason + "\n" +
                "- Escalation Date: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")) + "\n\n" +
                "This case requires immediate attention. Please log in to the system to review and provide your compliance verdict.\n\n" +
                "Best regards,\n" +
                "Loan Management System"
            );
            
            mailSender.send(message);
            log.info("Compliance escalation email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send compliance escalation email to: {}", to, e);
        }
    }

    @Override
    public void sendComplianceVerdictEmail(String to, String officerName, String loanId, String verdict, String remarks) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Compliance Verdict Available - LMS");
            
            message.setText(
                "Dear " + officerName + ",\n\n" +
                "A compliance verdict is now available for your review.\n\n" +
                "Verdict Details:\n" +
                "- Loan ID: " + loanId + "\n" +
                "- Compliance Verdict: " + verdict + "\n" +
                "- Verdict Date: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")) + "\n\n" +
                (remarks != null && !remarks.isEmpty() ? 
                    "Compliance Remarks:\n" + remarks + "\n\n" : "") +
                "Please log in to the system to review the verdict and make your final decision on the loan application.\n\n" +
                "Best regards,\n" +
                "Loan Management System"
            );
            
            mailSender.send(message);
            log.info("Compliance verdict email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send compliance verdict email to: {}", to, e);
        }
    }

    @Override
    public void sendLoanOfficerWelcomeEmail(String to, String officerName, String username, String password) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Welcome to Loan Management System - Loan Officer Account Created");
            
            message.setText(
                "Dear " + officerName + ",\n\n" +
                "Welcome to the Loan Management System! Your Loan Officer account has been successfully created.\n\n" +
                "Your Account Details:\n" +
                "- Username: " + username + "\n" +
                "- Email: " + to + "\n" +
                "- Temporary Password: " + password + "\n\n" +
                "🔒 IMPORTANT SECURITY NOTICE:\n" +
                "For your account security, please change your password immediately after your first login.\n\n" +
                "How to get started:\n" +
                "1. Visit: http://localhost:4200/auth/login\n" +
                "2. Login with your username and temporary password\n" +
                "3. Change your password in the profile section\n" +
                "4. Complete your profile information\n" +
                "5. Start reviewing assigned loan applications\n\n" +
                "Your Responsibilities:\n" +
                "- Review loan applications assigned to you\n" +
                "- Evaluate applicant creditworthiness\n" +
                "- Make approval/rejection decisions\n" +
                "- Escalate high-risk cases to compliance\n" +
                "- Maintain accurate records and documentation\n\n" +
                "If you have any questions or need assistance, please contact the system administrator.\n\n" +
                "Best regards,\n" +
                "Loan Management System Administration Team"
            );
            
            mailSender.send(message);
            log.info("Loan Officer welcome email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send Loan Officer welcome email to: {}", to, e);
            // Log to console as fallback
            log.info("=================================================");
            log.info("FALLBACK - Loan Officer Account Created for {}", to);
            log.info("Username: {}", username);
            log.info("Password: {}", password);
            log.info("Please change password after first login");
            log.info("=================================================");
        }
    }

    @Override
    public void sendComplianceOfficerWelcomeEmail(String to, String officerName, String username, String password) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Welcome to Loan Management System - Compliance Officer Account Created");
            
            message.setText(
                "Dear " + officerName + ",\n\n" +
                "Welcome to the Loan Management System! Your Compliance Officer account has been successfully created.\n\n" +
                "Your Account Details:\n" +
                "- Username: " + username + "\n" +
                "- Email: " + to + "\n" +
                "- Temporary Password: " + password + "\n\n" +
                "🔒 IMPORTANT SECURITY NOTICE:\n" +
                "For your account security, please change your password immediately after your first login.\n\n" +
                "How to get started:\n" +
                "1. Visit: http://localhost:4200/auth/login\n" +
                "2. Login with your username and temporary password\n" +
                "3. Change your password in the profile section\n" +
                "4. Complete your profile information\n" +
                "5. Start reviewing escalated compliance cases\n\n" +
                "Your Responsibilities:\n" +
                "- Review high-risk loan applications escalated by loan officers\n" +
                "- Conduct comprehensive compliance checks\n" +
                "- Analyze fraud indicators and external data\n" +
                "- Provide compliance verdicts and recommendations\n" +
                "- Ensure regulatory compliance and risk management\n" +
                "- Request document resubmissions when necessary\n\n" +
                "Access Level:\n" +
                "You have access to enhanced screening data, external fraud databases, and comprehensive review tools.\n\n" +
                "If you have any questions or need assistance, please contact the system administrator.\n\n" +
                "Best regards,\n" +
                "Loan Management System Administration Team"
            );
            
            mailSender.send(message);
            log.info("Compliance Officer welcome email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send Compliance Officer welcome email to: {}", to, e);
            // Log to console as fallback
            log.info("=================================================");
            log.info("FALLBACK - Compliance Officer Account Created for {}", to);
            log.info("Username: {}", username);
            log.info("Password: {}", password);
            log.info("Please change password after first login");
            log.info("=================================================");
        }
    }

    @Override
    public void sendSystemMaintenanceEmail(String to, String userName, String maintenanceDate, String duration) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Scheduled System Maintenance - LMS");
            
            message.setText(
                "Dear " + userName + ",\n\n" +
                "We would like to inform you about scheduled system maintenance.\n\n" +
                "Maintenance Details:\n" +
                "- Date: " + maintenanceDate + "\n" +
                "- Duration: " + duration + "\n" +
                "- Services Affected: All online services\n\n" +
                "During this time, you may experience:\n" +
                "- Temporary unavailability of online services\n" +
                "- Delays in processing requests\n" +
                "- Limited access to your account\n\n" +
                "We apologize for any inconvenience and appreciate your patience.\n\n" +
                "Best regards,\n" +
                "Loan Management System Team"
            );
            
            mailSender.send(message);
            log.info("System maintenance email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send system maintenance email to: {}", to, e);
        }
    }

    @Override
    public void sendSecurityAlertEmail(String to, String userName, String alertType, String details) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("🔒 Security Alert - LMS");
            
            message.setText(
                "Dear " + userName + ",\n\n" +
                "We detected unusual activity on your account.\n\n" +
                "Security Alert Details:\n" +
                "- Alert Type: " + alertType + "\n" +
                "- Date/Time: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")) + "\n" +
                "- Details: " + details + "\n\n" +
                "If this was you, no action is required.\n\n" +
                "If this was NOT you:\n" +
                "1. Change your password immediately\n" +
                "2. Review your account activity\n" +
                "3. Contact our support team\n" +
                "4. Enable two-factor authentication\n\n" +
                "Your account security is our priority.\n\n" +
                "Best regards,\n" +
                "Loan Management System Security Team"
            );
            
            mailSender.send(message);
            log.info("Security alert email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send security alert email to: {}", to, e);
        }
    }

    private String getStatusMessage(String status) {
        switch (status.toUpperCase()) {
            case "PENDING":
                return "Your application is currently under review by our team.";
            case "UNDER_REVIEW":
                return "Your application is being carefully evaluated by our loan officers.";
            case "APPROVED":
                return "Congratulations! Your loan application has been approved.";
            case "REJECTED":
                return "Unfortunately, your loan application has been declined.";
            case "ESCALATED_TO_COMPLIANCE":
                return "Your application has been forwarded to our compliance team for additional review.";
            case "DISBURSED":
                return "Your loan has been successfully disbursed to your account.";
            case "CLOSED":
                return "Your loan account has been closed.";
            default:
                return "Your application status has been updated.";
        }
    }
}
