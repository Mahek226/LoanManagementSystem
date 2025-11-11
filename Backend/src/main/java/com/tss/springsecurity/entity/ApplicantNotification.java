package com.tss.springsecurity.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "applicant_notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicantNotification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private Applicant applicant;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id")
    private ApplicantLoanDetails loan;
    
    @Column(name = "assignment_id")
    private Long assignmentId;
    
    @Column(name = "title", nullable = false, length = 255)
    private String title;
    
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private NotificationPriority priority;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status;
    
    @Column(name = "requested_by", length = 255)
    private String requestedBy;
    
    @Column(name = "requested_at")
    private LocalDateTime requestedAt;
    
    @Column(name = "due_date")
    private LocalDateTime dueDate;
    
    @Column(name = "read_at")
    private LocalDateTime readAt;
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    @Column(name = "requested_documents", columnDefinition = "TEXT")
    private String requestedDocuments; // JSON array stored as string
    
    @Column(name = "requested_info", columnDefinition = "TEXT")
    private String requestedInfo; // JSON array stored as string
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (requestedAt == null) {
            requestedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = NotificationStatus.UNREAD;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Enums
    public enum NotificationType {
        INFO_REQUEST,
        DOCUMENT_REQUEST,
        STATUS_UPDATE,
        APPROVAL,
        REJECTION
    }
    
    public enum NotificationPriority {
        HIGH,
        MEDIUM,
        LOW
    }
    
    public enum NotificationStatus {
        UNREAD,
        READ,
        RESOLVED
    }
}
