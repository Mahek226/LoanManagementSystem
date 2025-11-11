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
        <h1>Compliance Officer Dashboard</h1>
        <div class="header-actions">
          <button (click)="refreshData()" class="btn btn-outline-primary me-2" [disabled]="isLoading">
            <i class="fas fa-sync-alt" [class.fa-spin]="isLoading"></i> Refresh
          </button>
          <button (click)="logout()" class="btn btn-secondary">Logout</button>
        </div>
      </div>

      <!-- Dashboard Stats -->
      <div class="stats-grid" *ngIf="stats">
        <div class="stat-card">
          <div class="stat-value">{{ stats.totalEscalations }}</div>
          <div class="stat-label">Total Escalations</div>
        </div>
        <div class="stat-card pending">
          <div class="stat-value">{{ stats.pendingReview }}</div>
          <div class="stat-label">Pending Review</div>
        </div>
        <div class="stat-card approved">
          <div class="stat-value">{{ stats.approved }}</div>
          <div class="stat-label">Approved</div>
        </div>
        <div class="stat-card rejected">
          <div class="stat-value">{{ stats.rejected }}</div>
          <div class="stat-label">Rejected</div>
        </div>
        <div class="stat-card high-risk">
          <div class="stat-value">{{ stats.highRisk }}</div>
          <div class="stat-label">High Risk</div>
        </div>
        <div class="stat-card critical-risk">
          <div class="stat-value">{{ stats.criticalRisk }}</div>
          <div class="stat-label">Critical Risk</div>
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
    }
    
    .dashboard-header h1 {
      font-size: 2rem;
      font-weight: 700;
      color: #1a202c;
      margin: 0;
    }

    .header-actions {
      display: flex;
      align-items: center;
    }
    
    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 1rem;
      margin-bottom: 2rem;
    }
    
    .stat-card {
      background: white;
      border-radius: 8px;
      padding: 1.5rem;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
      text-align: center;
      border-left: 4px solid #e2e8f0;
    }
    
    .stat-card.pending { border-left-color: #f59e0b; }
    .stat-card.approved { border-left-color: #10b981; }
    .stat-card.rejected { border-left-color: #ef4444; }
    .stat-card.high-risk { border-left-color: #f59e0b; }
    .stat-card.critical-risk { border-left-color: #dc2626; }
    
    .stat-value {
      font-size: 2rem;
      font-weight: 700;
      color: #1a202c;
      margin-bottom: 0.5rem;
    }
    
    .stat-label {
      font-size: 0.875rem;
      color: #64748b;
      text-transform: uppercase;
      letter-spacing: 0.05em;
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
}
