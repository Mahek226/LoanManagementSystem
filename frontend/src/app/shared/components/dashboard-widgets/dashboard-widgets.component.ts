import { Component, Input, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription, interval } from 'rxjs';
import { Chart, ChartConfiguration, ChartType, registerables } from 'chart.js';
import { AdminService } from '../../../core/services/admin.service';

Chart.register(...registerables);

export interface WidgetConfig {
  id: string;
  title: string;
  type: 'stat' | 'chart' | 'list' | 'progress' | 'metric' | 'activity';
  size: 'small' | 'medium' | 'large' | 'full';
  refreshInterval?: number; // in seconds
  chartType?: ChartType;
  dataSource?: string;
  icon?: string;
  color?: string;
  position?: { row: number; col: number };
  visible?: boolean;
  interactive?: boolean;
}

export interface WidgetData {
  value?: number | string;
  label?: string;
  trend?: number; // percentage change
  trendDirection?: 'up' | 'down' | 'stable';
  chartData?: any;
  listItems?: any[];
  progress?: number;
  status?: 'success' | 'warning' | 'danger' | 'info';
  lastUpdated?: Date;
  metadata?: { [key: string]: any };
}

@Component({
  selector: 'app-dashboard-widgets',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard-widgets.component.html',
  styleUrls: ['./dashboard-widgets.component.css']
})
export class DashboardWidgetsComponent implements OnInit, OnDestroy, AfterViewInit {
  @Input() widgets: WidgetConfig[] = [];
  @Input() autoRefresh = true;
  @Input() gridColumns = 4;
  @Input() userRole = '';

  widgetData: { [key: string]: WidgetData } = {};
  charts: { [key: string]: Chart } = {};
  isLoading: { [key: string]: boolean } = {};
  errors: { [key: string]: string } = {};
  
  private subscriptions: Subscription[] = [];

  // Default widget configurations for different roles
  private defaultWidgets: { [role: string]: WidgetConfig[] } = {
    'ADMIN': [
      {
        id: 'total_applicants',
        title: 'Total Applicants',
        type: 'stat',
        size: 'small',
        icon: 'fas fa-users',
        color: '#3b82f6',
        dataSource: 'dashboard_stats',
        refreshInterval: 30
      },
      {
        id: 'pending_applications',
        title: 'Pending Applications',
        type: 'stat',
        size: 'small',
        icon: 'fas fa-clock',
        color: '#f59e0b',
        dataSource: 'dashboard_stats',
        refreshInterval: 30
      },
      {
        id: 'approved_loans',
        title: 'Approved Loans',
        type: 'stat',
        size: 'small',
        icon: 'fas fa-check-circle',
        color: '#10b981',
        dataSource: 'dashboard_stats',
        refreshInterval: 30
      },
      {
        id: 'total_loan_amount',
        title: 'Total Loan Amount',
        type: 'stat',
        size: 'small',
        icon: 'fas fa-money-bill-wave',
        color: '#8b5cf6',
        dataSource: 'dashboard_stats',
        refreshInterval: 30
      },
      {
        id: 'application_trends',
        title: 'Application Trends',
        type: 'chart',
        size: 'medium',
        chartType: 'line',
        dataSource: 'application_trends',
        refreshInterval: 60
      },
      {
        id: 'loan_status_distribution',
        title: 'Loan Status Distribution',
        type: 'chart',
        size: 'medium',
        chartType: 'doughnut',
        dataSource: 'loan_status_distribution',
        refreshInterval: 60
      },
      {
        id: 'recent_activities',
        title: 'Recent Activities',
        type: 'activity',
        size: 'large',
        dataSource: 'recent_activities',
        refreshInterval: 15
      },
      {
        id: 'system_performance',
        title: 'System Performance',
        type: 'progress',
        size: 'medium',
        dataSource: 'system_performance',
        refreshInterval: 30
      }
    ],
    'LOAN_OFFICER': [
      {
        id: 'assigned_loans',
        title: 'Assigned Loans',
        type: 'stat',
        size: 'small',
        icon: 'fas fa-tasks',
        color: '#3b82f6',
        dataSource: 'officer_stats',
        refreshInterval: 30
      },
      {
        id: 'pending_review',
        title: 'Pending Review',
        type: 'stat',
        size: 'small',
        icon: 'fas fa-hourglass-half',
        color: '#f59e0b',
        dataSource: 'officer_stats',
        refreshInterval: 30
      },
      {
        id: 'completed_today',
        title: 'Completed Today',
        type: 'stat',
        size: 'small',
        icon: 'fas fa-check-double',
        color: '#10b981',
        dataSource: 'officer_stats',
        refreshInterval: 30
      },
      {
        id: 'high_risk_loans',
        title: 'High Risk Loans',
        type: 'stat',
        size: 'small',
        icon: 'fas fa-exclamation-triangle',
        color: '#ef4444',
        dataSource: 'officer_stats',
        refreshInterval: 30
      },
      {
        id: 'workload_distribution',
        title: 'Workload Distribution',
        type: 'chart',
        size: 'medium',
        chartType: 'bar',
        dataSource: 'workload_distribution',
        refreshInterval: 60
      },
      {
        id: 'recent_assignments',
        title: 'Recent Assignments',
        type: 'list',
        size: 'large',
        dataSource: 'recent_assignments',
        refreshInterval: 30
      }
    ],
    'APPLICANT': [
      {
        id: 'my_applications',
        title: 'My Applications',
        type: 'stat',
        size: 'small',
        icon: 'fas fa-file-alt',
        color: '#3b82f6',
        dataSource: 'applicant_stats',
        refreshInterval: 60
      },
      {
        id: 'application_status',
        title: 'Application Status',
        type: 'progress',
        size: 'medium',
        dataSource: 'application_progress',
        refreshInterval: 30
      },
      {
        id: 'loan_eligibility',
        title: 'Loan Eligibility',
        type: 'metric',
        size: 'medium',
        dataSource: 'eligibility_score',
        refreshInterval: 300
      },
      {
        id: 'document_status',
        title: 'Document Status',
        type: 'list',
        size: 'medium',
        dataSource: 'document_status',
        refreshInterval: 60
      }
    ]
  };

