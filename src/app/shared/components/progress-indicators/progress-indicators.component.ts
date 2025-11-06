import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription, interval } from 'rxjs';
import { AdminService } from '../../../core/services/admin.service';

export interface ProgressStep {
  id: string;
  label: string;
  description: string;
  status: 'pending' | 'in_progress' | 'completed' | 'failed' | 'skipped';
  timestamp?: Date;
  estimatedDuration?: number; // in minutes
  actualDuration?: number; // in minutes
  progress?: number; // 0-100
  icon?: string;
  color?: string;
}

export interface LoanProgressData {
  loanId: number;
  applicantName: string;
  loanType: string;
  currentStep: string;
  overallProgress: number;
  estimatedCompletion: Date;
  steps: ProgressStep[];
  priority: 'low' | 'normal' | 'high' | 'urgent';
  assignedOfficer?: string;
  lastUpdated: Date;
}

@Component({
  selector: 'app-progress-indicators',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './progress-indicators.component.html',
  styleUrls: ['./progress-indicators.component.css']
})
export class ProgressIndicatorsComponent implements OnInit, OnDestroy {
  @Input() loanId?: number;
  @Input() showMiniView = false;
  @Input() autoRefresh = true;
  @Input() refreshInterval = 30000; // 30 seconds

  progressData: LoanProgressData | null = null;
  isLoading = false;
  error: string | null = null;
  
  private subscriptions: Subscription[] = [];

  // Default progress steps for loan processing
  private defaultSteps: ProgressStep[] = [
    {
      id: 'application_submitted',
      label: 'Application Submitted',
      description: 'Loan application has been submitted',
      status: 'completed',
      icon: 'fas fa-file-alt',
      color: '#10b981',
      estimatedDuration: 0
    },
    {
      id: 'initial_review',
      label: 'Initial Review',
      description: 'Basic eligibility and document check',
      status: 'in_progress',
      icon: 'fas fa-search',
      color: '#3b82f6',
      estimatedDuration: 30
    },
    {
      id: 'document_verification',
      label: 'Document Verification',
      description: 'Verifying submitted documents',
      status: 'pending',
      icon: 'fas fa-file-check',
      color: '#f59e0b',
      estimatedDuration: 60
    },
    {
      id: 'credit_assessment',
      label: 'Credit Assessment',
      description: 'Credit score and financial analysis',
      status: 'pending',
      icon: 'fas fa-chart-line',
      color: '#8b5cf6',
      estimatedDuration: 45
    },
    {
      id: 'risk_evaluation',
      label: 'Risk Evaluation',
      description: 'Risk analysis and fraud detection',
      status: 'pending',
      icon: 'fas fa-shield-alt',
      color: '#ef4444',
      estimatedDuration: 30
    },
    {
      id: 'approval_decision',
      label: 'Approval Decision',
      description: 'Final loan approval or rejection',
      status: 'pending',
      icon: 'fas fa-gavel',
      color: '#10b981',
      estimatedDuration: 15
    },
    {
      id: 'disbursement',
      label: 'Loan Disbursement',
      description: 'Processing loan amount transfer',
      status: 'pending',
      icon: 'fas fa-money-bill-wave',
      color: '#059669',
      estimatedDuration: 30
    }
  ];

  constructor(private adminService: AdminService) {}

  ngOnInit() {
    if (this.loanId) {
      this.loadLoanProgress();
    } else {
      this.loadSystemProgress();
    }

    if (this.autoRefresh) {
      this.startAutoRefresh();
    }
  }

