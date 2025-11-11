import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { ApplicantService, PreQualificationStatus, LoanApplication, ApplicantProfile, Notification } from '@core/services/applicant.service';
import { DraftService, DraftApplication } from '@core/services/draft.service';

// Document Resubmission Request Interface
interface DocumentResubmissionRequest {
  id: number;
  loanId: number;
  applicationId: string;
  loanType: string;
  documentType: string;
  documentName: string;
  rejectionReason: string;
  requestedAt: string;
  requestedBy: string; // Loan Officer name
  status: 'PENDING' | 'RESUBMITTED' | 'APPROVED';
  dueDate: string;
}
import { Subscription } from 'rxjs';
import { DashboardWidgetComponent } from '../../../shared/components/dashboard-widget/dashboard-widget.component';
import { SkeletonLoaderComponent } from '../../../shared/components/skeleton-loader/skeleton-loader.component';
import { ProgressStepperComponent, Step } from '../../../shared/components/progress-stepper/progress-stepper.component';
import { LoanIdDisplayComponent } from '../../../shared/components/loan-id-display/loan-id-display.component';
import { EnhancedNotificationService } from '../../../core/services/enhanced-notification.service';
import { LoadingService } from '../../../core/services/loading.service';

@Component({
  selector: 'app-enhanced-dashboard',
  standalone: true,
  imports: [
    CommonModule, 
    RouterModule, 
    DashboardWidgetComponent, 
    SkeletonLoaderComponent, 
    ProgressStepperComponent,
    LoanIdDisplayComponent
  ],
  templateUrl: './enhanced-dashboard.component.html',
  styleUrls: ['./enhanced-dashboard.component.css']
})
export class EnhancedDashboardComponent implements OnInit, OnDestroy {
  userName: string = '';
  applicantId: number = 0;
  
  // Profile data
  profile: ApplicantProfile | null = null;
  
  // Pre-qualification status
  preQualification: PreQualificationStatus | null = null;
  
  // Applications
  applications: LoanApplication[] = [];
  activeApplications: LoanApplication[] = [];
  applicationHistory: LoanApplication[] = [];
  
  // Notifications
  notifications: Notification[] = [];
  unreadCount: number = 0;
  
  // Draft Applications
  draftApplications: DraftApplication[] = [];
  showDrafts = true;
  
  // Document Resubmission Requests
  documentResubmissionRequests: DocumentResubmissionRequest[] = [];
  showResubmissionRequests = true;
  
  // Loading states
  loading = {
    profile: true,
    preQualification: true,
    applications: true,
    notifications: true
  };
  
  // Last updated timestamp
  lastUpdated: Date = new Date();
  
  // Application progress steps
  applicationSteps: Step[] = [];
  
  // Subscriptions
  private subscriptions: Subscription[] = [];
  
  constructor(
    private authService: AuthService,
    private applicantService: ApplicantService,
    private draftService: DraftService,
    private router: Router,
    private notificationService: EnhancedNotificationService,
    private loadingService: LoadingService
  ) {
    const user = this.authService.currentUserValue;
    this.userName = user?.firstName && user?.lastName 
      ? `${user.firstName} ${user.lastName}` 
      : user?.username || 'Applicant';
    this.applicantId = user?.id || 0;
  }

  ngOnInit(): void {
    this.initializeApplicationSteps();
    this.loadDashboardData();
    this.loadDraftApplications();
    
    // Show welcome notification for new users
    setTimeout(() => {
      if (this.applications.length === 0) {
        this.notificationService.info(
          'Welcome to FraudShield!',
          'Complete your profile and start your loan application journey.',
          {
            action: {
              label: 'Get Started',
              callback: () => this.router.navigate(['/applicant/apply-loan'])
            }
          }
        );
      }
    }, 2000);
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.loadingService.stopAllLoading();
  }