  constructor(private adminService: AdminService) {}

  ngOnInit() {
    this.initializeWidgets();
    this.loadAllWidgetData();
    
    if (this.autoRefresh) {
      this.startAutoRefresh();
    }
  }

  ngAfterViewInit() {
    // Initialize charts after view is ready
    setTimeout(() => {
      this.initializeCharts();
    }, 100);
  }

  ngOnDestroy() {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.destroyAllCharts();
  }

  private initializeWidgets() {
    if (this.widgets.length === 0 && this.userRole) {
      this.widgets = this.defaultWidgets[this.userRole] || [];
    }
    
    // Initialize loading states
    this.widgets.forEach(widget => {
      this.isLoading[widget.id] = false;
      this.errors[widget.id] = '';
    });
  }

  private loadAllWidgetData() {
    this.widgets.forEach(widget => {
      this.loadWidgetData(widget);
    });
  }

  private loadWidgetData(widget: WidgetConfig) {
    this.isLoading[widget.id] = true;
    this.errors[widget.id] = '';

    switch (widget.dataSource) {
      case 'dashboard_stats':
        this.loadDashboardStats(widget);
        break;
      case 'application_trends':
        this.loadApplicationTrends(widget);
        break;
      case 'loan_status_distribution':
        this.loadLoanStatusDistribution(widget);
        break;
      case 'recent_activities':
        this.loadRecentActivities(widget);
        break;
      case 'system_performance':
        this.loadSystemPerformance(widget);
        break;
      case 'officer_stats':
        this.loadOfficerStats(widget);
        break;
      case 'workload_distribution':
        this.loadWorkloadDistribution(widget);
        break;
      case 'recent_assignments':
        this.loadRecentAssignments(widget);
        break;
      default:
        this.loadMockData(widget);
    }
  }

  private loadDashboardStats(widget: WidgetConfig) {
    const statsSub = this.adminService.getDashboardStats().subscribe({
      next: (stats) => {
        this.processDashboardStats(widget, stats);
        this.isLoading[widget.id] = false;
      },
      error: (error) => {
        this.errors[widget.id] = 'Failed to load data';
        this.isLoading[widget.id] = false;
        console.error(`Failed to load ${widget.id}:`, error);
      }
    });
    this.subscriptions.push(statsSub);
  }

