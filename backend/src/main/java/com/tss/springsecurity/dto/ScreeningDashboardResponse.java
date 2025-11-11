package com.tss.springsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScreeningDashboardResponse {
    
    // Officer Information
    private Long officerId;
    private String officerName;
    private String loanType;
    
    // Dashboard Statistics
    private Integer totalAssignedLoans;
    private Integer pendingScreenings;
    private Integer completedToday;
    private Integer escalatedLoans;
    
    // Performance Metrics
    private Double averageProcessingTime; // in hours
    private Integer approvalRate; // percentage
    private Integer rejectionRate; // percentage
    
    // Recent Activity
    private List<LoanScreeningResponse> recentAssignments;
    private List<LoanScreeningResponse> urgentLoans;
    
    // Workload Information
    private Integer dailyTarget;
    private Integer completedTarget;
    private String workloadStatus; // LIGHT, NORMAL, HEAVY, OVERLOADED
    
    // Alerts and Notifications
    private List<String> alerts;
    private Integer unreadNotifications;
    
    // Time Information
    private LocalDateTime lastLoginTime;
    private LocalDateTime dashboardRefreshTime;
    
    public ScreeningDashboardResponse(Long officerId, String officerName, String loanType) {
        this.officerId = officerId;
        this.officerName = officerName;
        this.loanType = loanType;
        this.dashboardRefreshTime = LocalDateTime.now();
    }
}