  loadDashboardData(silent: boolean = false): void {
    if (!silent) {
      this.loading.profile = true;
      this.loading.preQualification = true;
      this.loading.applications = true;
      this.loading.notifications = true;
    }
    
    // Update last updated timestamp
    this.lastUpdated = new Date();
    
    // Load profile
    this.applicantService.getApplicantProfile(this.applicantId).subscribe({
      next: (profile) => {
        this.profile = profile;
        this.loading.profile = false;
        this.calculatePreQualification();
      },
      error: (error) => {
        console.error('Error loading profile:', error);
        this.loading.profile = false;
      }
    });
    
    // Load applications
    this.applicantService.getMyApplications(this.applicantId).subscribe({
      next: (applications) => {
        this.applications = applications;
        this.activeApplications = applications.filter(app => 
          app.loanStatus === 'PENDING' || app.loanStatus === 'UNDER_REVIEW'
        );
        this.applicationHistory = applications.filter(app => 
          app.loanStatus === 'APPROVED' || app.loanStatus === 'REJECTED' || app.loanStatus === 'DISBURSED'
        );
        this.loading.applications = false;
        this.calculatePreQualification();
      },
      error: (error) => {
        console.error('Error loading applications:', error);
        this.loading.applications = false;
      }
    });
    
    // Load notifications (using mock for now)
    this.notifications = this.applicantService.getMockNotifications();
    this.unreadCount = this.notifications.filter(n => !n.isRead).length;
    this.loading.notifications = false;
  }

  calculatePreQualification(): void {
    if (this.profile && this.applications) {
      this.preQualification = this.applicantService.calculatePreQualification(
        this.profile,
        this.applications
      );
      this.loading.preQualification = false;
    }
  }

  getPreQualificationClass(): string {
    if (!this.preQualification) return 'bg-secondary';
    
    switch(this.preQualification.status) {
      case 'ELIGIBLE': return 'bg-success';
      case 'NEEDS_DOCUMENTS': return 'bg-warning';
      case 'PENDING_VERIFICATION': return 'bg-info';
      case 'NOT_ELIGIBLE': return 'bg-danger';
      default: return 'bg-secondary';
    }
  }

  getPreQualificationIcon(): string {
    if (!this.preQualification) return 'fa-question-circle';
    
    switch(this.preQualification.status) {
      case 'ELIGIBLE': return 'fa-check-circle';
      case 'NEEDS_DOCUMENTS': return 'fa-file-upload';
      case 'PENDING_VERIFICATION': return 'fa-clock';
      case 'NOT_ELIGIBLE': return 'fa-times-circle';
      default: return 'fa-question-circle';
    }
  }

  getNotificationIcon(type: string): string {
    switch(type) {
      case 'SUCCESS': return 'fa-check-circle text-success';
      case 'ERROR': return 'fa-exclamation-circle text-danger';
      case 'WARNING': return 'fa-exclamation-triangle text-warning';
      case 'ACTION_REQUIRED': return 'fa-bell text-primary';
      default: return 'fa-info-circle text-info';
    }
  }

  getNotificationPriorityBadge(priority: string): string {
    switch(priority) {
      case 'URGENT': return 'badge bg-danger';
      case 'HIGH': return 'badge bg-warning';
      case 'MEDIUM': return 'badge bg-info';
      default: return 'badge bg-secondary';
    }
  }

  formatCurrency(amount: number): string {
    return this.applicantService.formatCurrency(amount);
  }

  formatDate(dateString: string | undefined): string {
    if (!dateString) return 'N/A';
    return this.applicantService.formatDate(dateString);
  }

  getStatusColor(status: string): string {
    return this.applicantService.getStatusColor(status);
  }


  navigateToLoanTypes(): void {
    this.router.navigate(['/applicant/loan-types']);
  }

