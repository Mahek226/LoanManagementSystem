import { Component, OnInit, ViewChild, ElementRef, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { AdminService } from '../../../core/services/admin.service';
import { ThemeService } from '../../../core/services/theme.service';
import { Subscription } from 'rxjs';

Chart.register(...registerables);

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './reports.component.html',
  styleUrl: './reports.component.css'
})
export class ReportsComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('applicantStatusChart') applicantStatusChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('activityTrendChart') activityTrendChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('ruleStatusChart') ruleStatusChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('severityChart') severityChartRef!: ElementRef<HTMLCanvasElement>;

  loading = false;
  dashboardStats: any = null;
  activityStats: any = null;
  ruleStats: any = null;
  
  private applicantStatusChart: Chart | null = null;
  private activityTrendChart: Chart | null = null;
  private ruleStatusChart: Chart | null = null;
  private severityChart: Chart | null = null;
  private themeSubscription: Subscription | null = null;

  constructor(
    private adminService: AdminService,
    private themeService: ThemeService
  ) {}

  ngOnInit(): void {
    this.loadAllData();
    
    this.themeSubscription = this.themeService.theme$.subscribe(() => {
      setTimeout(() => {
        this.updateChartsForTheme();
      }, 100);
    });
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.initializeCharts();
    }, 100);
  }

  ngOnDestroy(): void {
    if (this.themeSubscription) {
      this.themeSubscription.unsubscribe();
    }
    this.destroyCharts();
  }

  private loadAllData(): void {
    this.loading = true;

    // Load all statistics
    this.adminService.getDashboardStats().subscribe({
      next: (stats) => {
        this.dashboardStats = stats;
        setTimeout(() => this.initializeCharts(), 100);
      },
      error: (err) => console.error('Error loading dashboard stats:', err)
    });

    this.adminService.getActivityStatistics().subscribe({
      next: (stats) => {
        this.activityStats = stats;
        setTimeout(() => this.initializeCharts(), 100);
      },
      error: (err) => console.error('Error loading activity stats:', err)
    });

    this.adminService.getFraudRuleStatistics().subscribe({
      next: (stats) => {
        this.ruleStats = stats;
        setTimeout(() => this.initializeCharts(), 100);
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading fraud rule stats:', err);
        this.loading = false;
      }
    });
  }

  private initializeCharts(): void {
    if (this.applicantStatusChartRef && this.dashboardStats) {
      this.createApplicantStatusChart();
    }
    if (this.activityTrendChartRef && this.activityStats) {
      this.createActivityTrendChart();
    }
    if (this.ruleStatusChartRef && this.ruleStats) {
      this.createRuleStatusChart();
    }
    if (this.severityChartRef && this.ruleStats) {
      this.createSeverityChart();
    }
  }

  private createApplicantStatusChart(): void {
    const ctx = this.applicantStatusChartRef.nativeElement.getContext('2d');
    if (!ctx || !this.dashboardStats) return;

    const isDark = document.documentElement.classList.contains('dark-theme');
    const textColor = isDark ? '#cbd5e1' : '#64748b';

    const config: ChartConfiguration = {
      type: 'doughnut',
      data: {
        labels: this.dashboardStats.loanStatusDistribution.map((item: any) => item.status),
        datasets: [{
          data: this.dashboardStats.loanStatusDistribution.map((item: any) => item.count),
          backgroundColor: ['#10b981', '#f59e0b', '#ef4444', '#6366f1'],
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
              font: { size: 12 },
              padding: 20,
              usePointStyle: true
            }
          },
          title: {
            display: true,
            text: 'Applicant Status Distribution',
            color: textColor,
            font: { size: 16, weight: 'bold' }
          }
        }
      }
    };

    this.applicantStatusChart = new Chart(ctx, config);
  }

  private createActivityTrendChart(): void {
    const ctx = this.activityTrendChartRef.nativeElement.getContext('2d');
    if (!ctx || !this.activityStats) return;

    const isDark = document.documentElement.classList.contains('dark-theme');
    const textColor = isDark ? '#cbd5e1' : '#64748b';
    const gridColor = isDark ? '#475569' : '#e2e8f0';

    const config: ChartConfiguration = {
      type: 'bar',
      data: {
        labels: ['Login', 'Create', 'Update', 'Delete', 'Approve', 'Reject'],
        datasets: [{
          label: 'Activity Count',
          data: [
            this.activityStats.loginCount || 0,
            this.activityStats.createCount || 0,
            this.activityStats.updateCount || 0,
            this.activityStats.deleteCount || 0,
            this.activityStats.approveCount || 0,
            this.activityStats.rejectCount || 0
          ],
          backgroundColor: ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899'],
          borderRadius: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          title: {
            display: true,
            text: 'Activity Type Distribution',
            color: textColor,
            font: { size: 16, weight: 'bold' }
          }
        },
        scales: {
          x: {
            grid: { color: gridColor },
            ticks: { color: textColor }
          },
          y: {
            grid: { color: gridColor },
            ticks: { color: textColor },
            beginAtZero: true
          }
        }
      }
    };

    this.activityTrendChart = new Chart(ctx, config);
  }

  private createRuleStatusChart(): void {
    const ctx = this.ruleStatusChartRef.nativeElement.getContext('2d');
    if (!ctx || !this.ruleStats) return;

    const isDark = document.documentElement.classList.contains('dark-theme');
    const textColor = isDark ? '#cbd5e1' : '#64748b';

    const config: ChartConfiguration = {
      type: 'pie',
      data: {
        labels: ['Active Rules', 'Inactive Rules'],
        datasets: [{
          data: [this.ruleStats.activeRules || 0, this.ruleStats.inactiveRules || 0],
          backgroundColor: ['#10b981', '#94a3b8'],
          borderWidth: 0
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
              padding: 15,
              usePointStyle: true
            }
          },
          title: {
            display: true,
            text: 'Rule Status',
            color: textColor,
            font: { size: 16, weight: 'bold' }
          }
        }
      }
    };

    this.ruleStatusChart = new Chart(ctx, config);
  }

  private createSeverityChart(): void {
    const ctx = this.severityChartRef.nativeElement.getContext('2d');
    if (!ctx || !this.ruleStats) return;

    const isDark = document.documentElement.classList.contains('dark-theme');
    const textColor = isDark ? '#cbd5e1' : '#64748b';
    const gridColor = isDark ? '#475569' : '#e2e8f0';

    const config: ChartConfiguration = {
      type: 'bar',
      data: {
        labels: ['Critical', 'High', 'Medium', 'Low'],
        datasets: [{
          label: 'Rules',
          data: [
            this.ruleStats.criticalRules || 0,
            this.ruleStats.highRules || 0,
            this.ruleStats.mediumRules || 0,
            this.ruleStats.lowRules || 0
          ],
          backgroundColor: ['#dc2626', '#f59e0b', '#3b82f6', '#10b981'],
          borderRadius: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        indexAxis: 'y',
        plugins: {
          legend: { display: false },
          title: {
            display: true,
            text: 'Rules by Severity',
            color: textColor,
            font: { size: 16, weight: 'bold' }
          }
        },
        scales: {
          x: {
            grid: { color: gridColor },
            ticks: { color: textColor },
            beginAtZero: true
          },
          y: {
            grid: { color: gridColor },
            ticks: { color: textColor }
          }
        }
      }
    };

    this.severityChart = new Chart(ctx, config);
  }

  private updateChartsForTheme(): void {
    this.destroyCharts();
    setTimeout(() => {
      this.initializeCharts();
    }, 50);
  }

  private destroyCharts(): void {
    if (this.applicantStatusChart) this.applicantStatusChart.destroy();
    if (this.activityTrendChart) this.activityTrendChart.destroy();
    if (this.ruleStatusChart) this.ruleStatusChart.destroy();
    if (this.severityChart) this.severityChart.destroy();
  }

  exportReport(type: string): void {
    console.log('Exporting report:', type);
    // TODO: Implement report export functionality
  }
}
