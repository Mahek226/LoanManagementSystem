package com.tss.springsecurity.service;

import com.tss.springsecurity.entity.Applicant;
import com.tss.springsecurity.entity.ApplicantLoanDetails;
import com.tss.springsecurity.entity.ApplicantNotification;
import com.tss.springsecurity.entity.ApplicantNotification.*;
import com.tss.springsecurity.repository.ApplicantNotificationRepository;
import com.tss.springsecurity.repository.ApplicantRepository;
import com.tss.springsecurity.repository.ApplicantLoanDetailsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicantNotificationService {
    
    private final ApplicantNotificationRepository notificationRepository;
    private final ApplicantRepository applicantRepository;
    private final ApplicantLoanDetailsRepository loanRepository;
    
    /**
     * Get all notifications for an applicant
     */
    public List<ApplicantNotification> getNotificationsForApplicant(Long applicantId) {
        log.info("Fetching notifications for applicant: {}", applicantId);
        return notificationRepository.findByApplicant_ApplicantIdOrderByRequestedAtDesc(applicantId);
    }
    
    /**
     * Get unread notifications count
     */
    public long getUnreadCount(Long applicantId) {
        return notificationRepository.countByApplicant_ApplicantIdAndStatus(
            applicantId, NotificationStatus.UNREAD);
    }
    
    /**
     * Get filtered notifications
     */
    public List<ApplicantNotification> getFilteredNotifications(
            Long applicantId,
            NotificationStatus status,
            NotificationType type,
            NotificationPriority priority) {
        return notificationRepository.findWithFilters(applicantId, status, type, priority);
    }
    
    /**
     * Get overdue notifications
     */
    public List<ApplicantNotification> getOverdueNotifications(Long applicantId) {
        return notificationRepository.findOverdueNotifications(applicantId, LocalDateTime.now());
    }
    
    /**
     * Create a new notification
     */
    @Transactional
    public ApplicantNotification createNotification(
            Long applicantId,
            Long loanId,
            Long assignmentId,
            String title,
            String message,
            NotificationType type,
            NotificationPriority priority,
            String requestedBy,
            LocalDateTime dueDate,
            String requestedDocuments,
            String requestedInfo) {
        
        log.info("Creating notification for applicant: {}, loan: {}", applicantId, loanId);
        
        Applicant applicant = applicantRepository.findById(applicantId)
            .orElseThrow(() -> new RuntimeException("Applicant not found with ID: " + applicantId));
        
        ApplicantLoanDetails loan = null;
        if (loanId != null) {
            loan = loanRepository.findById(loanId).orElse(null);
        }
        
        ApplicantNotification notification = new ApplicantNotification();
        notification.setApplicant(applicant);
        notification.setLoan(loan);
        notification.setAssignmentId(assignmentId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setPriority(priority);
        notification.setStatus(NotificationStatus.UNREAD);
        notification.setRequestedBy(requestedBy);
        notification.setRequestedAt(LocalDateTime.now());
        notification.setDueDate(dueDate);
        notification.setRequestedDocuments(requestedDocuments);
        notification.setRequestedInfo(requestedInfo);
        
        return notificationRepository.save(notification);
    }
    
    /**
     * Mark notification as read
     */
    @Transactional
    public ApplicantNotification markAsRead(Long notificationId) {
        log.info("Marking notification as read: {}", notificationId);
        
        ApplicantNotification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Notification not found with ID: " + notificationId));
        
        if (notification.getStatus() == NotificationStatus.UNREAD) {
            notification.setStatus(NotificationStatus.READ);
            notification.setReadAt(LocalDateTime.now());
            return notificationRepository.save(notification);
        }
        
        return notification;
    }
    
    /**
     * Mark notification as resolved
     */
    @Transactional
    public ApplicantNotification markAsResolved(Long notificationId) {
        log.info("Marking notification as resolved: {}", notificationId);
        
        ApplicantNotification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Notification not found with ID: " + notificationId));
        
        notification.setStatus(NotificationStatus.RESOLVED);
        notification.setResolvedAt(LocalDateTime.now());
        
        return notificationRepository.save(notification);
    }
    
    /**
     * Delete notification
     */
    @Transactional
    public void deleteNotification(Long notificationId) {
        log.info("Deleting notification: {}", notificationId);
        notificationRepository.deleteById(notificationId);
    }
    
    /**
     * Create document request notification
     */
    @Transactional
    public ApplicantNotification createDocumentRequest(
            Long applicantId,
            Long loanId,
            Long assignmentId,
            List<String> documentTypes,
            String reason,
            String requestedBy) {
        
        String title = "Documents Required for Loan Application";
        String message = "The loan officer has requested additional documents for your loan application. " +
                        "Reason: " + reason;
        
        // Convert list to JSON array string
        String documentsJson = "[\"" + String.join("\",\"", documentTypes) + "\"]";
        
        LocalDateTime dueDate = LocalDateTime.now().plusDays(7); // 7 days to submit
        
        return createNotification(
            applicantId,
            loanId,
            assignmentId,
            title,
            message,
            NotificationType.DOCUMENT_REQUEST,
            NotificationPriority.HIGH,
            requestedBy,
            dueDate,
            documentsJson,
            null
        );
    }
    
    /**
     * Create document resubmission request notification
     */
    @Transactional
    public ApplicantNotification createDocumentResubmissionRequest(
            Long applicantId,
            Long loanId,
            Long assignmentId,
            List<String> documentTypes,
            String reason,
            String additionalComments,
            String requestedBy) {
        
        String title = "Document Resubmission Required";
        String message = "The loan officer has requested that you resubmit certain documents for your loan application. " +
                        "Reason: " + reason;
        
        if (additionalComments != null && !additionalComments.isEmpty()) {
            message += " Additional notes: " + additionalComments;
        }
        
        // Convert list to JSON array string
        String documentsJson = "[\"" + String.join("\",\"", documentTypes) + "\"]";
        
        LocalDateTime dueDate = LocalDateTime.now().plusDays(7); // 7 days to resubmit
        
        return createNotification(
            applicantId,
            loanId,
            assignmentId,
            title,
            message,
            NotificationType.DOCUMENT_REQUEST,
            NotificationPriority.HIGH,
            requestedBy,
            dueDate,
            documentsJson,
            null
        );
    }
    
    /**
     * Create info request notification
     */
    @Transactional
    public ApplicantNotification createInfoRequest(
            Long applicantId,
            Long loanId,
            Long assignmentId,
            List<String> infoItems,
            String reason,
            String requestedBy) {
        
        String title = "Additional Information Required";
        String message = "The loan officer has requested additional information for your loan application. " +
                        "Reason: " + reason;
        
        // Convert list to JSON array string
        String infoJson = "[\"" + String.join("\",\"", infoItems) + "\"]";
        
        LocalDateTime dueDate = LocalDateTime.now().plusDays(5); // 5 days to respond
        
        return createNotification(
            applicantId,
            loanId,
            assignmentId,
            title,
            message,
            NotificationType.INFO_REQUEST,
            NotificationPriority.HIGH,
            requestedBy,
            dueDate,
            null,
            infoJson
        );
    }
    
    /**
     * Create status update notification
     */
    @Transactional
    public ApplicantNotification createStatusUpdate(
            Long applicantId,
            Long loanId,
            Long assignmentId,
            String oldStatus,
            String newStatus,
            String remarks) {
        
        String title = "Loan Application Status Updated";
        String message = String.format("Your loan application status has been updated from '%s' to '%s'. %s",
                                      oldStatus, newStatus, remarks != null ? "Remarks: " + remarks : "");
        
        NotificationPriority priority = newStatus.equals("APPROVED") || newStatus.equals("REJECTED") 
                                       ? NotificationPriority.HIGH 
                                       : NotificationPriority.MEDIUM;
        
        return createNotification(
            applicantId,
            loanId,
            assignmentId,
            title,
            message,
            NotificationType.STATUS_UPDATE,
            priority,
            "System",
            null,
            null,
            null
        );
    }
    
    /**
     * Create approval notification
     */
    @Transactional
    public ApplicantNotification createApprovalNotification(
            Long applicantId,
            Long loanId,
            Long assignmentId,
            String approvedBy,
            String remarks) {
        
        String title = "Loan Application Approved!";
        String message = "Congratulations! Your loan application has been approved. " +
                        (remarks != null ? "Remarks: " + remarks : "") +
                        " Please check your email for further instructions.";
        
        return createNotification(
            applicantId,
            loanId,
            assignmentId,
            title,
            message,
            NotificationType.APPROVAL,
            NotificationPriority.HIGH,
            approvedBy,
            null,
            null,
            null
        );
    }
    
    /**
     * Create rejection notification
     */
    @Transactional
    public ApplicantNotification createRejectionNotification(
            Long applicantId,
            Long loanId,
            Long assignmentId,
            String rejectedBy,
            String reason) {
        
        String title = "Loan Application Rejected";
        String message = "Unfortunately, your loan application has been rejected. " +
                        "Reason: " + reason +
                        " You may reapply after addressing the concerns mentioned.";
        
        return createNotification(
            applicantId,
            loanId,
            assignmentId,
            title,
            message,
            NotificationType.REJECTION,
            NotificationPriority.HIGH,
            rejectedBy,
            null,
            null,
            null
        );
    }
}
