import { Component, OnInit, ViewChild, ElementRef, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { AdminService, DashboardStats, ActivityLog } from '../../../core/services/admin.service';
import { ThemeService, Theme } from '../../../core/services/theme.service';
import { AuthService } from '../../../core/services/auth.service';
import { ApplicantsComponent } from '../applicants/applicants.component';
import { LoansComponent } from '../loans/loans.component';
import { LoanOfficersComponent } from '../loan-officers/loan-officers.component';
import { ComplianceOfficersComponent } from '../compliance-officers/compliance-officers.component';
import { ReportsComponent } from '../reports/reports.component';
import { ActivityLogsComponent } from '../activity-logs/activity-logs.component';
import { FraudRulesComponent } from '../fraud-rules/fraud-rules.component';
import { Subscription } from 'rxjs';

Chart.register(...registerables);

interface Activity {
  id: number;
  title: string;
  description: string;
  timestamp: Date;
  type: 'success' | 'warning' | 'info' | 'danger';
  icon: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, ApplicantsComponent, LoansComponent, LoanOfficersComponent, ComplianceOfficersComponent, ReportsComponent, ActivityLogsComponent, FraudRulesComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('monthlyChart') monthlyChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('statusChart') statusChartRef!: ElementRef<HTMLCanvasElement>;

  activeTab = 'dashboard';
  currentTheme: Theme = 'system';
  showFallbackLogo = false;
  notificationCount = 3;

  stats: DashboardStats | null = null;
  recentActivities: Activity[] = [];
  isLoadingStats = true;
  isLoadingActivities = true;
  
  private monthlyChart: Chart | null = null;
  private statusChart: Chart | null = null;
  private themeSubscription: Subscription | null = null;
  private statsSubscription: Subscription | null = null;
  private activitiesSubscription: Subscription | null = null;

  constructor(
    private adminService: AdminService,
    private themeService: ThemeService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initializeData();
    
    // Subscribe to theme changes
    this.themeSubscription = this.themeService.theme$.subscribe(theme => {
      this.currentTheme = theme;
      // Update charts when theme changes
      setTimeout(() => {
        this.updateChartsForTheme();
      }, 100);
    });
  }

  ngAfterViewInit(): void {
    // Initialize charts after view is ready
    setTimeout(() => {
      this.initializeCharts();
    }, 100);
  }

  ngOnDestroy(): void {
    if (this.themeSubscription) {
      this.themeSubscription.unsubscribe();
    }
    if (this.statsSubscription) {
      this.statsSubscription.unsubscribe();
    }
    if (this.activitiesSubscription) {
      this.activitiesSubscription.unsubscribe();
    }
    if (this.monthlyChart) {
      this.monthlyChart.destroy();
    }
    if (this.statusChart) {
      this.statusChart.destroy();
    }
  }

  private initializeData(): void {
    // Check if we already have cached data
    const cachedStats = this.adminService.getCachedDashboardStats();
    const cachedActivities = this.adminService.getCachedRecentActivities();

    if (cachedStats) {
      this.stats = cachedStats;
      this.isLoadingStats = false;
      setTimeout(() => {
        this.initializeCharts();
      }, 100);
    }

    if (cachedActivities && cachedActivities.length > 0) {
      this.recentActivities = cachedActivities.map(log => this.mapActivityLogToActivity(log));
      this.isLoadingActivities = false;
    }

    // Subscribe to dashboard stats observable
    this.statsSubscription = this.adminService.dashboardStats$.subscribe(stats => {
      if (stats) {
        this.stats = stats;
        this.isLoadingStats = false;
        setTimeout(() => {
          this.initializeCharts();
        }, 100);
      }
    });

    // Subscribe to recent activities observable
    this.activitiesSubscription = this.adminService.recentActivities$.subscribe(activities => {
      if (activities && activities.length > 0) {
        this.recentActivities = activities.map(log => this.mapActivityLogToActivity(log));
        this.isLoadingActivities = false;
      }
    });

    // Load fresh data if not cached
    this.loadDashboardData();
    this.loadRecentActivities();
  }

  private loadDashboardData(): void {
    this.adminService.getDashboardStats().subscribe({
      next: (stats) => {
        console.log('Dashboard stats loaded:', stats);
        this.isLoadingStats = false;
      },
      error: (error) => {
        console.error('Error loading dashboard stats:', error);
        this.isLoadingStats = false;
      }
    });
  }

  private loadRecentActivities(): void {
    this.adminService.getRecentActivityLogs().subscribe({
      next: (logs) => {
        console.log('Recent activity logs loaded:', logs);
        this.isLoadingActivities = false;
      },
      error: (error) => {
        console.error('Error loading recent activities:', error);
        this.isLoadingActivities = false;
      }
    });
  }

  private mapActivityLogToActivity(log: any): Activity {
    // Map activity type to visual type and icon
    const typeMapping: { [key: string]: { type: 'success' | 'warning' | 'info' | 'danger', icon: string } } = {
      'LOGIN': { type: 'info', icon: 'fa-sign-in-alt' },
      'LOGOUT': { type: 'info', icon: 'fa-sign-out-alt' },
      'CREATE': { type: 'success', icon: 'fa-plus-circle' },
      'UPDATE': { type: 'info', icon: 'fa-edit' },
      'DELETE': { type: 'danger', icon: 'fa-trash' },
      'APPROVE': { type: 'success', icon: 'fa-check-circle' },
      'REJECT': { type: 'danger', icon: 'fa-times-circle' },
      'SUBMIT': { type: 'info', icon: 'fa-paper-plane' },
      'UPLOAD': { type: 'info', icon: 'fa-upload' },
      'DOWNLOAD': { type: 'info', icon: 'fa-download' },
      'MAINTENANCE': { type: 'warning', icon: 'fa-tools' }
    };

    const mapping = typeMapping[log.activityType] || { type: 'info', icon: 'fa-info-circle' };

    return {
      id: log.logId,
      title: this.formatActivityTitle(log),
      description: log.description || this.generateDescription(log),
      timestamp: new Date(log.timestamp),
      type: log.status === 'FAILED' ? 'danger' : mapping.type,
      icon: mapping.icon
    };
  }

  private formatActivityTitle(log: any): string {
    const activityTypeFormatted = log.activityType.replace(/_/g, ' ').toLowerCase()
      .split(' ')
      .map((word: string) => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');

    if (log.entityType) {
      const entityTypeFormatted = log.entityType.replace(/_/g, ' ').toLowerCase()
        .split(' ')
        .map((word: string) => word.charAt(0).toUpperCase() + word.slice(1))
        .join(' ');
      return `${activityTypeFormatted} ${entityTypeFormatted}`;
    }

    return activityTypeFormatted;
  }

  private generateDescription(log: any): string {
    let description = `${log.performedBy}`;
    
    if (log.activityType === 'LOGIN') {
      description += ' logged into the system';
    } else if (log.activityType === 'LOGOUT') {
      description += ' logged out of the system';
    } else if (log.activityType === 'CREATE' && log.entityType) {
      description += ` created a new ${log.entityType.toLowerCase()}`;
    } else if (log.activityType === 'UPDATE' && log.entityType) {
      description += ` updated ${log.entityType.toLowerCase()}`;
      if (log.entityId) {
        description += ` #${log.entityId}`;
      }
    } else if (log.activityType === 'DELETE' && log.entityType) {
      description += ` deleted ${log.entityType.toLowerCase()}`;
      if (log.entityId) {
        description += ` #${log.entityId}`;
      }
    } else if (log.activityType === 'APPROVE' && log.entityType) {
      description += ` approved ${log.entityType.toLowerCase()}`;
      if (log.entityId) {
        description += ` #${log.entityId}`;
      }
    } else if (log.activityType === 'REJECT' && log.entityType) {
      description += ` rejected ${log.entityType.toLowerCase()}`;
      if (log.entityId) {
        description += ` #${log.entityId}`;
      }
    } else {
      description += ` performed ${log.activityType.toLowerCase()}`;
    }

    if (log.status === 'FAILED') {
      description += ' (Failed)';
    }

    return description;
  }

  private initializeCharts(): void {
    if (this.monthlyChartRef && this.statusChartRef) {
      this.createMonthlyChart();
      this.createStatusChart();
    }
  }

  private createMonthlyChart(): void {
    const ctx = this.monthlyChartRef.nativeElement.getContext('2d');
    if (!ctx || !this.stats) return;

    const isDark = document.documentElement.classList.contains('dark-theme');
    const textColor = isDark ? '#cbd5e1' : '#64748b';
    const gridColor = isDark ? '#475569' : '#e2e8f0';

    const config: ChartConfiguration = {
      type: 'line',
      data: {
        labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'],
        datasets: [{
          label: 'Applications',
          data: this.stats.monthlyApplications,
          borderColor: '#3b82f6',
          backgroundColor: 'rgba(59, 130, 246, 0.1)',
          borderWidth: 3,
          fill: true,
          tension: 0.4,
          pointBackgroundColor: '#3b82f6',
          pointBorderColor: '#ffffff',
          pointBorderWidth: 2,
          pointRadius: 6,
          pointHoverRadius: 8
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            display: false
          }
        },
        scales: {
          x: {
            grid: {
              color: gridColor
            },
            ticks: {
              color: textColor,
              font: {
                size: 12
              }
            }
          },
          y: {
            grid: {
              color: gridColor
            },
            ticks: {
              color: textColor,
              font: {
                size: 12
              }
            }
          }
        },
        elements: {
          point: {
            hoverBackgroundColor: '#3b82f6'
          }
        }
      }
    };

    this.monthlyChart = new Chart(ctx, config);
  }

  private createStatusChart(): void {
    const ctx = this.statusChartRef.nativeElement.getContext('2d');
    if (!ctx || !this.stats) return;

    const isDark = document.documentElement.classList.contains('dark-theme');
    const textColor = isDark ? '#cbd5e1' : '#64748b';

    const config: ChartConfiguration = {
      type: 'doughnut',
      data: {
        labels: this.stats.loanStatusDistribution.map(item => item.status),
        datasets: [{
          data: this.stats.loanStatusDistribution.map(item => item.count),
          backgroundColor: [
            '#10b981', // Approved - Green
            '#f59e0b', // Pending - Yellow
            '#ef4444', // Rejected - Red
            '#6366f1'  // Under Review - Indigo
          ],
          borderWidth: 0,
          hoverOffset: 4
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              color: textColor,
              font: {
                size: 12
              },
              padding: 20,
              usePointStyle: true,
              pointStyle: 'circle'
            }
          }
        }
      }
    };

    this.statusChart = new Chart(ctx, config);
  }

  private updateChartsForTheme(): void {
    if (this.monthlyChart) {
      this.monthlyChart.destroy();
    }
    if (this.statusChart) {
      this.statusChart.destroy();
    }
    
    setTimeout(() => {
      this.initializeCharts();
    }, 50);
  }

  // Theme methods
  setTheme(theme: Theme): void {
    this.themeService.setTheme(theme);
  }

  getThemeIcon(): string {
    switch (this.currentTheme) {
      case 'light': return 'fa-sun';
      case 'dark': return 'fa-moon';
      case 'system': return 'fa-desktop';
      default: return 'fa-desktop';
    }
  }

  getThemeLabel(): string {
    switch (this.currentTheme) {
      case 'light': return 'Light';
      case 'dark': return 'Dark';
      case 'system': return 'System';
      default: return 'System';
    }
  }

  // Navigation methods
  setActiveTab(tab: string): void {
    this.activeTab = tab;
    
    // If returning to dashboard tab, ensure data is loaded
    if (tab === 'dashboard') {
      // Check if we need to refresh data
      if (!this.stats) {
        this.initializeData();
      }
    }
  }

  // Refresh data method
  refreshData(): void {
    this.isLoadingStats = true;
    this.isLoadingActivities = true;
    this.adminService.refreshDashboardData();
  }

  // Utility methods
  onImageError(): void {
    this.showFallbackLogo = true;
  }

  logout(): void {
    // Clear cached data on logout
    this.adminService.clearCache();
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }
}
