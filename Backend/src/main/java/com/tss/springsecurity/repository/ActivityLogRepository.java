package com.tss.springsecurity.repository;

import com.tss.springsecurity.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    
    // Find by user
    Page<ActivityLog> findByPerformedByOrderByTimestampDesc(String performedBy, Pageable pageable);
    
    // Find by activity type
    Page<ActivityLog> findByActivityTypeOrderByTimestampDesc(String activityType, Pageable pageable);
    
    // Find by entity
    Page<ActivityLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, Long entityId, Pageable pageable);
    
    // Find all ordered by timestamp
    Page<ActivityLog> findAllByOrderByTimestampDesc(Pageable pageable);
    
    // Find by date range
    @Query("SELECT a FROM ActivityLog a WHERE a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    Page<ActivityLog> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate, 
                                      Pageable pageable);
    
    // Count by activity type
    long countByActivityType(String activityType);
    
    // Count by user
    long countByPerformedBy(String performedBy);
    
    // Count by status
    long countByStatus(String status);
    
    // Get recent activities
    List<ActivityLog> findTop10ByOrderByTimestampDesc();
    
    // Get activities by user and date range
    @Query("SELECT a FROM ActivityLog a WHERE a.performedBy = :performedBy AND a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    List<ActivityLog> findByUserAndDateRange(@Param("performedBy") String performedBy,
                                             @Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);
    
    // Search activities
    @Query("SELECT a FROM ActivityLog a WHERE " +
           "LOWER(a.performedBy) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.activityType) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.entityType) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.description) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY a.timestamp DESC")
    Page<ActivityLog> searchActivities(@Param("query") String query, Pageable pageable);
}