  ngOnDestroy() {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  private loadLoanProgress() {
    if (!this.loanId) return;

    this.isLoading = true;
    this.error = null;

    // Load loan application details
    const loanSub = this.adminService.getLoanApplication(this.loanId).subscribe({
      next: (loan) => {
        this.generateProgressFromLoan(loan);
        this.isLoading = false;
      },
      error: (error) => {
        this.error = 'Failed to load loan progress';
        this.isLoading = false;
        console.error('Failed to load loan progress:', error);
      }
    });

    this.subscriptions.push(loanSub);
  }

  private loadSystemProgress() {
    this.isLoading = true;
    this.error = null;

    // Load dashboard stats to show overall system progress
    const statsSub = this.adminService.getDashboardStats().subscribe({
      next: (stats) => {
        this.generateSystemProgress(stats);
        this.isLoading = false;
      },
      error: (error) => {
        this.error = 'Failed to load system progress';
        this.isLoading = false;
        console.error('Failed to load system progress:', error);
      }
    });

    this.subscriptions.push(statsSub);
  }

  private generateProgressFromLoan(loan: any): void {
    const steps = [...this.defaultSteps];
    const currentStatus = loan.status?.toLowerCase() || 'pending';
    
    // Update steps based on loan status
    this.updateStepsFromStatus(steps, currentStatus);

    // Calculate overall progress
    const completedSteps = steps.filter(s => s.status === 'completed').length;
    const overallProgress = (completedSteps / steps.length) * 100;

    // Estimate completion time
    const remainingSteps = steps.filter(s => s.status === 'pending' || s.status === 'in_progress');
    const estimatedMinutes = remainingSteps.reduce((total, step) => total + (step.estimatedDuration || 0), 0);
    const estimatedCompletion = new Date(Date.now() + estimatedMinutes * 60000);

    this.progressData = {
      loanId: loan.id,
      applicantName: loan.applicantName || 'Unknown Applicant',
      loanType: loan.loanType || 'Personal Loan',
      currentStep: this.getCurrentStepLabel(steps),
      overallProgress: Math.round(overallProgress),
      estimatedCompletion,
      steps,
      priority: this.calculatePriority(loan),
      assignedOfficer: loan.reviewedBy,
      lastUpdated: new Date()
    };
  }

  private generateSystemProgress(stats: any): void {
    // Create system-wide progress indicators
    const totalApplications = stats.totalApplicants || 1;
    const pendingApplications = stats.pendingApplications || 0;
    const approvedLoans = stats.approvedLoans || 0;
    const rejectedApplications = stats.rejectedApplications || 0;

    const systemSteps: ProgressStep[] = [
      {
        id: 'applications_received',
        label: 'Applications Received',
        description: `${totalApplications} total applications`,
        status: 'completed',
        icon: 'fas fa-inbox',
        color: '#10b981',
        progress: 100
      },
      {
        id: 'under_review',
        label: 'Under Review',
        description: `${pendingApplications} applications pending`,
        status: pendingApplications > 0 ? 'in_progress' : 'completed',
        icon: 'fas fa-clock',
        color: '#f59e0b',
        progress: pendingApplications > 0 ? Math.max(10, 100 - (pendingApplications / totalApplications) * 100) : 100
      },
      {
        id: 'approved',
        label: 'Approved',
        description: `${approvedLoans} loans approved`,
        status: approvedLoans > 0 ? 'completed' : 'pending',
        icon: 'fas fa-check-circle',
        color: '#10b981',
        progress: (approvedLoans / totalApplications) * 100
      },
      {
        id: 'processed',
        label: 'Fully Processed',
        description: `${approvedLoans + rejectedApplications} applications processed`,
        status: (approvedLoans + rejectedApplications) === totalApplications ? 'completed' : 'in_progress',
        icon: 'fas fa-tasks',
        color: '#8b5cf6',
        progress: ((approvedLoans + rejectedApplications) / totalApplications) * 100
      }
    ];

    const overallProgress = ((approvedLoans + rejectedApplications) / totalApplications) * 100;

    this.progressData = {
      loanId: 0,
      applicantName: 'System Overview',
      loanType: 'All Loan Types',
      currentStep: pendingApplications > 0 ? 'Processing Applications' : 'All Applications Processed',
      overallProgress: Math.round(overallProgress),
      estimatedCompletion: new Date(Date.now() + pendingApplications * 30 * 60000), // 30 min per application
      steps: systemSteps,
      priority: pendingApplications > 10 ? 'high' : 'normal',
      lastUpdated: new Date()
    };
  }

  private updateStepsFromStatus(steps: ProgressStep[], status: string): void {
    const statusMap: { [key: string]: number } = {
      'pending': 0,
      'under_review': 1,
      'document_verification': 2,
      'credit_check': 3,
      'risk_assessment': 4,
      'approved': 6,
      'rejected': 5,
      'disbursed': 6
    };

    const currentStepIndex = statusMap[status] || 0;

    steps.forEach((step, index) => {
      if (index < currentStepIndex) {
        step.status = 'completed';
        step.timestamp = new Date(Date.now() - (steps.length - index) * 60000); // Mock timestamps
        step.progress = 100;
      } else if (index === currentStepIndex) {
        step.status = 'in_progress';
        step.timestamp = new Date();
        step.progress = Math.random() * 80 + 10; // Random progress between 10-90%
      } else {
        step.status = 'pending';
        step.progress = 0;
      }
    });

    // Handle special cases
    if (status === 'rejected') {
      const approvalStep = steps.find(s => s.id === 'approval_decision');
      if (approvalStep) {
        approvalStep.status = 'failed';
        approvalStep.progress = 100;
      }
      // Skip disbursement
      const disbursementStep = steps.find(s => s.id === 'disbursement');
      if (disbursementStep) {
        disbursementStep.status = 'skipped';
      }
    }
  }

  private getCurrentStepLabel(steps: ProgressStep[]): string {
    const currentStep = steps.find(s => s.status === 'in_progress');
    return currentStep ? currentStep.label : 'Completed';
  }

  private calculatePriority(loan: any): 'low' | 'normal' | 'high' | 'urgent' {
    const appliedDate = new Date(loan.appliedDate || Date.now());
    const daysSinceApplied = (Date.now() - appliedDate.getTime()) / (1000 * 60 * 60 * 24);
    const loanAmount = loan.requestedAmount || 0;

    if (daysSinceApplied > 7) return 'urgent';
    if (daysSinceApplied > 3 || loanAmount > 1000000) return 'high';
    if (loanAmount > 500000) return 'normal';
    return 'low';
  }

  private startAutoRefresh(): void {
    const refreshSub = interval(this.refreshInterval).subscribe(() => {
      if (this.loanId) {
        this.loadLoanProgress();
      } else {
        this.loadSystemProgress();
      }
    });

    this.subscriptions.push(refreshSub);
  }

  // Public methods
  refreshProgress(): void {
    if (this.loanId) {
      this.loadLoanProgress();
    } else {
      this.loadSystemProgress();
    }
  }

  getStepStatusIcon(status: string): string {
    switch (status) {
      case 'completed': return 'fas fa-check-circle';
      case 'in_progress': return 'fas fa-spinner fa-spin';
      case 'failed': return 'fas fa-times-circle';
      case 'skipped': return 'fas fa-forward';
      default: return 'fas fa-circle';
    }
  }

  getStepStatusColor(status: string): string {
    switch (status) {
      case 'completed': return '#10b981';
      case 'in_progress': return '#3b82f6';
      case 'failed': return '#ef4444';
      case 'skipped': return '#6b7280';
      default: return '#d1d5db';
    }
  }

  getPriorityColor(priority: string): string {
    switch (priority) {
      case 'urgent': return '#dc2626';
      case 'high': return '#ea580c';
      case 'normal': return '#059669';
      default: return '#6b7280';
    }
  }

  getPriorityIcon(priority: string): string {
    switch (priority) {
      case 'urgent': return 'fas fa-fire';
      case 'high': return 'fas fa-exclamation-triangle';
      case 'normal': return 'fas fa-clock';
      default: return 'fas fa-info-circle';
    }
  }

  getRelativeTime(date: Date): string {
    const now = new Date();
    const diff = date.getTime() - now.getTime();
    const minutes = Math.floor(Math.abs(diff) / 60000);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);

    if (diff > 0) {
      // Future time
      if (minutes < 60) return `in ${minutes}m`;
      if (hours < 24) return `in ${hours}h`;
      return `in ${days}d`;
    } else {
      // Past time
      if (minutes < 60) return `${minutes}m ago`;
      if (hours < 24) return `${hours}h ago`;
      return `${days}d ago`;
    }
  }

  formatDuration(minutes: number): string {
    if (minutes < 60) return `${minutes}m`;
    const hours = Math.floor(minutes / 60);
    const remainingMinutes = minutes % 60;
    return remainingMinutes > 0 ? `${hours}h ${remainingMinutes}m` : `${hours}h`;
  }

  trackByStepId(index: number, step: ProgressStep): string {
    return step.id;
  }
}
