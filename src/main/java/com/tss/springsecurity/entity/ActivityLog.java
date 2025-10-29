package com.tss.springsecurity.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_log", indexes = {
    @Index(name = "idx_activity_user", columnList = "performed_by"),
    @Index(name = "idx_activity_type", columnList = "activity_type"),
    @Index(name = "idx_activity_timestamp", columnList = "timestamp"),
    @Index(name = "idx_activity_entity", columnList = "entity_type,entity_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;
    
    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy; // Username or Admin ID
    
    @Column(name = "user_role", length = 50)
    private String userRole; // ADMIN, LOAN_OFFICER, COMPLIANCE_OFFICER
    
    @Column(name = "activity_type", nullable = false, length = 100)
    private String activityType; // LOGIN, LOGOUT, CREATE, UPDATE, DELETE, APPROVE, REJECT, etc.
    
    @Column(name = "entity_type", length = 100)
    private String entityType; // APPLICANT, LOAN_OFFICER, FRAUD_RULE, etc.
    
    @Column(name = "entity_id")
    private Long entityId; // ID of the affected entity
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // Detailed description of the activity
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue; // For update operations
    
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue; // For update operations
    
    @Column(name = "status", length = 20)
    private String status; // SUCCESS, FAILED
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage; // If status is FAILED
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
        if (status == null) {
            status = "SUCCESS";
        }
    }
}
