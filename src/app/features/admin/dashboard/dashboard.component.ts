import { Component, OnInit, ViewChild, ElementRef, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { AdminService, DashboardStats } from '../../../core/services/admin.service';
import { ThemeService, Theme } from '../../../core/services/theme.service';
import { AuthService } from '../../../core/services/auth.service';
import { ApplicantsComponent } from '../applicants/applicants.component';
import { LoansComponent } from '../loans/loans.component';
import { Subscription } from 'rxjs';

Chart.register(...registerables);

interface Activity {
  id: number;
  title: string;
  description: string;
  timestamp: Date;
  type: 'success' | 'warning' | 'info' | 'danger';
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, ApplicantsComponent, LoansComponent],
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
  
  private monthlyChart: Chart | null = null;
  private statusChart: Chart | null = null;
  private themeSubscription: Subscription | null = null;

  constructor(
    private adminService: AdminService,
    private themeService: ThemeService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadDashboardData();
    this.loadRecentActivities();
    
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
    if (this.monthlyChart) {
      this.monthlyChart.destroy();
    }
    if (this.statusChart) {
      this.statusChart.destroy();
    }
  }

  private loadDashboardData(): void {
    // Load real data from backend
    this.adminService.getDashboardStats().subscribe({
      next: (stats) => {
        this.stats = stats;
        // Update charts after data is loaded
        setTimeout(() => {
          this.initializeCharts();
        }, 100);
      },
      error: (error) => {
        console.error('Error loading dashboard stats:', error);
        // Fallback to mock data if backend fails
        this.stats = {
          totalApplicants: 0,
          pendingApplications: 0,
          approvedLoans: 0,
          rejectedApplications: 0,
          totalLoanAmount: 0,
          averageLoanAmount: 0,
          monthlyApplications: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
          loanStatusDistribution: [
            { status: 'Approved', count: 0 },
            { status: 'Pending', count: 0 },
            { status: 'Rejected', count: 0 },
            { status: 'Under Review', count: 0 }
          ]
        };
      }
    });
  }

  private loadRecentActivities(): void {
    // Mock data for demonstration
    this.recentActivities = [
      {
        id: 1,
        title: 'New Loan Application',
        description: 'John Doe submitted a new loan application for $50,000',
        timestamp: new Date(Date.now() - 10 * 60 * 1000), // 10 minutes ago
        type: 'info'
      },
      {
        id: 2,
        title: 'Application Approved',
        description: 'Loan application #LA-2024-001 has been approved',
        timestamp: new Date(Date.now() - 30 * 60 * 1000), // 30 minutes ago
        type: 'success'
      },
      {
        id: 3,
        title: 'Document Upload',
        description: 'Sarah Smith uploaded required documents',
        timestamp: new Date(Date.now() - 45 * 60 * 1000), // 45 minutes ago
        type: 'info'
      },
      {
        id: 4,
        title: 'Application Rejected',
        description: 'Loan application #LA-2024-002 was rejected due to insufficient income',
        timestamp: new Date(Date.now() - 2 * 60 * 60 * 1000), // 2 hours ago
        type: 'danger'
      },
      {
        id: 5,
        title: 'System Maintenance',
        description: 'Scheduled system maintenance completed successfully',
        timestamp: new Date(Date.now() - 4 * 60 * 60 * 1000), // 4 hours ago
        type: 'warning'
      }
    ];
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
  }

  // Utility methods
  onImageError(): void {
    this.showFallbackLogo = true;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }
}
