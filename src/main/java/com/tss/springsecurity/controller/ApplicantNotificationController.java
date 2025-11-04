package com.tss.springsecurity.controller;

import com.tss.springsecurity.entity.ApplicantNotification;
import com.tss.springsecurity.entity.ApplicantNotification.*;
import com.tss.springsecurity.service.ApplicantNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/applicant")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"}, allowCredentials = "true")
public class ApplicantNotificationController {
    
    private final ApplicantNotificationService notificationService;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    
    /**
     * Get all notifications for an applicant
     */
    @GetMapping("/{applicantId}/notifications")
    public ResponseEntity<?> getNotifications(@PathVariable Long applicantId) {
        try {
            log.info("Fetching notifications for applicant: {}", applicantId);
            
            List<ApplicantNotification> notifications = 
                notificationService.getNotificationsForApplicant(applicantId);
            
            List<Map<String, Object>> response = notifications.stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching notifications: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch notifications: " + e.getMessage()));
        }
    }
    
    /**
     * Get unread notifications count
     */
    @GetMapping("/{applicantId}/notifications/unread-count")
    public ResponseEntity<?> getUnreadCount(@PathVariable Long applicantId) {
        try {
            long count = notificationService.getUnreadCount(applicantId);
            return ResponseEntity.ok(Map.of("unreadCount", count));
            
        } catch (Exception e) {
            log.error("Error fetching unread count: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch unread count: " + e.getMessage()));
        }
    }
    
    /**
     * Get filtered notifications
     */
    @GetMapping("/{applicantId}/notifications/filter")
    public ResponseEntity<?> getFilteredNotifications(
            @PathVariable Long applicantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String priority) {
        try {
            NotificationStatus statusEnum = status != null ? NotificationStatus.valueOf(status) : null;
            NotificationType typeEnum = type != null ? NotificationType.valueOf(type) : null;
            NotificationPriority priorityEnum = priority != null ? NotificationPriority.valueOf(priority) : null;
            
            List<ApplicantNotification> notifications = 
                notificationService.getFilteredNotifications(applicantId, statusEnum, typeEnum, priorityEnum);
            
            List<Map<String, Object>> response = notifications.stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching filtered notifications: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch filtered notifications: " + e.getMessage()));
        }
    }
    
    /**
     * Get overdue notifications
     */
    @GetMapping("/{applicantId}/notifications/overdue")
    public ResponseEntity<?> getOverdueNotifications(@PathVariable Long applicantId) {
        try {
            List<ApplicantNotification> notifications = 
                notificationService.getOverdueNotifications(applicantId);
            
            List<Map<String, Object>> response = notifications.stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error fetching overdue notifications: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch overdue notifications: " + e.getMessage()));
        }
    }
    
    /**
     * Mark notification as read
     */
    @PutMapping("/notification/{notificationId}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long notificationId) {
        try {
            ApplicantNotification notification = notificationService.markAsRead(notificationId);
            return ResponseEntity.ok(convertToMap(notification));
            
        } catch (Exception e) {
            log.error("Error marking notification as read: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Failed to mark notification as read: " + e.getMessage()));
        }
    }
    
    /**
     * Mark notification as resolved
     */
    @PutMapping("/notification/{notificationId}/resolve")
    public ResponseEntity<?> markAsResolved(@PathVariable Long notificationId) {
        try {
            ApplicantNotification notification = notificationService.markAsResolved(notificationId);
            return ResponseEntity.ok(convertToMap(notification));
            
        } catch (Exception e) {
            log.error("Error marking notification as resolved: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Failed to mark notification as resolved: " + e.getMessage()));
        }
    }
    
    /**
     * Delete notification
     */
    @DeleteMapping("/notification/{notificationId}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long notificationId) {
        try {
            notificationService.deleteNotification(notificationId);
            return ResponseEntity.ok(Map.of("message", "Notification deleted successfully"));
            
        } catch (Exception e) {
            log.error("Error deleting notification: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Failed to delete notification: " + e.getMessage()));
        }
    }
    
    /**
     * Helper method to convert entity to map
     */
    private Map<String, Object> convertToMap(ApplicantNotification notification) {
        Map<String, Object> map = new HashMap<>();
        map.put("notificationId", notification.getNotificationId());
        map.put("loanId", notification.getLoan() != null ? notification.getLoan().getLoanId() : null);
        map.put("assignmentId", notification.getAssignmentId());
        map.put("title", notification.getTitle());
        map.put("message", notification.getMessage());
        map.put("type", notification.getType().name());
        map.put("priority", notification.getPriority().name());
        map.put("status", notification.getStatus().name());
        map.put("requestedBy", notification.getRequestedBy());
        map.put("requestedAt", notification.getRequestedAt() != null ? notification.getRequestedAt().format(formatter) : null);
        map.put("dueDate", notification.getDueDate() != null ? notification.getDueDate().format(formatter) : null);
        map.put("readAt", notification.getReadAt() != null ? notification.getReadAt().format(formatter) : null);
        map.put("resolvedAt", notification.getResolvedAt() != null ? notification.getResolvedAt().format(formatter) : null);
        
        // Parse JSON arrays for requested documents and info
        if (notification.getRequestedDocuments() != null) {
            map.put("requestedDocuments", parseJsonArray(notification.getRequestedDocuments()));
        }
        if (notification.getRequestedInfo() != null) {
            map.put("requestedInfo", parseJsonArray(notification.getRequestedInfo()));
        }
        
        return map;
    }
    
    /**
     * Parse JSON array string to list
     */
    private List<String> parseJsonArray(String jsonArray) {
        try {
            // Simple parsing for JSON arrays
            String cleaned = jsonArray.replace("[", "").replace("]", "").replace("\"", "");
            return List.of(cleaned.split(","));
        } catch (Exception e) {
            return List.of();
        }
    }
}