  private processDashboardStats(widget: WidgetConfig, stats: any) {
    switch (widget.id) {
      case 'total_applicants':
        this.widgetData[widget.id] = {
          value: stats.totalApplicants || 0,
          label: 'Total Applicants',
          trend: this.calculateTrend(stats.totalApplicants, stats.previousTotalApplicants),
          trendDirection: this.getTrendDirection(stats.totalApplicants, stats.previousTotalApplicants),
          lastUpdated: new Date(),
          status: 'info'
        };
        break;
      case 'pending_applications':
        this.widgetData[widget.id] = {
          value: stats.pendingApplications || 0,
          label: 'Pending Applications',
          trend: this.calculateTrend(stats.pendingApplications, stats.previousPendingApplications),
          trendDirection: this.getTrendDirection(stats.pendingApplications, stats.previousPendingApplications),
          lastUpdated: new Date(),
          status: stats.pendingApplications > 10 ? 'warning' : 'success'
        };
        break;
      case 'approved_loans':
        this.widgetData[widget.id] = {
          value: stats.approvedLoans || 0,
          label: 'Approved Loans',
          trend: this.calculateTrend(stats.approvedLoans, stats.previousApprovedLoans),
          trendDirection: this.getTrendDirection(stats.approvedLoans, stats.previousApprovedLoans),
          lastUpdated: new Date(),
          status: 'success'
        };
        break;
      case 'total_loan_amount':
        this.widgetData[widget.id] = {
          value: this.formatCurrency(stats.averageLoanAmount * (stats.approvedLoans || 0)),
          label: 'Total Loan Amount',
          trend: this.calculateTrend(stats.averageLoanAmount, stats.previousAverageLoanAmount),
          trendDirection: this.getTrendDirection(stats.averageLoanAmount, stats.previousAverageLoanAmount),
          lastUpdated: new Date(),
          status: 'info'
        };
        break;
    }
  }

  private loadApplicationTrends(widget: WidgetConfig) {
    // Generate trend data based on current stats
    const trendData = this.generateTrendData();
    
    this.widgetData[widget.id] = {
      chartData: {
        labels: trendData.labels,
        datasets: [{
          label: 'Applications',
          data: trendData.data,
          borderColor: '#3b82f6',
          backgroundColor: 'rgba(59, 130, 246, 0.1)',
          tension: 0.4,
          fill: true
        }]
      },
      lastUpdated: new Date()
    };
    
    this.isLoading[widget.id] = false;
    
    // Update chart if it exists
    setTimeout(() => {
      this.updateChart(widget.id);
    }, 100);
  }

  private loadLoanStatusDistribution(widget: WidgetConfig) {
    const statsSub = this.adminService.getDashboardStats().subscribe({
      next: (stats) => {
        this.widgetData[widget.id] = {
          chartData: {
            labels: ['Approved', 'Pending', 'Rejected'],
            datasets: [{
              data: [
                stats.approvedLoans || 0,
                stats.pendingApplications || 0,
                stats.rejectedApplications || 0
              ],
              backgroundColor: ['#10b981', '#f59e0b', '#ef4444'],
              borderWidth: 0
            }]
          },
          lastUpdated: new Date()
        };
        
        this.isLoading[widget.id] = false;
        
        // Update chart if it exists
        setTimeout(() => {
          this.updateChart(widget.id);
        }, 100);
      },
      error: (error) => {
        this.errors[widget.id] = 'Failed to load data';
        this.isLoading[widget.id] = false;
      }
    });
    this.subscriptions.push(statsSub);
  }

  private loadRecentActivities(widget: WidgetConfig) {
    const activitiesSub = this.adminService.getRecentActivityLogs().subscribe({
      next: (activities) => {
        this.widgetData[widget.id] = {
          listItems: activities.slice(0, 5).map(activity => ({
            id: activity.logId,
            title: this.formatActivityTitle(activity),
            description: activity.description,
            timestamp: new Date(activity.timestamp),
            status: this.getActivityStatus(activity.status),
            icon: this.getActivityIcon(activity.activityType)
          })),
          lastUpdated: new Date()
        };
        this.isLoading[widget.id] = false;
      },
      error: (error) => {
        this.errors[widget.id] = 'Failed to load activities';
        this.isLoading[widget.id] = false;
      }
    });
    this.subscriptions.push(activitiesSub);
  }

