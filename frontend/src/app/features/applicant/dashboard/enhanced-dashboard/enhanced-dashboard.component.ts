import { Component, OnInit, ViewChild, ElementRef, AfterViewInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { AuthService } from '@core/services/auth.service';
import { ApplicantService, LoanApplication, DashboardStats } from '@core/services/applicant.service';
import { DraftService, DraftApplication } from '@core/services/draft.service';

Chart.register(...registerables);

@Component({
  selector: 'app-enhanced-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './enhanced-dashboard.component.html',
  styleUrl: './enhanced-dashboard.component.css'
})
export class EnhancedDashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('statusChart') statusChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('amountChart') amountChartRef!: ElementRef<HTMLCanvasElement>;

  userName: string = '';
  applicantId: number = 0;
  loading = false;
  error = '';
  
  stats: DashboardStats | null = null;
  applications: LoanApplication[] = [];
  recentApplications: LoanApplication[] = [];
  draftApplications: DraftApplication[] = [];
  showDrafts = true;
  
  private statusChart: Chart | null = null;
  private amountChart: Chart | null = null;

  constructor(
    private authService: AuthService,
    private applicantService: ApplicantService,
    private draftService: DraftService,
    public router: Router
  ) {
    const user = this.authService.currentUserValue;
    this.userName = user?.firstName && user?.lastName 
      ? `${user.firstName} ${user.lastName}` 
      : user?.username || 'Applicant';
    this.applicantId = user?.applicantId || 0;
  }

  ngOnInit(): void {
    this.loadDashboardData();
    this.loadDraftApplications();
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.initializeCharts();
    }, 100);
  }

  ngOnDestroy(): void {
    this.destroyCharts();
  }

  loadDashboardData(): void {
    this.loading = true;
    this.error = '';

    this.applicantService.getMyApplications(this.applicantId).subscribe({
      next: (applications) => {
        this.applications = applications;
        this.stats = this.applicantService.calculateStats(applications);
        this.recentApplications = this.stats.recentApplications;
        this.loading = false;
        setTimeout(() => this.initializeCharts(), 100);
      },
      error: (err) => {
        this.error = 'Failed to load dashboard data';
        console.error('Error loading dashboard:', err);
        this.loading = false;
      }
    });
  }

  private initializeCharts(): void {
    if (this.statusChartRef && this.stats) {
      this.createStatusChart();
    }
    if (this.amountChartRef && this.applications.length > 0) {
      this.createAmountChart();
    }
  }

  private createStatusChart(): void {
    const ctx = this.statusChartRef.nativeElement.getContext('2d');
    if (!ctx || !this.stats) return;

    const config: ChartConfiguration = {
      type: 'doughnut',
      data: {
        labels: ['Pending', 'Approved', 'Rejected'],
        datasets: [{
          data: [
            this.stats.pendingApplications,
            this.stats.approvedApplications,
            this.stats.rejectedApplications
          ],
          backgroundColor: ['#f59e0b', '#10b981', '#ef4444'],
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
              color: '#64748b',
              font: { size: 12 },
              padding: 15,
              usePointStyle: true
            }
          },
          title: {
            display: true,
            text: 'Application Status Distribution',
            color: '#1e293b',
            font: { size: 16, weight: 'bold' }
          }
        }
      }
    };

    this.statusChart = new Chart(ctx, config);
  }

  private createAmountChart(): void {
    const ctx = this.amountChartRef.nativeElement.getContext('2d');
    if (!ctx) return;

    const approvedApps = this.applications.filter(app => app.loanStatus === 'APPROVED');
    const labels = approvedApps.map(app => app.loanType);
    const amounts = approvedApps.map(app => app.loanAmount);

    const config: ChartConfiguration = {
      type: 'bar',
      data: {
        labels: labels.length > 0 ? labels : ['No Data'],
        datasets: [{
          label: 'Loan Amount',
          data: amounts.length > 0 ? amounts : [0],
          backgroundColor: '#3b82f6',
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
            text: 'Approved Loan Amounts',
            color: '#1e293b',
            font: { size: 16, weight: 'bold' }
          }
        },
        scales: {
          x: {
            grid: { color: '#e2e8f0' },
            ticks: { color: '#64748b' }
          },
          y: {
            grid: { color: '#e2e8f0' },
            ticks: { 
              color: '#64748b',
              callback: (value) => '₹' + (value as number).toLocaleString('en-IN')
            },
            beginAtZero: true
          }
        }
      }
    };

    this.amountChart = new Chart(ctx, config);
  }

  private destroyCharts(): void {
    if (this.statusChart) this.statusChart.destroy();
    if (this.amountChart) this.amountChart.destroy();
  }

  formatCurrency(amount: number): string {
    return this.applicantService.formatCurrency(amount);
  }

  formatDate(dateString: string): string {
    return this.applicantService.formatDate(dateString);
  }

  getStatusColor(status: string): string {
    return this.applicantService.getStatusColor(status);
  }

  getFraudStatusColor(status: string): string {
    return this.applicantService.getFraudStatusColor(status);
  }

  viewApplication(loanId: number): void {
    this.router.navigate(['/applicant/applications', loanId]);
  }

  applyForLoan(): void {
    // Check if there are existing drafts
    if (this.draftApplications.length > 0) {
      const resume = confirm(`You have ${this.draftApplications.length} incomplete application(s). Would you like to resume the latest one or start a new application?`);
      if (resume) {
        this.resumeDraft(this.draftApplications[0].id);
        return;
      }
    }
    this.router.navigate(['/applicant/apply-loan']);
  }

  viewAllApplications(): void {
    this.router.navigate(['/applicant/applications']);
  }

  viewProfile(): void {
    this.router.navigate(['/applicant/profile']);
  }

  logout(): void {
    this.authService.logout();
  }

  // Draft Applications Methods
  loadDraftApplications(): void {
    this.draftApplications = this.draftService.getDraftsByApplicant(this.applicantId);
  }

  resumeDraft(draftId: string): void {
    this.router.navigate(['/applicant/apply-loan'], { queryParams: { draftId: draftId } });
  }

  deleteDraft(draftId: string): void {
    if (confirm('Are you sure you want to delete this draft? This action cannot be undone.')) {
      this.draftService.deleteDraft(draftId);
      this.loadDraftApplications();
    }
  }

  getTimeSinceLastSaved(lastSaved: string): string {
    return this.draftService.getTimeSinceLastSaved(lastSaved);
  }

  getStepName(step: number): string {
    return this.draftService.getStepName(step);
  }

  toggleDraftsVisibility(): void {
    this.showDrafts = !this.showDrafts;
  }

  clearAllDrafts(): void {
    if (confirm('Are you sure you want to delete all drafts? This action cannot be undone.')) {
      this.draftService.clearAllDrafts();
      this.loadDraftApplications();
    }
  }

  // Debug methods
  checkLocalStorage(): void {
    const allDrafts = this.draftService.getDrafts();
    console.log('All drafts in localStorage:', allDrafts);
    console.log('Current applicant ID:', this.applicantId);
    console.log('Drafts for this applicant:', this.draftService.getDraftsByApplicant(this.applicantId));
    
    // Show alert with debug info
    alert(`Debug Info:\n` +
          `Total drafts in storage: ${allDrafts.length}\n` +
          `Applicant ID: ${this.applicantId}\n` +
          `Drafts for this applicant: ${this.draftApplications.length}\n` +
          `Check console for detailed logs`);
  }
}
