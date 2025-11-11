package com.tss.springsecurity.repository;

import com.tss.springsecurity.entity.ApplicantNotification;
import com.tss.springsecurity.entity.ApplicantNotification.NotificationStatus;
import com.tss.springsecurity.entity.ApplicantNotification.NotificationType;
import com.tss.springsecurity.entity.ApplicantNotification.NotificationPriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ApplicantNotificationRepository extends JpaRepository<ApplicantNotification, Long> {
    
    // Find all notifications for an applicant
    List<ApplicantNotification> findByApplicant_ApplicantIdOrderByRequestedAtDesc(Long applicantId);
    
    // Find notifications by applicant with pagination
    Page<ApplicantNotification> findByApplicant_ApplicantIdOrderByRequestedAtDesc(Long applicantId, Pageable pageable);
    
    // Find unread notifications for an applicant
    List<ApplicantNotification> findByApplicant_ApplicantIdAndStatusOrderByRequestedAtDesc(
        Long applicantId, NotificationStatus status);
    
    // Find notifications by type
    List<ApplicantNotification> findByApplicant_ApplicantIdAndTypeOrderByRequestedAtDesc(
        Long applicantId, NotificationType type);
    
    // Find notifications by priority
    List<ApplicantNotification> findByApplicant_ApplicantIdAndPriorityOrderByRequestedAtDesc(
        Long applicantId, NotificationPriority priority);
    
    // Find notifications for a specific loan
    List<ApplicantNotification> findByLoan_LoanIdOrderByRequestedAtDesc(Long loanId);
    
    // Count unread notifications
    long countByApplicant_ApplicantIdAndStatus(Long applicantId, NotificationStatus status);
    
    // Find overdue notifications
    @Query("SELECT n FROM ApplicantNotification n WHERE n.applicant.applicantId = :applicantId " +
           "AND n.status != 'RESOLVED' AND n.dueDate < :currentDate")
    List<ApplicantNotification> findOverdueNotifications(
        @Param("applicantId") Long applicantId, 
        @Param("currentDate") LocalDateTime currentDate);
    
    // Complex filter query
    @Query("SELECT n FROM ApplicantNotification n WHERE n.applicant.applicantId = :applicantId " +
           "AND (:status IS NULL OR n.status = :status) " +
           "AND (:type IS NULL OR n.type = :type) " +
           "AND (:priority IS NULL OR n.priority = :priority) " +
           "ORDER BY n.requestedAt DESC")
    List<ApplicantNotification> findWithFilters(
        @Param("applicantId") Long applicantId,
        @Param("status") NotificationStatus status,
        @Param("type") NotificationType type,
        @Param("priority") NotificationPriority priority);
}
