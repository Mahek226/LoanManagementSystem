package com.tss.springsecurity.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
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
    
    // Builder pattern for easy construction
    public static class Builder {
        private ActivityLog activityLog = new ActivityLog();
        
        public Builder performedBy(String performedBy) {
            activityLog.performedBy = performedBy;
            return this;
        }
        
        public Builder userRole(String userRole) {
            activityLog.userRole = userRole;
            return this;
        }
        
        public Builder activityType(String activityType) {
            activityLog.activityType = activityType;
            return this;
        }
        
        public Builder entityType(String entityType) {
            activityLog.entityType = entityType;
            return this;
        }
        
        public Builder entityId(Long entityId) {
            activityLog.entityId = entityId;
            return this;
        }
        
        public Builder description(String description) {
            activityLog.description = description;
            return this;
        }
        
        public Builder ipAddress(String ipAddress) {
            activityLog.ipAddress = ipAddress;
            return this;
        }
        
        public Builder userAgent(String userAgent) {
            activityLog.userAgent = userAgent;
            return this;
        }
        
        public Builder oldValue(String oldValue) {
            activityLog.oldValue = oldValue;
            return this;
        }
        
        public Builder newValue(String newValue) {
            activityLog.newValue = newValue;
            return this;
        }
        
        public Builder status(String status) {
            activityLog.status = status;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            activityLog.errorMessage = errorMessage;
            return this;
        }
        
        public ActivityLog build() {
            return activityLog;
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}