  private loadSystemPerformance(widget: WidgetConfig) {
    // Mock system performance data
    this.widgetData[widget.id] = {
      progress: Math.floor(Math.random() * 30) + 70, // 70-100%
      label: 'System Health',
      status: 'success',
      metadata: {
        cpu: Math.floor(Math.random() * 40) + 30,
        memory: Math.floor(Math.random() * 50) + 40,
        storage: Math.floor(Math.random() * 30) + 20
      },
      lastUpdated: new Date()
    };
    this.isLoading[widget.id] = false;
  }

  private loadOfficerStats(widget: WidgetConfig) {
    // Mock officer statistics
    const mockStats = {
      assignedLoans: Math.floor(Math.random() * 20) + 5,
      pendingReview: Math.floor(Math.random() * 10) + 2,
      completedToday: Math.floor(Math.random() * 8) + 1,
      highRiskLoans: Math.floor(Math.random() * 5) + 1
    };

    switch (widget.id) {
      case 'assigned_loans':
        this.widgetData[widget.id] = {
          value: mockStats.assignedLoans,
          label: 'Assigned Loans',
          status: 'info',
          lastUpdated: new Date()
        };
        break;
      case 'pending_review':
        this.widgetData[widget.id] = {
          value: mockStats.pendingReview,
          label: 'Pending Review',
          status: mockStats.pendingReview > 5 ? 'warning' : 'success',
          lastUpdated: new Date()
        };
        break;
      case 'completed_today':
        this.widgetData[widget.id] = {
          value: mockStats.completedToday,
          label: 'Completed Today',
          status: 'success',
          lastUpdated: new Date()
        };
        break;
      case 'high_risk_loans':
        this.widgetData[widget.id] = {
          value: mockStats.highRiskLoans,
          label: 'High Risk Loans',
          status: mockStats.highRiskLoans > 3 ? 'danger' : 'warning',
          lastUpdated: new Date()
        };
        break;
    }
    this.isLoading[widget.id] = false;
  }

  private loadWorkloadDistribution(widget: WidgetConfig) {
    this.widgetData[widget.id] = {
      chartData: {
        labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri'],
        datasets: [{
          label: 'Loans Processed',
          data: [12, 19, 15, 17, 14],
          backgroundColor: '#3b82f6',
          borderRadius: 4
        }]
      },
      lastUpdated: new Date()
    };
    
    this.isLoading[widget.id] = false;
    
    setTimeout(() => {
      this.updateChart(widget.id);
    }, 100);
  }

  private loadRecentAssignments(widget: WidgetConfig) {
    // Mock recent assignments
    this.widgetData[widget.id] = {
      listItems: [
        {
          id: 1,
          title: 'Loan Application #1001',
          description: 'Personal Loan - ₹5,00,000',
          timestamp: new Date(Date.now() - 30 * 60000),
          status: 'warning',
          icon: 'fas fa-file-alt'
        },
        {
          id: 2,
          title: 'Loan Application #1002',
          description: 'Home Loan - ₹25,00,000',
          timestamp: new Date(Date.now() - 60 * 60000),
          status: 'info',
          icon: 'fas fa-home'
        },
        {
          id: 3,
          title: 'Loan Application #1003',
          description: 'Business Loan - ₹10,00,000',
          timestamp: new Date(Date.now() - 120 * 60000),
          status: 'danger',
          icon: 'fas fa-briefcase'
        }
      ],
      lastUpdated: new Date()
    };
    this.isLoading[widget.id] = false;
  }

  private loadMockData(widget: WidgetConfig) {
    // Fallback mock data
    this.widgetData[widget.id] = {
      value: Math.floor(Math.random() * 100),
      label: widget.title,
      status: 'info',
      lastUpdated: new Date()
    };
    this.isLoading[widget.id] = false;
  }

  private initializeCharts() {
    this.widgets.forEach(widget => {
      if (widget.type === 'chart' && this.widgetData[widget.id]?.chartData) {
        this.createChart(widget);
      }
    });
  }

  private createChart(widget: WidgetConfig) {
    const canvas = document.getElementById(`chart-${widget.id}`) as HTMLCanvasElement;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // Destroy existing chart
    if (this.charts[widget.id]) {
      this.charts[widget.id].destroy();
    }

    const config: ChartConfiguration = {
      type: widget.chartType || 'line',
      data: this.widgetData[widget.id].chartData,
      options: this.getChartOptions(widget.chartType || 'line')
    };

    this.charts[widget.id] = new Chart(ctx, config);
  }

