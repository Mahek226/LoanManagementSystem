import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { ApplicantService, PreQualificationStatus, LoanApplication, ApplicantProfile, Notification } from '@core/services/applicant.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-enhanced-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
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
  
  // Loading states
  loading = {
    profile: true,
    preQualification: true,
    applications: true,
    notifications: true
  };
  
  // Last updated timestamp
  lastUpdated: Date = new Date();
  
  constructor(
    private authService: AuthService,
    private applicantService: ApplicantService,
    private router: Router
  ) {
    const user = this.authService.currentUserValue;
    this.userName = user?.firstName && user?.lastName 
      ? `${user.firstName} ${user.lastName}` 
      : user?.username || 'Applicant';
    this.applicantId = user?.id || 0;
  }

  ngOnInit(): void {
    this.loadDashboardData();
  }

  ngOnDestroy(): void {
    // Cleanup if needed
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
}
