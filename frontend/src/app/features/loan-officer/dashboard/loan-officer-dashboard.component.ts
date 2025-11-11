import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { LoanOfficerService, LoanScreeningResponse } from '@core/services/loan-officer.service';

@Component({
  selector: 'app-loan-officer-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="dashboard-container">
      <div class="dashboard-header">
        <h1>Loan Officer Dashboard</h1>
        <div class="header-actions">
          <button (click)="refreshLoans()" class="btn btn-primary me-2">
            <i class="fas fa-sync-alt"></i> Refresh
          </button>
          <button (click)="logout()" class="btn btn-secondary">Logout</button>
        </div>
      </div>

      <div class="dashboard-content">
        <!-- Quick Stats -->
        <div class="stats-row mb-4">
          <div class="stat-card">
            <div class="stat-number">{{ totalLoans }}</div>
            <div class="stat-label">Total Assigned</div>
          </div>
          <div class="stat-card">
            <div class="stat-number">{{ pendingLoans }}</div>
            <div class="stat-label">Pending Review</div>
          </div>
          <div class="stat-card">
            <div class="stat-number">{{ escalatedLoans }}</div>
            <div class="stat-label">Escalated</div>
          </div>
          <div class="stat-card">
            <div class="stat-number">{{ loansWithVerdict }}</div>
            <div class="stat-label">Compliance Verdicts</div>
          </div>
        </div>

        <!-- Loans requiring action after compliance -->
        <div class="section" *ngIf="loansRequiringAction.length > 0">
          <h3 class="section-title">
            <i class="fas fa-exclamation-triangle text-warning"></i>
            Loans Requiring Action After Compliance Review
          </h3>
          <div class="loans-grid">
            <div *ngFor="let loan of loansRequiringAction" class="loan-card compliance-verdict-card">
              <div class="loan-header">
                <h4>{{ loan.applicantName }}</h4>
                <span class="badge" [ngClass]="'badge-' + getStatusColor(loan.status)">
                  {{ loan.status }}
                </span>
              </div>
              
              <div class="loan-details">
                <div class="detail-row">
                  <span class="label">Loan Amount:</span>
                  <span class="value">{{ formatCurrency(loan.loanAmount) }}</span>
                </div>
                <div class="detail-row">
                  <span class="label">Loan Type:</span>
                  <span class="value">{{ loan.loanType }}</span>
                </div>
                <div class="detail-row">
                  <span class="label">Risk Level:</span>
                  <span class="badge" [ngClass]="'badge-' + getRiskColor(loan.riskLevel)">
                    {{ loan.riskLevel }}
                  </span>
                </div>
              </div>

              <!-- Compliance Verdict Section -->
              <div class="compliance-verdict" *ngIf="loan.hasComplianceVerdict">
                <h5 class="verdict-title">
                  <i class="fas fa-gavel"></i> Compliance Verdict
                </h5>
                <div class="verdict-details">
                  <div class="verdict-row">
                    <span class="verdict-label">Decision:</span>
                    <span class="badge" [ngClass]="getComplianceVerdictColor(loan.complianceVerdict!)">
                      {{ loan.complianceVerdict }}
                    </span>
                  </div>
                  <div class="verdict-row" *ngIf="loan.complianceVerdictReason">
                    <span class="verdict-label">Reason:</span>
                    <span class="verdict-value">{{ loan.complianceVerdictReason }}</span>
                  </div>
                  <div class="verdict-row" *ngIf="loan.complianceOfficerName">
                    <span class="verdict-label">Officer:</span>
                    <span class="verdict-value">{{ loan.complianceOfficerName }}</span>
                  </div>
                  <div class="verdict-row" *ngIf="loan.complianceVerdictTimestamp">
                    <span class="verdict-label">Date:</span>
                    <span class="verdict-value">{{ formatDate(loan.complianceVerdictTimestamp) }}</span>
                  </div>
                  <div class="next-action" *ngIf="loan.nextAction">
                    <strong>Next Action:</strong> {{ loan.nextAction }}
                  </div>
                  <div class="compliance-remarks" *ngIf="loan.complianceRemarks">
                    <strong>Compliance Remarks:</strong>
                    <p>{{ loan.complianceRemarks }}</p>
                  </div>
                </div>
              </div>

              <!-- Action Buttons -->
              <div class="loan-actions">
                <button 
                  (click)="processAfterCompliance(loan, 'APPROVE')" 
                  class="btn btn-success btn-sm me-2"
                  [disabled]="processingLoan === loan.assignmentId">
                  <i class="fas fa-check"></i> Final Approve
                </button>
                <button 
                  (click)="processAfterCompliance(loan, 'REJECT')" 
                  class="btn btn-danger btn-sm me-2"
                  [disabled]="processingLoan === loan.assignmentId">
                  <i class="fas fa-times"></i> Final Reject
                </button>
                <button 
                  (click)="viewLoanDetails(loan)" 
                  class="btn btn-info btn-sm">
                  <i class="fas fa-eye"></i> View Details
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- All Assigned Loans -->
        <div class="section">
          <h3 class="section-title">
            <i class="fas fa-list"></i>
            All Assigned Loans
          </h3>
          
          <!-- Filter Controls -->
          <div class="filters mb-3">
            <select [(ngModel)]="statusFilter" (change)="applyFilters()" class="form-select me-2">
              <option value="">All Statuses</option>
              <option value="PENDING">Pending</option>
              <option value="IN_PROGRESS">In Progress</option>
              <option value="ESCALATED_TO_COMPLIANCE">Escalated</option>
              <option value="APPROVED">Approved</option>
              <option value="REJECTED">Rejected</option>
            </select>
            <select [(ngModel)]="riskFilter" (change)="applyFilters()" class="form-select">
              <option value="">All Risk Levels</option>
              <option value="LOW">Low Risk</option>
              <option value="MEDIUM">Medium Risk</option>
              <option value="HIGH">High Risk</option>
            </select>
          </div>

          <div class="loans-grid">
            <div *ngFor="let loan of filteredLoans" class="loan-card">
              <div class="loan-header">
                <h4>{{ loan.applicantName }}</h4>
                <span class="badge" [ngClass]="'badge-' + getStatusColor(loan.status)">
                  {{ loan.status }}
                </span>
              </div>
              
              <div class="loan-details">
                <div class="detail-row">
                  <span class="label">Loan Amount:</span>
                  <span class="value">{{ formatCurrency(loan.loanAmount) }}</span>
                </div>
                <div class="detail-row">
                  <span class="label">Loan Type:</span>
                  <span class="value">{{ loan.loanType }}</span>
                </div>
                <div class="detail-row">
                  <span class="label">Risk Level:</span>
                  <span class="badge" [ngClass]="'badge-' + getRiskColor(loan.riskLevel)">
                    {{ loan.riskLevel }}
                  </span>
                </div>
                <div class="detail-row">
                  <span class="label">Assigned:</span>
                  <span class="value">{{ formatDate(loan.assignedAt) }}</span>
                </div>
              </div>

              <!-- Show compliance status if escalated -->
              <div class="compliance-status" *ngIf="loan.status === 'ESCALATED_TO_COMPLIANCE'">
                <div *ngIf="loan.hasComplianceVerdict" class="verdict-available">
                  <i class="fas fa-check-circle text-success"></i>
                  <span class="text-success">Compliance verdict available</span>
                </div>
                <div *ngIf="!loan.hasComplianceVerdict" class="verdict-pending">
                  <i class="fas fa-clock text-warning"></i>
                  <span class="text-warning">Awaiting compliance review</span>
                </div>
              </div>

              <div class="loan-actions">
                <button 
                  (click)="viewLoanDetails(loan)" 
                  class="btn btn-info btn-sm me-2">
                  <i class="fas fa-eye"></i> View
                </button>
                <button 
                  *ngIf="loan.status === 'PENDING' || loan.status === 'IN_PROGRESS'" 
                  (click)="processLoan(loan)" 
                  class="btn btn-primary btn-sm me-2">
                  <i class="fas fa-cog"></i> Process
                </button>
                <button 
                  *ngIf="loan.hasComplianceVerdict && loan.status === 'ESCALATED_TO_COMPLIANCE'" 
                  (click)="showComplianceVerdict(loan)" 
                  class="btn btn-success btn-sm">
                  <i class="fas fa-gavel"></i> View Verdict
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Loading Overlay -->
    <div *ngIf="loading" class="loading-overlay">
      <div class="spinner"></div>
      <p>Loading loans...</p>
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
      border-radius: 8px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }
    
    .dashboard-header h1 {
      font-size: 2rem;
      font-weight: 700;
      color: #2c3e50;
      margin: 0;
    }
    
    .header-actions {
      display: flex;
      gap: 0.5rem;
    }
    
    .stats-row {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 1rem;
      margin-bottom: 2rem;
    }
    
    .stat-card {
      background: white;
      padding: 1.5rem;
      border-radius: 8px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
      text-align: center;
    }
    
    .stat-number {
      font-size: 2rem;
      font-weight: bold;
      color: #3498db;
      margin-bottom: 0.5rem;
    }
    
    .stat-label {
      color: #7f8c8d;
      font-size: 0.9rem;
    }
    
    .section {
      background: white;
      border-radius: 8px;
      padding: 1.5rem;
      margin-bottom: 2rem;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }
    
    .section-title {
      font-size: 1.3rem;
      font-weight: 600;
      color: #2c3e50;
      margin-bottom: 1.5rem;
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }
    
    .filters {
      display: flex;
      gap: 1rem;
    }
    
    .loans-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
      gap: 1.5rem;
    }
    
    .loan-card {
      border: 1px solid #e9ecef;
      border-radius: 8px;
      padding: 1.5rem;
      background: #f8f9fa;
      transition: all 0.3s ease;
    }
    
    .loan-card:hover {
      box-shadow: 0 4px 8px rgba(0,0,0,0.15);
      transform: translateY(-2px);
    }
    
    .compliance-verdict-card {
      border-left: 4px solid #f39c12;
      background: #fff9e6;
    }
    
    .loan-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1rem;
    }
    
    .loan-header h4 {
      margin: 0;
      color: #2c3e50;
      font-size: 1.1rem;
    }
    
    .loan-details {
      margin-bottom: 1rem;
    }
    
    .detail-row {
      display: flex;
      justify-content: space-between;
      margin-bottom: 0.5rem;
    }
    
    .label {
      font-weight: 500;
      color: #7f8c8d;
    }
    
    .value {
      color: #2c3e50;
      font-weight: 500;
    }
    
    .compliance-verdict {
      background: #e8f5e8;
      border: 1px solid #c3e6c3;
      border-radius: 6px;
      padding: 1rem;
      margin: 1rem 0;
    }
    
    .verdict-title {
      font-size: 1rem;
      font-weight: 600;
      color: #27ae60;
      margin-bottom: 0.75rem;
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }
    
    .verdict-details {
      font-size: 0.9rem;
    }
    
    .verdict-row {
      display: flex;
      justify-content: space-between;
      margin-bottom: 0.5rem;
    }
    
    .verdict-label {
      font-weight: 500;
      color: #2c3e50;
    }
    
    .verdict-value {
      color: #34495e;
    }
    
    .next-action {
      background: #fff3cd;
      border: 1px solid #ffeaa7;
      border-radius: 4px;
      padding: 0.5rem;
      margin: 0.75rem 0;
      font-size: 0.9rem;
      color: #856404;
    }
    
    .compliance-remarks {
      background: #f8f9fa;
      border-left: 3px solid #6c757d;
      padding: 0.75rem;
      margin-top: 0.75rem;
      font-size: 0.9rem;
    }
    
    .compliance-remarks p {
      margin: 0.5rem 0 0 0;
      color: #495057;
    }
    
    .compliance-status {
      padding: 0.5rem;
      margin: 0.5rem 0;
      border-radius: 4px;
      font-size: 0.9rem;
    }
    
    .verdict-available {
      background: #d4edda;
      color: #155724;
    }
    
    .verdict-pending {
      background: #fff3cd;
      color: #856404;
    }
    
    .loan-actions {
      display: flex;
      gap: 0.5rem;
      flex-wrap: wrap;
      margin-top: 1rem;
    }
    
    .badge {
      padding: 0.25rem 0.5rem;
      border-radius: 4px;
      font-size: 0.75rem;
      font-weight: 600;
      text-transform: uppercase;
    }
    
    .badge-success { background: #d4edda; color: #155724; }
    .badge-danger { background: #f8d7da; color: #721c24; }
    .badge-warning { background: #fff3cd; color: #856404; }
    .badge-info { background: #d1ecf1; color: #0c5460; }
    .badge-secondary { background: #e2e3e5; color: #383d41; }
    .badge-primary { background: #cce7ff; color: #004085; }
    
    .loading-overlay {
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: rgba(0,0,0,0.5);
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      z-index: 9999;
      color: white;
    }
    
    .spinner {
      border: 4px solid #f3f3f3;
      border-top: 4px solid #3498db;
      border-radius: 50%;
      width: 40px;
      height: 40px;
      animation: spin 1s linear infinite;
      margin-bottom: 1rem;
    }
    
    @keyframes spin {
      0% { transform: rotate(0deg); }
      100% { transform: rotate(360deg); }
    }
    
    .btn {
      padding: 0.5rem 1rem;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 0.9rem;
      font-weight: 500;
      text-decoration: none;
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      transition: all 0.3s ease;
    }
    
    .btn:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
    
    .btn-primary { background: #3498db; color: white; }
    .btn-primary:hover:not(:disabled) { background: #2980b9; }
    
    .btn-success { background: #27ae60; color: white; }
    .btn-success:hover:not(:disabled) { background: #229954; }
    
    .btn-danger { background: #e74c3c; color: white; }
    .btn-danger:hover:not(:disabled) { background: #c0392b; }
    
    .btn-info { background: #17a2b8; color: white; }
    .btn-info:hover:not(:disabled) { background: #138496; }
    
    .btn-secondary { background: #6c757d; color: white; }
    .btn-secondary:hover:not(:disabled) { background: #5a6268; }
    
    .btn-sm {
      padding: 0.375rem 0.75rem;
      font-size: 0.8rem;
    }
    
    .form-select {
      padding: 0.5rem;
      border: 1px solid #ced4da;
      border-radius: 4px;
      background: white;
      min-width: 150px;
    }
    
    .me-2 { margin-right: 0.5rem; }
    .mb-3 { margin-bottom: 1rem; }
    .mb-4 { margin-bottom: 1.5rem; }
    
    .text-success { color: #28a745; }
    .text-warning { color: #ffc107; }
    .text-danger { color: #dc3545; }
    .text-info { color: #17a2b8; }
  `]
})
export class LoanOfficerDashboardComponent implements OnInit {
  userName: string;
  loading = false;
  processingLoan: number | null = null;
  
  // Data properties
  allLoans: LoanScreeningResponse[] = [];
  filteredLoans: LoanScreeningResponse[] = [];
  loansRequiringAction: LoanScreeningResponse[] = [];
  
  // Filter properties
  statusFilter = '';
  riskFilter = '';
  
  // Stats
  totalLoans = 0;
  pendingLoans = 0;
  escalatedLoans = 0;
  loansWithVerdict = 0;

  constructor(
    private authService: AuthService,
    private router: Router,
    private loanOfficerService: LoanOfficerService
  ) {
    const user = this.authService.currentUserValue;
    this.userName = user?.username || (user?.firstName && user?.lastName ? `${user.firstName} ${user.lastName}` : 'Loan Officer');
  }

  ngOnInit(): void {
    this.loadLoans();
  }

  loadLoans(): void {
    this.loading = true;
    const user = this.authService.currentUserValue;
    const officerId = user?.id;

    if (!officerId) {
      console.error('Officer ID not found');
      this.loading = false;
      return;
    }

    this.loanOfficerService.getAssignedLoans(officerId).subscribe({
      next: (loans) => {
        this.allLoans = loans;
        this.filteredLoans = [...loans];
        this.updateStats();
        this.identifyLoansRequiringAction();
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading loans:', error);
        this.loading = false;
      }
    });
  }

  updateStats(): void {
    this.totalLoans = this.allLoans.length;
    this.pendingLoans = this.allLoans.filter(l => 
      l.status === 'PENDING' || l.status === 'IN_PROGRESS' || l.status === 'ASSIGNED'
    ).length;
    this.escalatedLoans = this.allLoans.filter(l => 
      l.status === 'ESCALATED_TO_COMPLIANCE'
    ).length;
    this.loansWithVerdict = this.allLoans.filter(l => 
      l.hasComplianceVerdict === true
    ).length;
  }

  identifyLoansRequiringAction(): void {
    this.loansRequiringAction = this.allLoans.filter(loan => 
      loan.status === 'ESCALATED_TO_COMPLIANCE' && loan.hasComplianceVerdict === true
    );
  }

  applyFilters(): void {
    this.filteredLoans = this.allLoans.filter(loan => {
      const statusMatch = !this.statusFilter || loan.status === this.statusFilter;
      const riskMatch = !this.riskFilter || loan.riskLevel === this.riskFilter;
      return statusMatch && riskMatch;
    });
  }

  refreshLoans(): void {
    this.loadLoans();
  }

  processAfterCompliance(loan: LoanScreeningResponse, decision: string): void {
    const remarks = prompt(`Enter remarks for ${decision.toLowerCase()}ing this loan after compliance review:`);
    if (remarks === null) return; // User cancelled

    this.processingLoan = loan.assignmentId;
    const user = this.authService.currentUserValue;
    const officerId = user?.id;

    if (!officerId) {
      console.error('Officer ID not found');
      this.processingLoan = null;
      return;
    }

    const request = {
      loanId: loan.loanId,
      assignmentId: loan.assignmentId,
      decision: decision,
      remarks: remarks || ''
    };

    this.loanOfficerService.processLoanAfterCompliance(officerId, request).subscribe({
      next: (response) => {
        console.log('Loan processed successfully:', response);
        this.processingLoan = null;
        this.refreshLoans(); // Reload to get updated status
        alert(`Loan ${decision.toLowerCase()}ed successfully!`);
      },
      error: (error) => {
        console.error('Error processing loan:', error);
        this.processingLoan = null;
        alert(`Error processing loan: ${error.error?.message || error.message}`);
      }
    });
  }

  processLoan(loan: LoanScreeningResponse): void {
    // Navigate to loan processing page or open modal
    this.router.navigate(['/loan-officer/process', loan.assignmentId]);
  }

  viewLoanDetails(loan: LoanScreeningResponse): void {
    // Navigate to loan details page
    this.router.navigate(['/loan-officer/loan', loan.loanId]);
  }

  showComplianceVerdict(loan: LoanScreeningResponse): void {
    // Show detailed compliance verdict in modal or navigate to details
    alert(`Compliance Verdict: ${loan.complianceVerdict}\nReason: ${loan.complianceVerdictReason}\nNext Action: ${loan.nextAction}`);
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

  getComplianceVerdictColor(verdict: string): string {
    switch (verdict?.toUpperCase()) {
      case 'APPROVED': return 'badge-success';
      case 'REJECTED': return 'badge-danger';
      case 'FLAGGED': return 'badge-warning';
      case 'CONDITIONAL_APPROVAL': return 'badge-info';
      default: return 'badge-secondary';
    }
  }

  logout(): void {
    this.authService.logout();
  }
}
