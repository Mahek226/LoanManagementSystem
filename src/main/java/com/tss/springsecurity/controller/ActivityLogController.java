package com.tss.springsecurity.controller;

import com.tss.springsecurity.entity.ActivityLog;
import com.tss.springsecurity.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/activity-logs")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"}, allowCredentials = "true")
@RequiredArgsConstructor
public class ActivityLogController {
    
    private final ActivityLogService activityLogService;
    
    /**
     * Get all activity logs with pagination
     */
    @GetMapping
    public ResponseEntity<Page<ActivityLog>> getAllActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ActivityLog> activities = activityLogService.getAllActivities(pageable);
        return ResponseEntity.ok(activities);
    }
    
    /**
     * Get recent activities (last 10)
     */
    @GetMapping("/recent")
    public ResponseEntity<List<ActivityLog>> getRecentActivities() {
        List<ActivityLog> activities = activityLogService.getRecentActivities();
        return ResponseEntity.ok(activities);
    }
    
    /**
     * Get activities by user
     */
    @GetMapping("/user/{username}")
    public ResponseEntity<Page<ActivityLog>> getActivitiesByUser(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ActivityLog> activities = activityLogService.getActivitiesByUser(username, pageable);
        return ResponseEntity.ok(activities);
    }
    
    /**
     * Get activities by type
     */
    @GetMapping("/type/{activityType}")
    public ResponseEntity<Page<ActivityLog>> getActivitiesByType(
            @PathVariable String activityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ActivityLog> activities = activityLogService.getActivitiesByType(activityType, pageable);
        return ResponseEntity.ok(activities);
    }
    
    /**
     * Get activities by entity
     */
    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<Page<ActivityLog>> getActivitiesByEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ActivityLog> activities = activityLogService.getActivitiesByEntity(entityType, entityId, pageable);
        return ResponseEntity.ok(activities);
    }
    
    /**
     * Get activities by date range
     */
    @GetMapping("/date-range")
    public ResponseEntity<Page<ActivityLog>> getActivitiesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ActivityLog> activities = activityLogService.getActivitiesByDateRange(startDate, endDate, pageable);
        return ResponseEntity.ok(activities);
    }
    
    /**
     * Search activities
     */
    @GetMapping("/search")
    public ResponseEntity<Page<ActivityLog>> searchActivities(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ActivityLog> activities = activityLogService.searchActivities(query, pageable);
        return ResponseEntity.ok(activities);
    }
    
    /**
     * Get activity statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getActivityStatistics() {
        Map<String, Object> stats = activityLogService.getActivityStatistics();
        return ResponseEntity.ok(stats);
    }
}