  private updateChart(widgetId: string) {
    const chart = this.charts[widgetId];
    const data = this.widgetData[widgetId]?.chartData;
    
    if (chart && data) {
      chart.data = data;
      chart.update('none');
    }
  }

  private getChartOptions(chartType: ChartType): any {
    const baseOptions = {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          display: chartType !== 'doughnut'
        }
      }
    };

    switch (chartType) {
      case 'line':
        return {
          ...baseOptions,
          scales: {
            y: {
              beginAtZero: true,
              grid: {
                color: 'rgba(0, 0, 0, 0.1)'
              }
            },
            x: {
              grid: {
                display: false
              }
            }
          }
        };
      case 'bar':
        return {
          ...baseOptions,
          scales: {
            y: {
              beginAtZero: true
            }
          }
        };
      case 'doughnut':
        return {
          ...baseOptions,
          cutout: '60%'
        };
      default:
        return baseOptions;
    }
  }

  private startAutoRefresh() {
    this.widgets.forEach(widget => {
      if (widget.refreshInterval) {
        const refreshSub = interval(widget.refreshInterval * 1000).subscribe(() => {
          this.loadWidgetData(widget);
        });
        this.subscriptions.push(refreshSub);
      }
    });
  }

  private destroyAllCharts() {
    Object.values(this.charts).forEach(chart => {
      if (chart) {
        chart.destroy();
      }
    });
    this.charts = {};
  }

  // Utility methods
  private calculateTrend(current: number, previous: number): number {
    if (!previous || previous === 0) return 0;
    return Math.round(((current - previous) / previous) * 100);
  }

  private getTrendDirection(current: number, previous: number): 'up' | 'down' | 'stable' {
    if (!previous) return 'stable';
    if (current > previous) return 'up';
    if (current < previous) return 'down';
    return 'stable';
  }

  private formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(amount);
  }

  private generateTrendData(): { labels: string[]; data: number[] } {
    const labels = [];
    const data = [];
    const now = new Date();
    
    for (let i = 6; i >= 0; i--) {
      const date = new Date(now.getTime() - i * 24 * 60 * 60 * 1000);
      labels.push(date.toLocaleDateString('en-US', { weekday: 'short' }));
      data.push(Math.floor(Math.random() * 20) + 5);
    }
    
    return { labels, data };
  }

  private formatActivityTitle(activity: any): string {
    return `${activity.performedBy} ${activity.activityType.toLowerCase()} ${activity.entityType}`;
  }

  private getActivityStatus(status: string): 'success' | 'warning' | 'danger' | 'info' {
    switch (status?.toLowerCase()) {
      case 'success': return 'success';
      case 'failed': return 'danger';
      case 'pending': return 'warning';
      default: return 'info';
    }
  }

  private getActivityIcon(activityType: string): string {
    switch (activityType) {
      case 'CREATE': return 'fas fa-plus-circle';
      case 'UPDATE': return 'fas fa-edit';
      case 'DELETE': return 'fas fa-trash';
      case 'APPROVE': return 'fas fa-check-circle';
      case 'REJECT': return 'fas fa-times-circle';
      default: return 'fas fa-info-circle';
    }
  }

  // Public methods for template
  getWidgetGridClass(widget: WidgetConfig): string {
    const sizeMap = {
      'small': 'widget-small',
      'medium': 'widget-medium', 
      'large': 'widget-large',
      'full': 'widget-full'
    };
    return sizeMap[widget.size] || 'widget-medium';
  }

  refreshWidget(widget: WidgetConfig) {
    this.loadWidgetData(widget);
  }

  getRelativeTime(date: Date): string {
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(minutes / 60);
    
    if (minutes < 1) return 'Just now';
    if (minutes < 60) return `${minutes}m ago`;
    if (hours < 24) return `${hours}h ago`;
    return date.toLocaleDateString();
  }

  trackByWidgetId(index: number, widget: WidgetConfig): string {
    return widget.id;
  }

  getTrendIcon(direction?: 'up' | 'down' | 'stable'): string {
    switch (direction) {
      case 'up': return 'fas fa-arrow-up';
      case 'down': return 'fas fa-arrow-down';
      default: return 'fas fa-minus';
    }
  }

  // Expose Math for template
  Math = Math;
}
