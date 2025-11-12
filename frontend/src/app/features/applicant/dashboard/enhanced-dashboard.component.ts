import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { ApplicantService, PreQualificationStatus, LoanApplication, ApplicantProfile, Notification, DocumentResubmissionNotification } from '@core/services/applicant.service';
import { DraftService, DraftApplication } from '@core/services/draft.service';
import { ToastService } from '@core/services/toast.service';

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
    FormsModule,
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
  documentResubmissionRequests: DocumentResubmissionNotification[] = [];
  showResubmissionRequests = true;
  
  // Modal properties
  selectedResubmissionRequest: DocumentResubmissionNotification | null = null;
  selectedFile: File | null = null;
  applicantComments: string = '';
  isUploading: boolean = false;
  
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
    private loadingService: LoadingService,
    private toastService: ToastService
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
    this.loadDocumentResubmissionRequests();
    
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
    
    // Load notifications from database
    this.applicantService.getNotifications(this.applicantId).subscribe({
      next: (notifications) => {
        // Convert DocumentResubmissionNotification to Notification format for general notifications
        this.notifications = notifications.map(notif => ({
          id: notif.notificationId,
          type: notif.type,
          title: notif.title,
          message: notif.message,
          priority: notif.priority,
          isRead: notif.status === 'READ' || notif.status === 'READ'.toUpperCase() || notif.readAt != null,
          createdAt: notif.requestedAt,
          actionUrl: notif.type === 'DOCUMENT_REQUEST' ? '/applicant/documents' : undefined
        }));
        this.unreadCount = this.notifications.filter(n => !n.isRead).length;
        this.loading.notifications = false;
        console.log('Loaded notifications from database:', notifications);
      },
      error: (error) => {
        console.error('Error loading notifications from database:', error);
        this.notifications = [];
        this.unreadCount = 0;
        this.loading.notifications = false;
      }
    });
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

  // Document Resubmission Request Methods
  loadDocumentResubmissionRequests(): void {
    console.log('Loading document resubmission requests for applicant:', this.applicantId);
    
    this.applicantService.getDocumentResubmissionRequests(this.applicantId).subscribe({
      next: (requests) => {
        this.documentResubmissionRequests = requests || [];
        console.log('Loaded document resubmission requests from database:', requests);
        console.log('Number of requests found:', requests?.length || 0);
      },
      error: (error) => {
        console.error('Error loading document resubmission requests from database:', error);
        console.error('Error details:', error.error || error.message);
        // Set empty array instead of mock data - only use real database data
        this.documentResubmissionRequests = [];
      }
    });
  }


  // Modal methods
  openResubmissionModal(request: DocumentResubmissionNotification): void {
    console.log('Opening resubmission modal for request:', request);
    this.selectedResubmissionRequest = request;
    this.selectedFile = null;
    this.applicantComments = '';
    this.isUploading = false;
  }

  closeResubmissionModal(): void {
    // Reset modal state
    this.selectedResubmissionRequest = null;
    this.selectedFile = null;
    this.applicantComments = '';
    this.isUploading = false;
    
    // Close modal using Bootstrap API
    const modal = document.getElementById('resubmissionModal');
    if (modal) {
      const bootstrapModal = (window as any).bootstrap?.Modal?.getInstance(modal);
      if (bootstrapModal) {
        bootstrapModal.hide();
      } else {
        // Fallback: create new modal instance and hide it
        try {
          const newModal = new (window as any).bootstrap.Modal(modal);
          newModal.hide();
        } catch (error) {
          console.error('Error closing modal:', error);
          // Last resort: manually remove modal classes
          modal.classList.remove('show');
          modal.style.display = 'none';
          document.body.classList.remove('modal-open');
          
          // Remove backdrop if it exists
          const backdrop = document.querySelector('.modal-backdrop');
          if (backdrop) {
            backdrop.remove();
          }
        }
      }
    }
  }

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      // Validate file size (10MB limit)
      if (file.size > 10 * 1024 * 1024) {
        alert('File size must be less than 10MB');
        return;
      }
      
      // Validate file type
      const allowedTypes = ['application/pdf', 'image/jpeg', 'image/jpg', 'image/png', 
                           'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'];
      if (!allowedTypes.includes(file.type)) {
        alert('Please select a valid file type (PDF, JPG, PNG, DOC, DOCX)');
        return;
      }
      
      this.selectedFile = file;
      console.log('File selected:', file.name, file.size, file.type);
    }
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  getCleanedMessage(message: string): string {
    // Remove internal details and show only user-friendly message
    if (message.includes('Reason:')) {
      const parts = message.split('Reason:');
      if (parts.length > 1) {
        let reason = parts[1].trim();
        // Remove additional notes part if it contains internal codes
        if (reason.includes('Additional notes:')) {
          const reasonParts = reason.split('Additional notes:');
          const mainReason = reasonParts[0].trim();
          const additionalNotes = reasonParts[1]?.trim() || '';
          
          // Convert internal codes to user-friendly messages
          let friendlyReason = mainReason;
          switch (mainReason) {
            case 'INCOMPLETE_INFO':
              friendlyReason = 'The document appears to be incomplete or missing required information.';
              break;
            case 'MISMATCH_DATA':
              friendlyReason = 'The information in the document does not match your application details.';
              break;
            case 'POOR_QUALITY':
              friendlyReason = 'The document quality is not clear enough for verification.';
              break;
            case 'EXPIRED_DOCUMENT':
              friendlyReason = 'The document has expired and needs to be updated.';
              break;
            default:
              friendlyReason = mainReason;
          }
          
          // Only include additional notes if they seem user-friendly (no random codes)
          if (additionalNotes && additionalNotes.length > 3 && !/^[a-z]{8,}$/.test(additionalNotes)) {
            return `${friendlyReason} Additional details: ${additionalNotes}`;
          }
          return friendlyReason;
        }
        return reason;
      }
    }
    return message;
  }

  submitResubmission(): void {
    if (!this.selectedFile || !this.selectedResubmissionRequest) {
      return;
    }

    this.isUploading = true;
    console.log('Submitting document resubmission...');

    // Create FormData for file upload
    const formData = new FormData();
    formData.append('file', this.selectedFile);
    formData.append('loanId', this.selectedResubmissionRequest.loanId.toString());
    formData.append('documentType', this.selectedResubmissionRequest.requestedDocuments[0]);
    formData.append('notificationId', this.selectedResubmissionRequest.notificationId.toString());
    formData.append('assignmentId', this.selectedResubmissionRequest.assignmentId.toString());
    formData.append('applicantComments', this.applicantComments || '');
    formData.append('isResubmission', 'true');
    formData.append('applicantId', this.applicantId.toString());

    // Store reference for cleanup
    const currentRequest = this.selectedResubmissionRequest;

    // Use the real document resubmission API
    this.applicantService.submitDocumentResubmission(formData).subscribe({
      next: (response) => {
        console.log('Document uploaded successfully:', response);
        
        // Mark notification as resolved (but don't wait for it to complete the UI flow)
        this.applicantService.markNotificationAsResolved(currentRequest.notificationId).subscribe({
          next: () => {
            console.log('Notification marked as resolved');
          },
          error: (error) => {
            console.error('Error marking notification as resolved:', error);
          }
        });

        // Remove from local array immediately for better UX
        this.documentResubmissionRequests = this.documentResubmissionRequests.filter(
          req => req.notificationId !== currentRequest.notificationId
        );
        
        // Force update the UI by triggering change detection
        console.log(`Removed notification ${currentRequest.notificationId} from local array. Remaining requests:`, this.documentResubmissionRequests.length);
        
        // Reset upload state
        this.isUploading = false;
        
        // Close modal immediately
        this.closeResubmissionModal();
        
        // Show success message with toast
        this.toastService.showSuccess(
          '📄 Document Resubmitted Successfully!',
          `Your ${this.getDocumentDisplayName(currentRequest.requestedDocuments)} has been submitted and will be reviewed by your assigned loan officer shortly.`,
          5000
        );

        // Refresh the dashboard data to reflect changes
        setTimeout(() => {
          this.loadDashboardData(true); // Silent refresh
          this.loadDocumentResubmissionRequests();
          
          // Also refresh notifications to ensure they're up to date
          this.applicantService.getNotifications(this.applicantId).subscribe({
            next: (notifications) => {
              this.notifications = notifications.map(notif => ({
                id: notif.notificationId,
                type: notif.type,
                title: notif.title,
                message: notif.message,
                priority: notif.priority,
                isRead: notif.status === 'read' || notif.status === 'READ'.toUpperCase() || notif.readAt != null,
                createdAt: notif.requestedAt,
                actionUrl: notif.type === 'DOCUMENT_REQUEST' ? '/applicant/documents' : undefined
              }));
              this.unreadCount = this.notifications.filter(n => !n.isRead).length;
              console.log('Refreshed notifications after document submission');
            },
            error: (error) => {
              console.error('Error refreshing notifications:', error);
            }
          });
        }, 500); // Reduced timeout for faster UI updates
      },
      error: (error) => {
        console.error('Error uploading document:', error);
        this.isUploading = false;
        this.toastService.showError(
          'Document Upload Failed',
          error.error?.message || 'Failed to upload document. Please try again or contact support.',
          6000
        );
      }
    });
  }

  dismissResubmissionRequest(notificationId: number): void {
    if (confirm('Are you sure you want to dismiss this resubmission request? You can still resubmit the document later.')) {
      this.applicantService.markNotificationAsResolved(notificationId).subscribe({
        next: () => {
          // Remove from local array
          this.documentResubmissionRequests = this.documentResubmissionRequests.filter(req => req.notificationId !== notificationId);
          console.log('Notification dismissed successfully');
        },
        error: (error) => {
          console.error('Error dismissing notification:', error);
          // Still remove from local array for better UX
          this.documentResubmissionRequests = this.documentResubmissionRequests.filter(req => req.notificationId !== notificationId);
        }
      });
    }
  }

  toggleResubmissionRequestsVisibility(): void {
    this.showResubmissionRequests = !this.showResubmissionRequests;
  }

  getDocumentIcon(documentTypes: string[]): string {
    if (!documentTypes || documentTypes.length === 0) return 'fa-file-alt';
    
    const iconMap: { [key: string]: string } = {
      'INCOME_PROOF': 'fa-file-invoice-dollar',
      'IDENTITY_PROOF': 'fa-id-card', 
      'ADDRESS_PROOF': 'fa-home',
      'BANK_STATEMENT': 'fa-university',
      'EMPLOYMENT_PROOF': 'fa-briefcase',
      'AADHAAR': 'fa-id-card',
      'PAN': 'fa-id-card',
      'PASSPORT': 'fa-passport',
      'OTHER': 'fa-file-alt'
    };
    
    // Return icon for first document type
    return iconMap[documentTypes[0]] || 'fa-file-alt';
  }

  getDocumentDisplayName(documentTypes: string[]): string {
    if (!documentTypes || documentTypes.length === 0) return 'Documents';
    
    const nameMap: { [key: string]: string } = {
      'INCOME_PROOF': 'Income Proof',
      'IDENTITY_PROOF': 'Identity Proof',
      'ADDRESS_PROOF': 'Address Proof', 
      'BANK_STATEMENT': 'Bank Statement',
      'EMPLOYMENT_PROOF': 'Employment Proof',
      'AADHAAR': 'Aadhaar Card',
      'PAN': 'PAN Card',
      'PASSPORT': 'Passport'
    };
    
    if (documentTypes.length === 1) {
      return nameMap[documentTypes[0]] || documentTypes[0];
    } else {
      return `${documentTypes.length} Documents`;
    }
  }


  formatDate(dateString: string | undefined): string {
    if (!dateString) return 'N/A';
    try {
      const date = new Date(dateString);
      if (isNaN(date.getTime())) return 'N/A';
      return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
      });
    } catch (error) {
      return 'N/A';
    }
  }

  getApplicationDate(app: any): string {
    // Try multiple date fields to find a valid date
    const dateFields = [
      app.applicationDate,
      app.submittedAt,
      app.createdAt,
      app.appliedDate,
      app.requestedAt
    ];
    
    for (const dateField of dateFields) {
      if (dateField) {
        try {
          const date = new Date(dateField);
          if (!isNaN(date.getTime())) {
            return date.toLocaleDateString('en-US', {
              year: 'numeric',
              month: 'short',
              day: 'numeric'
            });
          }
        } catch (error) {
          continue;
        }
      }
    }
    
    // If no valid date found, return a formatted current date as fallback
    return new Date().toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  getStatusColor(status: string): string {
    return this.applicantService.getStatusColor(status);
  }

  // Helper methods for Math operations in template
  getAbsoluteDays(days: number): number {
    return Math.abs(days);
  }

  getProgressBarWidth(dueDate: string): number {
    const daysRemaining = this.getDaysRemaining(dueDate);
    return Math.max(10, Math.min(100, (7 - daysRemaining) / 7 * 100));
  }

  getResubmissionStatusColor(status: string): string {
    switch(status?.toLowerCase()) {
      case 'unread':
      case 'pending':
        return 'warning';
      case 'read':
        return 'info';
      case 'resolved':
        return 'success';
      default:
        return 'secondary';
    }
  }

  // Document resubmission helper methods
  formatResubmissionDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  getDaysRemaining(dueDate: string): number {
    const due = new Date(dueDate);
    const now = new Date();
    const diffTime = due.getTime() - now.getTime();
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  }

  isOverdue(dueDate: string): boolean {
    return this.getDaysRemaining(dueDate) < 0;
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
      'PERSONAL': 'fa-user',
      'Home Loan': 'fa-home',
      'HOME': 'fa-home',
      'Car Loan': 'fa-car',
      'VEHICLE': 'fa-car',
      'Business Loan': 'fa-briefcase',
      'BUSINESS': 'fa-briefcase',
      'Education Loan': 'fa-graduation-cap',
      'EDUCATION': 'fa-graduation-cap',
      'Gold Loan': 'fa-coins',
      'GOLD': 'fa-coins'
    };
    return iconMap[loanType] || 'fa-money-bill-wave';
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

  // Get tenure display with fallback
  getTenureDisplay(app: LoanApplication): string {
    const tenure = app.tenureMonths || app.loanTenure || 12; // Default to 12 months if not available
    return tenure.toString();
  }

  // Generate dummy loan ID in LNID format
  generateDummyLoanId(loanId: number): string {
    // Generate a consistent dummy ID based on the actual loan ID
    const baseNumber = 100000 + (loanId % 900000); // Ensures 6-digit number
    return `LNID${baseNumber.toString().padStart(6, '0')}`;
  }

  // Get interest rate for display
  getInterestRate(app: LoanApplication): string {
    return (app.interestRate || this.getDefaultInterestRate(app.loanType)).toFixed(1);
  }

  // Get default interest rate based on loan type
  private getDefaultInterestRate(loanType: string): number {
    const rates: { [key: string]: number } = {
      'PERSONAL': 14.5,
      'HOME': 9.5,
      'VEHICLE': 11.5,
      'EDUCATION': 11.0,
      'BUSINESS': 13.5,
      'GOLD': 10.5
    };
    return rates[loanType] || 12.0;
  }

  // Download application PDF
  downloadApplicationPDF(loanId: number): void {
    // Implementation for PDF download
    console.log('Downloading PDF for loan:', loanId);
    // You can implement the actual PDF download logic here
  }
}
