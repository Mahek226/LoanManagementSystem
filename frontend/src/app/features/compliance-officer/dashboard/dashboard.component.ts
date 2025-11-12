import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { ComplianceOfficerService, ComplianceEscalation, DashboardStats } from '../../../core/services/compliance-officer.service';
import { AuthService } from '../../../core/services/auth.service';

Chart.register(...registerables);

@Component({
  selector: 'app-compliance-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class ComplianceDashboardComponent implements OnInit, OnDestroy {
  stats: DashboardStats = {
    totalEscalations: 0,
    pendingReview: 0,
    approved: 0,
    rejected: 0,
    highRisk: 0,
    criticalRisk: 0
  };

  recentEscalations: ComplianceEscalation[] = [];
  loading: boolean = true;
  errorMessage: string = '';
  timeFilter: string = 'all';
  userName: string = '';

  private statusChart: Chart | null = null;
  private riskChart: Chart | null = null;

  constructor(
    private complianceService: ComplianceOfficerService,
    private authService: AuthService
  ) {
    const user = this.authService.currentUserValue;
    this.userName = user?.username || (user?.firstName && user?.lastName ? `${user.firstName} ${user.lastName}` : 'Compliance Officer');
  }

  ngOnInit(): void {
    this.loadDashboardData();
  }

  ngOnDestroy(): void {
    // Destroy charts to prevent memory leaks
    if (this.statusChart) {
      this.statusChart.destroy();
    }
    if (this.riskChart) {
      this.riskChart.destroy();
    }
  }

  loadDashboardData(): void {
    this.loading = true;
    this.complianceService.getEscalations().subscribe({
      next: (escalations) => {
        this.stats = this.complianceService.calculateStats(escalations);
        this.recentEscalations = escalations.slice(0, 5); // Get 5 most recent
        this.loading = false;
        
        // Create charts after data is loaded
        setTimeout(() => {
          this.createStatusChart();
          this.createRiskChart();
        }, 100);
      },
      error: (error) => {
        console.error('Error loading dashboard data:', error);
        this.errorMessage = 'Failed to load dashboard data. Please try again.';
        this.loading = false;
      }
    });
  }

  createStatusChart(): void {
    const canvas = document.getElementById('statusChart') as HTMLCanvasElement;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    if (this.statusChart) {
      this.statusChart.destroy();
    }

    const config: ChartConfiguration = {
      type: 'doughnut',
      data: {
        labels: ['Pending Review', 'Approved', 'Rejected'],
        datasets: [{
          data: [this.stats.pendingReview, this.stats.approved, this.stats.rejected],
          backgroundColor: ['#f59e0b', '#10b981', '#ef4444'],
          borderWidth: 0,
          hoverOffset: 10
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              padding: 15,
              font: { size: 12 },
              usePointStyle: true
            }
          },
          tooltip: {
            backgroundColor: 'rgba(0, 0, 0, 0.8)',
            padding: 12,
            titleFont: { size: 14, weight: 'bold' },
            bodyFont: { size: 13 },
            cornerRadius: 8
          }
        },
        animation: {
          duration: 1000,
          easing: 'easeOutQuart'
        }
      }
    };

    this.statusChart = new Chart(ctx, config);
  }

  createRiskChart(): void {
    const canvas = document.getElementById('riskChart') as HTMLCanvasElement;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    if (this.riskChart) {
      this.riskChart.destroy();
    }

    const config: ChartConfiguration = {
      type: 'bar',
      data: {
        labels: ['High Risk', 'Critical Risk'],
        datasets: [{
          label: 'Risk Level Distribution',
          data: [this.stats.highRisk, this.stats.criticalRisk],
          backgroundColor: ['#ef4444', '#dc2626'],
          borderRadius: 8,
          borderSkipped: false
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: 'rgba(0, 0, 0, 0.8)',
            padding: 12,
            titleFont: { size: 14, weight: 'bold' },
            bodyFont: { size: 13 },
            cornerRadius: 8
          }
        },
        scales: {
          y: {
            beginAtZero: true,
            ticks: { stepSize: 1 },
            grid: { color: 'rgba(0, 0, 0, 0.05)' }
          },
          x: {
            grid: { display: false }
          }
        },
        animation: {
          duration: 1000,
          easing: 'easeOutQuart'
        }
      }
    };

    this.riskChart = new Chart(ctx, config);
  }

  formatCurrency(amount: number): string {
    return this.complianceService.formatCurrency(amount);
  }

  formatDate(dateString: string): string {
    return this.complianceService.formatDate(dateString);
  }

  getRiskBadgeClass(riskLevel: string): string {
    return this.complianceService.getRiskBadgeClass(riskLevel);
  }

  getStatusBadgeClass(status: string): string {
    return this.complianceService.getStatusBadgeClass(status);
  }

  getPriorityLevel(escalation: ComplianceEscalation): string {
    return this.complianceService.getPriorityLevel(escalation);
  }

  getPriorityColor(priority: string): string {
    return this.complianceService.getPriorityColor(priority);
  }

  getCurrentDate(): string {
    return new Date().toLocaleDateString('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  onTimeFilterChange(): void {
    this.loadDashboardData();
  }
}
