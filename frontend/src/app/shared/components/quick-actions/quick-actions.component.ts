import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AdminService } from '../../../core/services/admin.service';
import { AuthService } from '../../../core/services/auth.service';

export interface QuickAction {
  id: string;
  icon: string;
  label: string;
  description: string;
  action: () => void;
  color: string;
  badge?: number;
  roles: string[];
  visible: boolean;
}

@Component({
  selector: 'app-quick-actions',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './quick-actions.component.html',
  styleUrls: ['./quick-actions.component.css']
})
export class QuickActionsComponent implements OnInit, OnDestroy {
  isOpen = false;
  actions: QuickAction[] = [];
  currentUserRole = '';
  private subscriptions: Subscription[] = [];

  constructor(
    private router: Router,
    private adminService: AdminService,
    private authService: AuthService
  ) {}

  ngOnInit() {
    this.loadUserRole();
    this.initializeActions();
    this.loadRealTimeData();
  }

  ngOnDestroy() {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  private loadUserRole() {
    // Get current user role from localStorage or token
    const userRole = localStorage.getItem('userRole') || '';
    this.currentUserRole = userRole;
  }

  private initializeActions() {
    this.actions = [
      // Admin Actions
      {
        id: 'new_applicant',
        icon: 'fas fa-user-plus',
        label: 'New Applicant',
        description: 'Add new loan applicant',
        action: () => this.router.navigate(['/admin/applicants/new']),
        color: '#10b981',
        roles: ['ADMIN'],
        visible: true
      },
      {
        id: 'pending_applications',
        icon: 'fas fa-clock',
        label: 'Pending Apps',
        description: 'View pending applications',
        action: () => this.router.navigate(['/admin/loans'], { queryParams: { status: 'PENDING' } }),
        color: '#f59e0b',
        badge: 0, // Will be updated with real data
        roles: ['ADMIN', 'LOAN_OFFICER'],
        visible: true
      },
      {
        id: 'approve_loans',
        icon: 'fas fa-check-circle',
        label: 'Approve Loans',
        description: 'Quick loan approval',
        action: () => this.router.navigate(['/admin/loans'], { queryParams: { status: 'UNDER_REVIEW' } }),
        color: '#10b981',
        badge: 0,
        roles: ['ADMIN', 'LOAN_OFFICER'],
        visible: true
      },
      {
        id: 'rejected_apps',
        icon: 'fas fa-times-circle',
        label: 'Rejected Apps',
        description: 'Review rejected applications',
        action: () => this.router.navigate(['/admin/loans'], { queryParams: { status: 'REJECTED' } }),
        color: '#ef4444',
        badge: 0,
        roles: ['ADMIN'],
        visible: true
      },
      {
        id: 'add_officer',
        icon: 'fas fa-user-tie',
        label: 'Add Officer',
        description: 'Add loan officer',
        action: () => this.router.navigate(['/admin/officers/new']),
        color: '#3b82f6',
        roles: ['ADMIN'],
        visible: true
      },
      {
        id: 'activity_logs',
        icon: 'fas fa-history',
        label: 'Activity Logs',
        description: 'View system activities',
        action: () => this.router.navigate(['/admin/dashboard'], { fragment: 'activity-logs' }),
        color: '#8b5cf6',
        roles: ['ADMIN'],
        visible: true
      },
      {
        id: 'reports',
        icon: 'fas fa-chart-bar',
        label: 'Reports',
        description: 'Generate reports',
        action: () => this.router.navigate(['/admin/dashboard'], { fragment: 'reports' }),
        color: '#06b6d4',
        roles: ['ADMIN'],
        visible: true
      },
      {
        id: 'predictive_analytics',
        icon: 'fas fa-brain',
        label: 'AI Analytics',
        description: 'View predictive insights',
        action: () => this.router.navigate(['/admin/dashboard'], { fragment: 'predictive-analytics' }),
        color: '#ec4899',
        roles: ['ADMIN'],
        visible: true
      },

      // Applicant Actions
      {
        id: 'apply_loan',
        icon: 'fas fa-file-alt',
        label: 'Apply Loan',
        description: 'Submit new loan application',
        action: () => this.router.navigate(['/applicant/apply-loan']),
        color: '#10b981',
        roles: ['APPLICANT'],
        visible: true
      },
      {
        id: 'my_applications',
        icon: 'fas fa-list',
        label: 'My Applications',
        description: 'View my loan applications',
        action: () => this.router.navigate(['/applicant/applications']),
        color: '#3b82f6',
        badge: 0,
        roles: ['APPLICANT'],
        visible: true
      },
      {
        id: 'upload_documents',
        icon: 'fas fa-upload',
        label: 'Upload Docs',
        description: 'Upload required documents',
        action: () => this.router.navigate(['/applicant/documents']),
        color: '#f59e0b',
        roles: ['APPLICANT'],
        visible: true
      },
      {
        id: 'profile_update',
        icon: 'fas fa-user-edit',
        label: 'Update Profile',
        description: 'Update personal information',
        action: () => this.router.navigate(['/applicant/profile']),
        color: '#8b5cf6',
        roles: ['APPLICANT'],
        visible: true
      },

      // Loan Officer Actions
      {
        id: 'assigned_loans',
        icon: 'fas fa-tasks',
        label: 'Assigned Loans',
        description: 'View assigned loan applications',
        action: () => this.router.navigate(['/loan-officer/assignments']),
        color: '#10b981',
        badge: 0,
        roles: ['LOAN_OFFICER'],
        visible: true
      },
      {
        id: 'review_applications',
        icon: 'fas fa-search',
        label: 'Review Apps',
        description: 'Review loan applications',
        action: () => this.router.navigate(['/loan-officer/review']),
        color: '#f59e0b',
        roles: ['LOAN_OFFICER'],
        visible: true
      },

      // Compliance Officer Actions
      {
        id: 'compliance_escalations',
        icon: 'fas fa-exclamation-triangle',
        label: 'Escalations',
        description: 'Handle compliance escalations',
        action: () => this.router.navigate(['/compliance-officer/escalations']),
        color: '#dc2626',
        badge: 0,
        roles: ['COMPLIANCE_OFFICER'],
        visible: true
      }
    ];

    // Filter actions based on user role
    this.actions = this.actions.filter(action => 
      action.roles.includes(this.currentUserRole)
    );
  }

  private loadRealTimeData() {
    if (this.currentUserRole === 'ADMIN' || this.currentUserRole === 'LOAN_OFFICER') {
      // Load real dashboard stats for badges
      const statsSub = this.adminService.getDashboardStats().subscribe({
        next: (stats) => {
          this.updateActionBadges(stats);
        },
        error: (error) => console.error('Failed to load dashboard stats:', error)
      });
      this.subscriptions.push(statsSub);
    }

    // Auto-refresh every 30 seconds
    const refreshInterval = setInterval(() => {
      this.loadRealTimeData();
    }, 30000);

    // Clean up interval on destroy
    this.subscriptions.push({
      unsubscribe: () => clearInterval(refreshInterval)
    } as Subscription);
  }

  private updateActionBadges(stats: any) {
    // Update pending applications badge
    const pendingAction = this.actions.find(a => a.id === 'pending_applications');
    if (pendingAction) {
      pendingAction.badge = stats.pendingApplications || 0;
    }

    // Update approve loans badge (under review)
    const approveAction = this.actions.find(a => a.id === 'approve_loans');
    if (approveAction) {
      // Estimate under review applications (could be enhanced with specific API)
      approveAction.badge = Math.floor((stats.pendingApplications || 0) * 0.6);
    }

    // Update rejected apps badge
    const rejectedAction = this.actions.find(a => a.id === 'rejected_apps');
    if (rejectedAction) {
      rejectedAction.badge = stats.rejectedApplications || 0;
    }

    // For applicant - update my applications badge
    const myAppsAction = this.actions.find(a => a.id === 'my_applications');
    if (myAppsAction && this.currentUserRole === 'APPLICANT') {
      // This would need applicant-specific API call
      myAppsAction.badge = 0; // Placeholder
    }
  }

  toggleMenu() {
    this.isOpen = !this.isOpen;
  }

  executeAction(action: QuickAction) {
    action.action();
    this.isOpen = false; // Close menu after action
  }

  getVisibleActions(): QuickAction[] {
    return this.actions.filter(action => action.visible);
  }

  // Keyboard shortcuts
  onKeydown(event: KeyboardEvent) {
    if (event.ctrlKey && event.key === 'q') {
      event.preventDefault();
      this.toggleMenu();
    }
    if (event.key === 'Escape' && this.isOpen) {
      this.isOpen = false;
    }
  }
}
