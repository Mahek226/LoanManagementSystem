import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { ComplianceOfficerService, ComplianceEscalation, DashboardStats } from '@core/services/compliance-officer.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-compliance-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="dashboard-container">
      <div class="dashboard-header">
        <div class="header-left">
          <h1>Compliance Officer Dashboard</h1>
          <p class="dashboard-subtitle">{{ userName }} • {{ getCurrentDate() }}</p>
        </div>
        <div class="header-actions">
          <select class="form-select me-2" [(ngModel)]="timeFilter" (change)="onTimeFilterChange()" style="width: auto;">
            <option value="today">Today</option>
            <option value="week">This Week</option>
            <option value="month">This Month</option>
            <option value="quarter">This Quarter</option>
            <option value="all">All Time</option>
          </select>
          <button (click)="refreshData()" class="btn btn-outline-primary me-2" [disabled]="isLoading">
            <i class="fas fa-sync-alt" [class.fa-spin]="isLoading"></i> Refresh
          </button>
          <button (click)="logout()" class="btn btn-secondary">Logout</button>
        </div>
      </div>

      <!-- KPI Dashboard -->
      <div class="kpi-dashboard" *ngIf="stats">
        <!-- Primary KPIs -->
        <div class="kpi-section">
          <h3 class="kpi-section-title">
            <i class="fas fa-tachometer-alt me-2"></i>Key Performance Indicators
          </h3>
          <div class="kpi-grid primary">
            <div class="kpi-card primary">
              <div class="kpi-header">
                <i class="fas fa-clipboard-list"></i>
                <span class="kpi-trend positive" *ngIf="stats.todayReviewed! > 0">+{{ stats.todayReviewed }}</span>
              </div>
              <div class="kpi-value">{{ stats.totalEscalations }}</div>
              <div class="kpi-label">Total Escalations</div>
              <div class="kpi-subtitle">{{ stats.pendingReview }} pending review</div>
            </div>
            
            <div class="kpi-card warning">
              <div class="kpi-header">
                <i class="fas fa-clock"></i>
                <span class="kpi-trend negative" *ngIf="stats.overdueReviews! > 0">{{ stats.overdueReviews }} overdue</span>
              </div>
              <div class="kpi-value">{{ stats.avgDecisionTime }}h</div>
              <div class="kpi-label">Avg Decision Time</div>
              <div class="kpi-subtitle">Target: ≤48 hours</div>
            </div>
            
            <div class="kpi-card success">
              <div class="kpi-header">
                <i class="fas fa-check-circle"></i>
                <span class="kpi-trend positive">{{ stats.approvalRate }}%</span>
              </div>
              <div class="kpi-value">{{ stats.approved }}</div>
              <div class="kpi-label">Approved Cases</div>
              <div class="kpi-subtitle">{{ stats.reviewsCompletedOnTime }} on-time</div>
            </div>
            
            <div class="kpi-card danger">
              <div class="kpi-header">
                <i class="fas fa-times-circle"></i>
                <span class="kpi-trend">{{ stats.rejectionRate }}%</span>
              </div>
              <div class="kpi-value">{{ stats.rejected }}</div>
              <div class="kpi-label">Rejected Cases</div>
              <div class="kpi-subtitle">Quality control</div>
            </div>
          </div>
        </div>

        <!-- Performance Metrics -->
        <div class="kpi-section">
          <h3 class="kpi-section-title">
            <i class="fas fa-chart-line me-2"></i>Performance Metrics
          </h3>
          <div class="kpi-grid secondary">
            <div class="kpi-card info">
              <div class="kpi-header">
                <i class="fas fa-calendar-day"></i>
              </div>
              <div class="kpi-value">{{ stats.todayReviewed }}</div>
              <div class="kpi-label">Today's Reviews</div>
              <div class="kpi-subtitle">{{ stats.avgDailyReviews }}/day avg</div>
            </div>
            
            <div class="kpi-card info">
              <div class="kpi-header">
                <i class="fas fa-calendar-week"></i>
              </div>
              <div class="kpi-value">{{ stats.weeklyReviewed }}</div>
              <div class="kpi-label">This Week</div>
              <div class="kpi-subtitle">7-day total</div>
            </div>
            
            <div class="kpi-card info">
              <div class="kpi-header">
                <i class="fas fa-calendar-alt"></i>
              </div>
              <div class="kpi-value">{{ stats.monthlyReviewed }}</div>
              <div class="kpi-label">This Month</div>
              <div class="kpi-subtitle">30-day total</div>
            </div>
            
            <div class="kpi-card warning">
              <div class="kpi-header">
                <i class="fas fa-exclamation-triangle"></i>
              </div>
              <div class="kpi-value">{{ stats.avgRiskScore }}</div>
              <div class="kpi-label">Avg Risk Score</div>
              <div class="kpi-subtitle">{{ stats.highRisk + stats.criticalRisk }} high-risk</div>
            </div>
          </div>
        </div>

        <!-- Risk & Quality Metrics -->
        <div class="kpi-section">
          <h3 class="kpi-section-title">
            <i class="fas fa-shield-alt me-2"></i>Risk & Quality Analysis
          </h3>
          <div class="kpi-grid tertiary">
            <div class="kpi-card danger">
              <div class="kpi-header">
                <i class="fas fa-user-secret"></i>
              </div>
              <div class="kpi-value">{{ stats.fraudDetectionRate }}%</div>
              <div class="kpi-label">Fraud Detection</div>
              <div class="kpi-subtitle">Risk identification</div>
            </div>
            
            <div class="kpi-card success">
              <div class="kpi-header">
                <i class="fas fa-bullseye"></i>
              </div>
              <div class="kpi-value">{{ stats.escalationAccuracy }}%</div>
              <div class="kpi-label">Accuracy Rate</div>
              <div class="kpi-subtitle">Decision quality</div>
            </div>
            
            <div class="kpi-card warning">
              <div class="kpi-header">
                <i class="fas fa-file-alt"></i>
              </div>
              <div class="kpi-value">{{ stats.documentResubmissionRate }}%</div>
              <div class="kpi-label">Doc Resubmission</div>
              <div class="kpi-subtitle">Quality indicator</div>
            </div>
            
            <div class="kpi-card info">
              <div class="kpi-header">
                <i class="fas fa-coins"></i>
              </div>
              <div class="kpi-value">{{ stats.highValueLoansReviewed }}</div>
              <div class="kpi-label">High-Value Loans</div>
              <div class="kpi-subtitle">>₹10L reviewed</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Loading State -->
      <div *ngIf="isLoading" class="loading-container">
        <div class="spinner-border text-primary" role="status">
          <span class="visually-hidden">Loading...</span>
        </div>
        <p class="mt-2">Loading escalations...</p>
      </div>

      <!-- Error State -->
      <div *ngIf="error" class="alert alert-danger">
        <i class="fas fa-exclamation-triangle me-2"></i>
        {{ error }}
      </div>

      <!-- Escalations List -->
      <div class="escalations-section" *ngIf="!isLoading && !error">
        <div class="section-header">
          <h3>Loan Escalations</h3>
          <div class="filters">
            <select class="form-select" [(ngModel)]="statusFilter" (change)="applyFilters()">
              <option value="">All Statuses</option>
              <option value="PENDING">Pending</option>
              <option value="ESCALATED_TO_COMPLIANCE">Escalated</option>
              <option value="APPROVED">Approved</option>
              <option value="REJECTED">Rejected</option>
            </select>
            <select class="form-select ms-2" [(ngModel)]="riskFilter" (change)="applyFilters()">
              <option value="">All Risk Levels</option>
              <option value="LOW">Low Risk</option>
              <option value="MEDIUM">Medium Risk</option>
              <option value="HIGH">High Risk</option>
              <option value="CRITICAL">Critical Risk</option>
            </select>
          </div>
        </div>

        <div class="escalations-grid" *ngIf="filteredEscalations.length > 0">
          <div class="escalation-card" *ngFor="let escalation of filteredEscalations" 
               [class.pending]="escalation.status === 'PENDING' || escalation.status === 'ESCALATED_TO_COMPLIANCE'"
               [class.approved]="escalation.status === 'APPROVED'"
               [class.rejected]="escalation.status === 'REJECTED'">
            <div class="card-header">
              <div class="loan-info">
                <h5>Loan #{{ escalation.loanId }}</h5>
                <p class="applicant-name">{{ escalation.applicantName }}</p>
              </div>
              <div class="badges">
                <span [class]="complianceService.getStatusBadgeClass(escalation.status)">{{ escalation.status }}</span>
                <span [class]="complianceService.getRiskBadgeClass(escalation.riskLevel)">{{ escalation.riskLevel }}</span>
              </div>
            </div>
            <div class="card-body">
              <div class="loan-details">
                <div class="detail-item">
                  <span class="label">Loan Type:</span>
                  <span class="value">{{ escalation.loanType }}</span>
                </div>
                <div class="detail-item">
                  <span class="label">Amount:</span>
                  <span class="value">{{ complianceService.formatCurrency(escalation.loanAmount) }}</span>
                </div>
                <div class="detail-item">
                  <span class="label">Risk Score:</span>
                  <span class="value">{{ escalation.riskScore }}</span>
                </div>
                <div class="detail-item">
                  <span class="label">Assigned:</span>
                  <span class="value">{{ complianceService.formatDate(escalation.assignedAt) }}</span>
                </div>
                <div class="detail-item" *ngIf="escalation.processedAt">
                  <span class="label">Processed:</span>
                  <span class="value">{{ complianceService.formatDate(escalation.processedAt) }}</span>
                </div>
              </div>
              <div class="card-actions" *ngIf="escalation.status === 'PENDING' || escalation.status === 'ESCALATED_TO_COMPLIANCE'">
                <button class="btn btn-primary btn-sm" (click)="reviewEscalation(escalation)">
                  <i class="fas fa-eye me-1"></i> Review
                </button>
              </div>
              <div class="card-actions" *ngIf="escalation.status === 'APPROVED' || escalation.status === 'REJECTED'">
                <button class="btn btn-outline-secondary btn-sm" (click)="viewDetails(escalation)">
                  <i class="fas fa-info-circle me-1"></i> View Details
                </button>
              </div>
            </div>
          </div>
        </div>

        <div class="no-data" *ngIf="filteredEscalations.length === 0">
          <i class="fas fa-inbox fa-3x text-muted mb-3"></i>
          <h4>No escalations found</h4>
          <p class="text-muted">{{ statusFilter || riskFilter ? 'Try adjusting your filters' : 'No loan escalations have been assigned yet' }}</p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .dashboard-container {
      min-height: 100vh;
      background-color: #f8f9fa;
      padding: 2rem;
    }
    
    .dashboard-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 2rem;
      background: white;
      padding: 1.5rem;
      border-radius: 12px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }
    
    .header-left h1 {
      font-size: 2rem;
      font-weight: 700;
      color: #1a202c;
      margin: 0;
    }

    .dashboard-subtitle {
      font-size: 0.875rem;
      color: #64748b;
      margin: 0.25rem 0 0 0;
    }

    .header-actions {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }
    
    .kpi-dashboard {
      margin-bottom: 2rem;
    }
    
    .kpi-section {
      margin-bottom: 2rem;
    }
    
    .kpi-section-title {
      font-size: 1.25rem;
      font-weight: 600;
      color: #1a202c;
      margin-bottom: 1rem;
      display: flex;
      align-items: center;
    }
    
    .kpi-grid {
      display: grid;
      gap: 1rem;
    }
    
    .kpi-grid.primary {
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    }
    
    .kpi-grid.secondary,
    .kpi-grid.tertiary {
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    }
    
    .kpi-card {
      background: white;
      border-radius: 12px;
      padding: 1.5rem;
      box-shadow: 0 4px 6px rgba(0, 0, 0, 0.07);
      border: 1px solid #f1f5f9;
      transition: all 0.3s ease;
      position: relative;
      overflow: hidden;
    }
    
    .kpi-card:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
    }
    
    .kpi-card::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 4px;
      background: linear-gradient(90deg, #e2e8f0, #cbd5e1);
    }
    
    .kpi-card.primary::before { background: linear-gradient(90deg, #3b82f6, #1d4ed8); }
    .kpi-card.success::before { background: linear-gradient(90deg, #10b981, #059669); }
    .kpi-card.warning::before { background: linear-gradient(90deg, #f59e0b, #d97706); }
    .kpi-card.danger::before { background: linear-gradient(90deg, #ef4444, #dc2626); }
    .kpi-card.info::before { background: linear-gradient(90deg, #06b6d4, #0891b2); }
    
    .kpi-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1rem;
    }
    
    .kpi-header i {
      font-size: 1.5rem;
      color: #64748b;
    }
    
    .kpi-card.primary .kpi-header i { color: #3b82f6; }
    .kpi-card.success .kpi-header i { color: #10b981; }
    .kpi-card.warning .kpi-header i { color: #f59e0b; }
    .kpi-card.danger .kpi-header i { color: #ef4444; }
    .kpi-card.info .kpi-header i { color: #06b6d4; }
    
    .kpi-trend {
      font-size: 0.75rem;
      font-weight: 600;
      padding: 0.25rem 0.5rem;
      border-radius: 12px;
      background: #f1f5f9;
      color: #64748b;
    }
    
    .kpi-trend.positive {
      background: #dcfce7;
      color: #166534;
    }
    
    .kpi-trend.negative {
      background: #fef2f2;
      color: #991b1b;
    }
    
    .kpi-value {
      font-size: 2.5rem;
      font-weight: 700;
      color: #1a202c;
      margin-bottom: 0.5rem;
      line-height: 1;
    }
    
    .kpi-label {
      font-size: 0.875rem;
      font-weight: 600;
      color: #374151;
      margin-bottom: 0.25rem;
    }
    
    .kpi-subtitle {
      font-size: 0.75rem;
      color: #64748b;
      font-weight: 500;
    }
    
    .loading-container {
      text-align: center;
      padding: 3rem;
    }
    
    .escalations-section {
      background: white;
      border-radius: 8px;
      padding: 1.5rem;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }
    
    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1.5rem;
      flex-wrap: wrap;
      gap: 1rem;
    }
    
    .section-header h3 {
      margin: 0;
      color: #1a202c;
    }
    
    .filters {
      display: flex;
      gap: 0.5rem;
    }
    
    .escalations-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
      gap: 1rem;
    }
    
    .escalation-card {
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      background: white;
      transition: all 0.2s;
      border-left: 4px solid #e2e8f0;
    }
    
    .escalation-card:hover {
      box-shadow: 0 4px 12px rgba(0,0,0,0.1);
      transform: translateY(-2px);
    }
    
    .escalation-card.pending { border-left-color: #f59e0b; }
    .escalation-card.approved { border-left-color: #10b981; }
    .escalation-card.rejected { border-left-color: #ef4444; }
    
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      padding: 1rem 1rem 0.5rem;
    }
    
    .loan-info h5 {
      margin: 0 0 0.25rem;
      color: #1a202c;
      font-weight: 600;
    }
    
    .applicant-name {
      margin: 0;
      color: #64748b;
      font-size: 0.875rem;
    }
    
    .badges {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
      align-items: flex-end;
    }
    
    .card-body {
      padding: 0.5rem 1rem 1rem;
    }
    
    .loan-details {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 0.5rem;
      margin-bottom: 1rem;
    }
    
    .detail-item {
      display: flex;
      justify-content: space-between;
      font-size: 0.875rem;
    }
    
    .detail-item .label {
      color: #64748b;
      font-weight: 500;
    }
    
    .detail-item .value {
      color: #1a202c;
      font-weight: 600;
    }
    
    .card-actions {
      display: flex;
      gap: 0.5rem;
      justify-content: flex-end;
    }
    
    .no-data {
      text-align: center;
      padding: 3rem;
      color: #64748b;
    }
    
    .badge {
      font-size: 0.75rem;
      padding: 0.25rem 0.5rem;
      border-radius: 4px;
      font-weight: 500;
    }
    
    .bg-warning { background-color: #fbbf24 !important; color: #92400e; }
    .bg-success { background-color: #34d399 !important; color: #065f46; }
    .bg-danger { background-color: #f87171 !important; color: #991b1b; }
    .bg-info { background-color: #60a5fa !important; color: #1e40af; }
    .bg-secondary { background-color: #9ca3af !important; color: #374151; }
    
    @media (max-width: 768px) {
      .dashboard-container {
        padding: 1rem;
      }
      
      .dashboard-header {
        flex-direction: column;
        align-items: stretch;
        gap: 1rem;
        padding: 1rem;
      }
      
      .header-left h1 {
        font-size: 1.5rem;
      }
      
      .header-actions {
        justify-content: center;
        flex-wrap: wrap;
      }
      
      .kpi-grid.primary {
        grid-template-columns: 1fr;
      }
      
      .kpi-grid.secondary,
      .kpi-grid.tertiary {
        grid-template-columns: repeat(2, 1fr);
      }
      
      .kpi-card {
        padding: 1rem;
      }
      
      .kpi-value {
        font-size: 2rem;
      }
      
      .escalations-grid {
        grid-template-columns: 1fr;
      }
      
      .section-header {
        flex-direction: column;
        align-items: stretch;
      }
      
      .filters {
        flex-direction: column;
      }
    }
    
    @media (max-width: 480px) {
      .kpi-grid.secondary,
      .kpi-grid.tertiary {
        grid-template-columns: 1fr;
      }
      
      .kpi-section-title {
        font-size: 1rem;
      }
      
      .kpi-value {
        font-size: 1.75rem;
      }
    }
  `]
})
export class ComplianceDashboardComponent implements OnInit, OnDestroy {
  userName: string;
  escalations: ComplianceEscalation[] = [];
  filteredEscalations: ComplianceEscalation[] = [];
  stats: DashboardStats | null = null;
  isLoading = false;
  error: string | null = null;
  statusFilter = '';
  riskFilter = '';
  timeFilter = 'all';
  private subscription = new Subscription();

  constructor(
    private authService: AuthService,
    private router: Router,
    public complianceService: ComplianceOfficerService
  ) {
    const user = this.authService.currentUserValue;
    this.userName = user?.username || (user?.firstName && user?.lastName ? `${user.firstName} ${user.lastName}` : 'Compliance Officer');
  }

  ngOnInit(): void {
    this.loadEscalations();
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  loadEscalations(): void {
    this.isLoading = true;
    this.error = null;

    const sub = this.complianceService.getEscalations().subscribe({
      next: (escalations) => {
        this.escalations = escalations;
        this.filteredEscalations = [...escalations];
        this.stats = this.complianceService.calculateStats(escalations);
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading escalations:', error);
        this.error = 'Failed to load escalations. Please try again.';
        this.isLoading = false;
      }
    });

    this.subscription.add(sub);
  }

  refreshData(): void {
    this.loadEscalations();
  }

  applyFilters(): void {
    this.filteredEscalations = this.escalations.filter(escalation => {
      const statusMatch = !this.statusFilter || escalation.status === this.statusFilter;
      const riskMatch = !this.riskFilter || escalation.riskLevel === this.riskFilter;
      return statusMatch && riskMatch;
    });
  }

  reviewEscalation(escalation: ComplianceEscalation): void {
    // Navigate to detailed review page
    this.router.navigate(['/compliance-officer/review', escalation.assignmentId]);
  }

  viewDetails(escalation: ComplianceEscalation): void {
    // Navigate to view-only details page
    this.router.navigate(['/compliance-officer/details', escalation.assignmentId]);
  }

  logout(): void {
    this.authService.logout();
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
    // Filter escalations based on time period
    this.applyTimeFilter();
    // Recalculate stats with filtered data
    this.stats = this.complianceService.calculateStats(this.filteredEscalations);
  }

  private applyTimeFilter(): void {
    const now = new Date();
    let startDate: Date;

    switch (this.timeFilter) {
      case 'today':
        startDate = new Date(now.getFullYear(), now.getMonth(), now.getDate());
        break;
      case 'week':
        startDate = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
        break;
      case 'month':
        startDate = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);
        break;
      case 'quarter':
        startDate = new Date(now.getTime() - 90 * 24 * 60 * 60 * 1000);
        break;
      default:
        // 'all' - no filtering
        this.filteredEscalations = [...this.escalations];
        this.applyFilters(); // Apply status and risk filters
        return;
    }

    // Filter escalations by time period
    this.filteredEscalations = this.escalations.filter(escalation => {
      const assignedDate = new Date(escalation.assignedAt);
      return assignedDate >= startDate;
    });

    // Apply additional filters
    this.applyFilters();
  }
}
