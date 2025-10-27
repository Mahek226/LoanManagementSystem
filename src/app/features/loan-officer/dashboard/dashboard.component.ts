import { Component, OnInit, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { AuthService } from '@core/services/auth.service';
import { LoanOfficerService, LoanScreeningResponse, DashboardStats } from '@core/services/loan-officer.service';

Chart.register(...registerables);

@Component({
  selector: 'app-lo-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class LoDashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  officerName: string = '';
  officerId: number = 0;
  loading = false;
  error = '';
  
  loans: LoanScreeningResponse[] = [];
  stats: DashboardStats | null = null;
  recentLoans: LoanScreeningResponse[] = [];
  
  // Charts
  private statusChart: Chart | null = null;
  private riskChart: Chart | null = null;

  constructor(
    private authService: AuthService,
    private loanOfficerService: LoanOfficerService,
    private router: Router
  ) {
    const user = this.authService.currentUserValue;
    this.officerName = user?.firstName && user?.lastName 
      ? `${user.firstName} ${user.lastName}` 
      : user?.username || 'Officer';
    this.officerId = user?.officerId || user?.userId || 0;
  }

  ngOnInit(): void {
    this.loadDashboardData();
  }

  ngAfterViewInit(): void {
    // Initialize charts after view is ready
    setTimeout(() => {
      if (this.stats) {
        this.initializeCharts();
      }
    }, 100);
  }

  ngOnDestroy(): void {
    // Clean up charts
    if (this.statusChart) {
      this.statusChart.destroy();
    }
    if (this.riskChart) {
      this.riskChart.destroy();
    }
  }

  loadDashboardData(): void {
    this.loading = true;
    this.error = '';

    console.log('Loading dashboard data for officer ID:', this.officerId);

    if (!this.officerId || this.officerId === 0) {
      this.error = 'Officer ID not found. Please log in again.';
      this.loading = false;
      console.error('Invalid officer ID:', this.officerId);
      return;
    }

    this.loanOfficerService.getAssignedLoans(this.officerId).subscribe({
      next: (data) => {
        console.log('Received loan data:', data);
        this.loans = data;
        this.stats = this.loanOfficerService.calculateStats(data);
        this.recentLoans = data.slice(0, 5); // Show 5 most recent
        this.loading = false;
        
        // Initialize charts after data is loaded
        setTimeout(() => this.initializeCharts(), 100);
      },
      error: (err) => {
        console.error('Error loading dashboard data:', err);
        console.error('Error details:', err.error);
        console.error('Status:', err.status);
        
        if (err.status === 404) {
          this.error = 'No assigned loans found. Your dashboard will update when loans are assigned to you.';
          // Show empty state instead of error
          this.loans = [];
          this.stats = this.loanOfficerService.calculateStats([]);
          this.recentLoans = [];
          this.loading = false;
          setTimeout(() => this.initializeCharts(), 100);
        } else if (err.status === 0) {
          this.error = 'Cannot connect to server. Please check if the backend is running.';
          this.loading = false;
        } else {
          this.error = err.error?.message || 'Failed to load dashboard data. Please try again.';
          this.loading = false;
        }
      }
    });
  }

  initializeCharts(): void {
    if (!this.stats) return;

    this.initializeStatusChart();
    this.initializeRiskChart();
  }

  initializeStatusChart(): void {
    const canvas = document.getElementById('statusChart') as HTMLCanvasElement;
    if (!canvas || !this.stats) return;

    if (this.statusChart) {
      this.statusChart.destroy();
    }

    const config: ChartConfiguration = {
      type: 'doughnut',
      data: {
        labels: ['Pending Review', 'Approved', 'Rejected', 'Escalated'],
        datasets: [{
          data: [
            this.stats.pendingReview,
            this.stats.approved,
            this.stats.rejected,
            this.stats.escalated
          ],
          backgroundColor: ['#f59e0b', '#10b981', '#ef4444', '#06b6d4'],
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
              padding: 15,
              usePointStyle: true
            }
          }
        }
      }
    };

    this.statusChart = new Chart(canvas, config);
  }

  initializeRiskChart(): void {
    const canvas = document.getElementById('riskChart') as HTMLCanvasElement;
    if (!canvas || !this.stats) return;

    if (this.riskChart) {
      this.riskChart.destroy();
    }

    const config: ChartConfiguration = {
      type: 'bar',
      data: {
        labels: ['Low Risk', 'Medium Risk', 'High Risk'],
        datasets: [{
          label: 'Number of Loans',
          data: [
            this.stats.lowRiskCount,
            this.stats.mediumRiskCount,
            this.stats.highRiskCount
          ],
          backgroundColor: ['#10b981', '#f59e0b', '#ef4444'],
          borderRadius: 8
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
          y: {
            beginAtZero: true,
            ticks: {
              precision: 0
            }
          }
        }
      }
    };

    this.riskChart = new Chart(canvas, config);
  }

  viewAllLoans(): void {
    this.router.navigate(['/loan-officer/assigned-loans']);
  }

  reviewLoan(assignmentId: number): void {
    this.router.navigate(['/loan-officer/review', assignmentId]);
  }

  formatCurrency(amount: number): string {
    return this.loanOfficerService.formatCurrency(amount);
  }

  formatDate(dateString: string): string {
    return this.loanOfficerService.formatDate(dateString);
  }

  getRiskColor(riskLevel: string): string {
    return this.loanOfficerService.getRiskColor(riskLevel);
  }

  getStatusColor(status: string): string {
    return this.loanOfficerService.getStatusColor(status);
  }
}
