import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '@core/services/auth.service';

export interface Notification {
  notificationId: number;
  loanId: number;
  assignmentId: number;
  title: string;
  message: string;
  type: 'INFO_REQUEST' | 'DOCUMENT_REQUEST' | 'STATUS_UPDATE' | 'APPROVAL' | 'REJECTION';
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  status: 'UNREAD' | 'READ' | 'RESOLVED';
  requestedBy: string;
  requestedAt: string;
  dueDate?: string;
  requestedDocuments?: string[];
  requestedInfo?: string[];
}

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './notifications.component.html',
  styleUrl: './notifications.component.css'
})
export class NotificationsComponent implements OnInit {
  applicantId: number = 0;
  loading = false;
  error = '';
  successMessage = '';
  
  notifications: Notification[] = [];
  filteredNotifications: Notification[] = [];
  
  // Filters
  statusFilter = '';
  typeFilter = '';
  priorityFilter = '';
  
  statusOptions = ['UNREAD', 'READ', 'RESOLVED'];
  typeOptions = ['INFO_REQUEST', 'DOCUMENT_REQUEST', 'STATUS_UPDATE', 'APPROVAL', 'REJECTION'];
  priorityOptions = ['HIGH', 'MEDIUM', 'LOW'];
  
  // Selected notification for detail view
  selectedNotification: Notification | null = null;
  showDetailModal = false;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {
    const user = this.authService.currentUserValue;
    this.applicantId = user?.applicantId || user?.userId || 0;
  }

  ngOnInit(): void {
    this.loadNotifications();
  }

  loadNotifications(): void {
    this.loading = true;
    this.error = '';
    
    // Mock data - replace with actual API call
    setTimeout(() => {
      this.notifications = [
        {
          notificationId: 1,
          loanId: 1001,
          assignmentId: 501,
          title: 'Additional Information Required',
          message: 'The loan officer has requested additional information for your home loan application.',
          type: 'INFO_REQUEST',
          priority: 'HIGH',
          status: 'UNREAD',
          requestedBy: 'John Doe (Loan Officer)',
          requestedAt: new Date().toISOString(),
          dueDate: new Date(Date.now() + 3 * 24 * 60 * 60 * 1000).toISOString(),
          requestedInfo: ['Employment verification letter', 'Last 3 months bank statements', 'Property valuation report']
        },
        {
          notificationId: 2,
          loanId: 1001,
          assignmentId: 501,
          title: 'Documents Need Resubmission',
          message: 'Some of your submitted documents need to be resubmitted with better quality.',
          type: 'DOCUMENT_REQUEST',
          priority: 'MEDIUM',
          status: 'UNREAD',
          requestedBy: 'John Doe (Loan Officer)',
          requestedAt: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString(),
          requestedDocuments: ['Aadhaar Card (front and back)', 'PAN Card', 'Salary Slips']
        },
        {
          notificationId: 3,
          loanId: 1002,
          assignmentId: 502,
          title: 'Application Status Updated',
          message: 'Your personal loan application status has been updated to "Under Review".',
          type: 'STATUS_UPDATE',
          priority: 'LOW',
          status: 'READ',
          requestedBy: 'System',
          requestedAt: new Date(Date.now() - 5 * 24 * 60 * 60 * 1000).toISOString()
        }
      ];
      
      this.applyFilters();
      this.loading = false;
    }, 1000);
  }

  applyFilters(): void {
    this.filteredNotifications = this.notifications.filter(notif => {
      const matchesStatus = !this.statusFilter || notif.status === this.statusFilter;
      const matchesType = !this.typeFilter || notif.type === this.typeFilter;
      const matchesPriority = !this.priorityFilter || notif.priority === this.priorityFilter;
      
      return matchesStatus && matchesType && matchesPriority;
    });
  }

  clearFilters(): void {
    this.statusFilter = '';
    this.typeFilter = '';
    this.priorityFilter = '';
    this.applyFilters();
  }

  viewNotification(notification: Notification): void {
    this.selectedNotification = notification;
    this.showDetailModal = true;
    
    // Mark as read
    if (notification.status === 'UNREAD') {
      notification.status = 'READ';
      // API call to mark as read
    }
  }

  closeDetailModal(): void {
    this.showDetailModal = false;
    this.selectedNotification = null;
  }

  markAsResolved(notification: Notification): void {
    notification.status = 'RESOLVED';
    this.successMessage = 'Notification marked as resolved';
    setTimeout(() => this.successMessage = '', 3000);
    // API call to update status
  }

  respondToRequest(notification: Notification): void {
    // Navigate to appropriate page based on request type
    if (notification.type === 'DOCUMENT_REQUEST') {
      this.router.navigate(['/applicant/documents'], {
        queryParams: { loanId: notification.loanId }
      });
    } else if (notification.type === 'INFO_REQUEST') {
      this.router.navigate(['/applicant/applications', notification.loanId]);
    }
    this.closeDetailModal();
  }

  getTypeIcon(type: string): string {
    const iconMap: { [key: string]: string } = {
      'INFO_REQUEST': 'fa-info-circle',
      'DOCUMENT_REQUEST': 'fa-file-upload',
      'STATUS_UPDATE': 'fa-sync-alt',
      'APPROVAL': 'fa-check-circle',
      'REJECTION': 'fa-times-circle'
    };
    return iconMap[type] || 'fa-bell';
  }

  getTypeColor(type: string): string {
    const colorMap: { [key: string]: string } = {
      'INFO_REQUEST': 'info',
      'DOCUMENT_REQUEST': 'warning',
      'STATUS_UPDATE': 'primary',
      'APPROVAL': 'success',
      'REJECTION': 'danger'
    };
    return colorMap[type] || 'secondary';
  }

  getPriorityColor(priority: string): string {
    switch (priority) {
      case 'HIGH': return 'danger';
      case 'MEDIUM': return 'warning';
      case 'LOW': return 'info';
      default: return 'secondary';
    }
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'UNREAD': return 'warning';
      case 'READ': return 'info';
      case 'RESOLVED': return 'success';
      default: return 'secondary';
    }
  }

  formatDate(dateString: string): string {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { 
      year: 'numeric', 
      month: 'short', 
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getDaysRemaining(dueDate: string): number {
    if (!dueDate) return 0;
    const due = new Date(dueDate);
    const now = new Date();
    const diff = due.getTime() - now.getTime();
    return Math.ceil(diff / (1000 * 60 * 60 * 24));
  }
}