  navigateToApplications(): void {
    this.router.navigate(['/applicant/applications']);
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




  handleNotificationAction(notification: Notification): void {
    if (notification.actionUrl) {
      this.router.navigate([notification.actionUrl]);
    }
  }

  markAsRead(notification: Notification): void {
    notification.isRead = true;
    this.unreadCount = this.notifications.filter(n => !n.isRead).length;
  }

  viewApplication(loanId: number): void {
    this.router.navigate(['/applicant/applications', loanId]);
  }

  // Loan Summary Methods
  getTotalAppliedAmount(): number {
    return this.applications.reduce((total, app) => total + app.loanAmount, 0);
  }

  getApprovedAmount(): number {
    return this.applications
      .filter(app => app.loanStatus === 'APPROVED' || app.status === 'approved')
      .reduce((total, app) => total + app.loanAmount, 0);
  }

  getAverageInterestRate(): number {
    const applicationsWithRate = this.applications.filter(app => app.interestRate && app.interestRate > 0);
    if (applicationsWithRate.length === 0) return 8.5; // Default rate
    
    const totalRate = applicationsWithRate.reduce((total, app) => total + (app.interestRate || 0), 0);
    return Math.round((totalRate / applicationsWithRate.length) * 10) / 10;
  }

  getSuccessRate(): number {
    if (this.applications.length === 0) return 0;
    
    const approvedCount = this.applications.filter(app => 
      app.loanStatus === 'APPROVED' || app.status === 'approved' || 
      app.loanStatus === 'DISBURSED' || app.status === 'disbursed'
    ).length;
    
    return Math.round((approvedCount / this.applications.length) * 100);
  }

  // Loan Types Applied
  getLoanTypesApplied(): any[] {
    const loanTypeMap = new Map();
    
    this.applications.forEach(app => {
      const type = app.loanType;
      if (loanTypeMap.has(type)) {
        const existing = loanTypeMap.get(type);
        existing.count += 1;
        existing.totalAmount += app.loanAmount;
      } else {
        loanTypeMap.set(type, {
          type: type,
          count: 1,
          totalAmount: app.loanAmount
        });
      }
    });
    
    return Array.from(loanTypeMap.values());
  }

  getLoanTypeIcon(loanType: string): string {
    const iconMap: { [key: string]: string } = {
      'Personal Loan': 'fa-user',
      'Home Loan': 'fa-home',
      'Car Loan': 'fa-car',
      'Business Loan': 'fa-briefcase',
      'Education Loan': 'fa-graduation-cap',
      'Gold Loan': 'fa-coins'
    };
    return iconMap[loanType] || 'fa-money-bill';
  }

  getLoanTypeIconClass(loanType: string): string {
    const classMap: { [key: string]: string } = {
      'Personal Loan': 'bg-primary',
      'Home Loan': 'bg-success',
      'Car Loan': 'bg-info',
      'Business Loan': 'bg-warning',
      'Education Loan': 'bg-purple',
      'Gold Loan': 'bg-orange'
    };
    return classMap[loanType] || 'bg-secondary';
  }

  // Recent Activity
  getRecentActivity(): any[] {
    const activities: any[] = [];
    
    // Add application activities
    this.applications.slice(0, 5).forEach(app => {
      activities.push({
        type: 'application',
        title: 'Application Submitted',
        description: `${app.loanType} for ${this.formatCurrency(app.loanAmount)}`,
        timestamp: app.submittedAt || app.applicationDate
      });
      
      if (app.reviewedAt) {
        activities.push({
          type: 'review',
          title: 'Application Reviewed',
          description: `${app.loanType} - Status: ${app.loanStatus}`,
          timestamp: app.reviewedAt
        });
      }
    });
    
    // Sort by timestamp (most recent first)
    activities.sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
    
    return activities.slice(0, 5);
  }

  getActivityIcon(activityType: string): string {
    const iconMap: { [key: string]: string } = {
      'application': 'fa-file-alt',
      'review': 'fa-eye',
      'approval': 'fa-check-circle',
      'rejection': 'fa-times-circle',
      'disbursement': 'fa-university'
    };
    return iconMap[activityType] || 'fa-info-circle';
  }

  getActivityIconClass(activityType: string): string {
    const classMap: { [key: string]: string } = {
      'application': 'bg-primary',
      'review': 'bg-info',
      'approval': 'bg-success',
      'rejection': 'bg-danger',
      'disbursement': 'bg-warning'
    };
    return classMap[activityType] || 'bg-secondary';
  }

  getTimeAgo(timestamp: string): string {
    const now = new Date();
    const past = new Date(timestamp);
    const diffMs = now.getTime() - past.getTime();
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffMinutes = Math.floor(diffMs / (1000 * 60));
    
    if (diffDays > 0) {
      return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
    } else if (diffHours > 0) {
      return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
    } else if (diffMinutes > 0) {
      return `${diffMinutes} minute${diffMinutes > 1 ? 's' : ''} ago`;
    } else {
      return 'Just now';
    }
  }

  // Quick Actions
  downloadApplicationReport(): void {
    // Generate CSV report of all applications
    const headers = ['Loan ID', 'Type', 'Amount', 'Status', 'Interest Rate', 'Applied Date'];
    const rows = this.applications.map(app => [
      app.loanId,
      app.loanType,
      app.loanAmount,
      app.loanStatus,
      app.interestRate || 'N/A',
      this.formatDate(app.submittedAt || app.applicationDate)
    ]);
    
    const csvContent = [headers, ...rows].map(row => row.join(',')).join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `loan_applications_${new Date().toISOString().split('T')[0]}.csv`;
    link.click();
    window.URL.revokeObjectURL(url);
  }

  // Initialize application progress steps
  initializeApplicationSteps(): void {
    this.applicationSteps = [
      {
        id: 'profile',
        title: 'Complete Profile',
        description: 'Fill in your personal information',
        icon: 'fa-user',
        status: this.profile?.isEmailVerified ? 'completed' : 'active'
      },
      {
        id: 'documents',
        title: 'Upload Documents',
        description: 'Submit required documents',
        icon: 'fa-file-upload',
        status: 'pending'
      },
      {
        id: 'application',
        title: 'Loan Application',
        description: 'Fill out loan application form',
        icon: 'fa-file-alt',
        status: 'pending'
      },
      {
        id: 'review',
        title: 'Under Review',
        description: 'Application being processed',
        icon: 'fa-search',
        status: 'pending'
      },
      {
        id: 'approval',
        title: 'Approval Decision',
        description: 'Final decision on your application',
        icon: 'fa-check-circle',
        status: 'pending'
      }
    ];

    // Update step statuses based on current applications
    if (this.applications.length > 0) {
      const latestApp = this.applications[0];
      this.updateStepStatusBasedOnApplication(latestApp);
    }
  }

  // Update step status based on application progress
  private updateStepStatusBasedOnApplication(application: LoanApplication): void {
    // Mark profile as completed if we have an application
    this.applicationSteps[0].status = 'completed';
    
    // Update other steps based on application status
    switch (application.loanStatus) {
      case 'DRAFT':
        this.applicationSteps[1].status = 'active';
        break;
      case 'SUBMITTED':
      case 'PENDING':
        this.applicationSteps[1].status = 'completed';
        this.applicationSteps[2].status = 'completed';
        this.applicationSteps[3].status = 'active';
        break;
      case 'UNDER_REVIEW':
        this.applicationSteps[1].status = 'completed';
        this.applicationSteps[2].status = 'completed';
        this.applicationSteps[3].status = 'active';
        break;
      case 'APPROVED':
        this.applicationSteps.forEach((step, index) => {
          if (index < 4) step.status = 'completed';
        });
        this.applicationSteps[4].status = 'completed';
        break;
      case 'REJECTED':
        this.applicationSteps.forEach((step, index) => {
          if (index < 4) step.status = 'completed';
        });
        this.applicationSteps[4].status = 'error';
        break;
    }
  }

  // Handle step click for navigation
  onStepClick(event: { step: Step; index: number }): void {
    const { step } = event;
    
    switch (step.id) {
      case 'profile':
        this.router.navigate(['/applicant/profile']);
        break;
      case 'documents':
        this.router.navigate(['/applicant/documents']);
        break;
      case 'application':
        // Check if there are existing drafts
        if (this.draftApplications.length > 0) {
          const resume = confirm(`You have ${this.draftApplications.length} incomplete application(s). Would you like to resume the latest one or start a new application?`);
          if (resume) {
            this.resumeDraft(this.draftApplications[0].id);
            return;
          }
        }
        this.router.navigate(['/applicant/apply-loan']);
        break;
      case 'review':
        this.router.navigate(['/applicant/applications']);
        break;
    }
  }
}
