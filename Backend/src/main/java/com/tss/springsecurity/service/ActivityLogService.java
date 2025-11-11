package com.tss.springsecurity.service;

import com.tss.springsecurity.entity.ActivityLog;
import com.tss.springsecurity.repository.ActivityLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ActivityLogService {
    
    private final ActivityLogRepository activityLogRepository;
    
    /**
     * Log an activity
     */
    @Transactional
    public ActivityLog logActivity(String performedBy, String userRole, String activityType, 
                                   String entityType, Long entityId, String description) {
        HttpServletRequest request = getCurrentRequest();
        
        ActivityLog log = ActivityLog.builder()
                .performedBy(performedBy)
                .userRole(userRole)
                .activityType(activityType)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .ipAddress(request != null ? getClientIP(request) : null)
                .userAgent(request != null ? request.getHeader("User-Agent") : null)
                .status("SUCCESS")
                .build();
        
        return activityLogRepository.save(log);
    }
    
    /**
     * Log an activity with old and new values
     */
    @Transactional
    public ActivityLog logActivityWithValues(String performedBy, String userRole, String activityType,
                                            String entityType, Long entityId, String description,
                                            String oldValue, String newValue) {
        HttpServletRequest request = getCurrentRequest();
        
        ActivityLog log = ActivityLog.builder()
                .performedBy(performedBy)
                .userRole(userRole)
                .activityType(activityType)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .oldValue(oldValue)
                .newValue(newValue)
                .ipAddress(request != null ? getClientIP(request) : null)
                .userAgent(request != null ? request.getHeader("User-Agent") : null)
                .status("SUCCESS")
                .build();
        
        return activityLogRepository.save(log);
    }
    
    /**
     * Log a failed activity
     */
    @Transactional
    public ActivityLog logFailedActivity(String performedBy, String userRole, String activityType,
                                        String entityType, Long entityId, String description,
                                        String errorMessage) {
        HttpServletRequest request = getCurrentRequest();
        
        ActivityLog log = ActivityLog.builder()
                .performedBy(performedBy)
                .userRole(userRole)
                .activityType(activityType)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .ipAddress(request != null ? getClientIP(request) : null)
                .userAgent(request != null ? request.getHeader("User-Agent") : null)
                .status("FAILED")
                .errorMessage(errorMessage)
                .build();
        
        return activityLogRepository.save(log);
    }
    
    /**
     * Get all activities with pagination
     */
    @Transactional(readOnly = true)
    public Page<ActivityLog> getAllActivities(Pageable pageable) {
        return activityLogRepository.findAllByOrderByTimestampDesc(pageable);
    }
    
    /**
     * Get activities by user
     */
    @Transactional(readOnly = true)
    public Page<ActivityLog> getActivitiesByUser(String username, Pageable pageable) {
        return activityLogRepository.findByPerformedByOrderByTimestampDesc(username, pageable);
    }
    
    /**
     * Get activities by type
     */
    @Transactional(readOnly = true)
    public Page<ActivityLog> getActivitiesByType(String activityType, Pageable pageable) {
        return activityLogRepository.findByActivityTypeOrderByTimestampDesc(activityType, pageable);
    }
    
    /**
     * Get activities by entity
     */
    @Transactional(readOnly = true)
    public Page<ActivityLog> getActivitiesByEntity(String entityType, Long entityId, Pageable pageable) {
        return activityLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId, pageable);
    }
    
    /**
     * Get activities by date range
     */
    @Transactional(readOnly = true)
    public Page<ActivityLog> getActivitiesByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return activityLogRepository.findByDateRange(startDate, endDate, pageable);
    }
    
    /**
     * Search activities
     */
    @Transactional(readOnly = true)
    public Page<ActivityLog> searchActivities(String query, Pageable pageable) {
        return activityLogRepository.searchActivities(query, pageable);
    }
    
    /**
     * Get recent activities
     */
    @Transactional(readOnly = true)
    public List<ActivityLog> getRecentActivities() {
        return activityLogRepository.findTop10ByOrderByTimestampDesc();
    }
    
    /**
     * Get activity statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getActivityStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalActivities", activityLogRepository.count());
        stats.put("successfulActivities", activityLogRepository.countByStatus("SUCCESS"));
        stats.put("failedActivities", activityLogRepository.countByStatus("FAILED"));
        
        // Count by activity type
        stats.put("loginCount", activityLogRepository.countByActivityType("LOGIN"));
        stats.put("createCount", activityLogRepository.countByActivityType("CREATE"));
        stats.put("updateCount", activityLogRepository.countByActivityType("UPDATE"));
        stats.put("deleteCount", activityLogRepository.countByActivityType("DELETE"));
        stats.put("approveCount", activityLogRepository.countByActivityType("APPROVE"));
        stats.put("rejectCount", activityLogRepository.countByActivityType("REJECT"));
        
        return stats;
    }
    
    /**
     * Get current HTTP request
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
    
    /**
     * Get client IP address from request
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
