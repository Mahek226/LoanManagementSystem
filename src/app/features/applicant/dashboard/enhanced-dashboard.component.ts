import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { ApplicantService, PreQualificationStatus, LoanApplication, ApplicantProfile, Notification } from '@core/services/applicant.service';
import { interval, Subscription } from 'rxjs';

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
  
  // Auto-refresh subscription
  private refreshSubscription?: Subscription;
  
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
    
    // Auto-refresh every 30 seconds
    this.refreshSubscription = interval(30000).subscribe(() => {
      this.loadDashboardData(true);
    });
  }

  ngOnDestroy(): void {
    if (this.refreshSubscription) {
      this.refreshSubscription.unsubscribe();
    }
  }

  loadDashboardData(silent: boolean = false): void {
    if (!silent) {
      this.loading.profile = true;
      this.loading.preQualification = true;
      this.loading.applications = true;
      this.loading.notifications = true;
    }
    
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

  getTimeAgo(timestamp: string): string {
    const now = new Date().getTime();
    const time = new Date(timestamp).getTime();
    const diff = now - time;
    
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);
    
    if (minutes < 60) return `${minutes}m ago`;
    if (hours < 24) return `${hours}h ago`;
    return `${days}d ago`;
  }

  navigateToLoanTypes(): void {
    this.router.navigate(['/applicant/loan-types']);
  }

  navigateToApplications(): void {
    this.router.navigate(['/applicant/applications']);
  }

  navigateToProfile(): void {
    this.router.navigate(['/applicant/profile']);
  }

  navigateToDocuments(): void {
    this.router.navigate(['/applicant/documents']);
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
}
