import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, interval, Subscription, of } from 'rxjs';
import { switchMap, catchError, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface Notification {
  notificationId: number;
  userId: number;
  userType: string; // LOAN_OFFICER, COMPLIANCE_OFFICER, APPLICANT
  title: string;
  message: string;
  type: string; // NEW_APPLICATION, DOCUMENT_UPLOADED, FRAUD_CHECK_COMPLETE, ESCALATION, APPROVAL, REJECTION
  relatedEntityType?: string; // LOAN, DOCUMENT, APPLICATION
  relatedEntityId?: number;
  isRead: boolean;
  createdAt: string;
  readAt?: string;
}

export interface NotificationStats {
  unreadCount: number;
  totalCount: number;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private apiUrl = `${environment.apiUrl}/notifications`;
  
  // Observable streams
  private notificationsSubject = new BehaviorSubject<Notification[]>([]);
  private unreadCountSubject = new BehaviorSubject<number>(0);
  
  public notifications$ = this.notificationsSubject.asObservable();
  public unreadCount$ = this.unreadCountSubject.asObservable();
  
  private pollingInterval = 30000; // Poll every 30 seconds
  private pollingSubscription: any;

  constructor(private http: HttpClient) {}

  /**
   * Start polling for new notifications
   */
  startPolling(userId: number): void {
    // Initial load
    this.loadNotifications(userId);
    
    // Poll every 30 seconds
    this.pollingSubscription = interval(this.pollingInterval)
      .pipe(
        switchMap(() => this.getNotifications(userId))
      )
      .subscribe({
        next: (notifications) => {
          this.notificationsSubject.next(notifications);
          this.updateUnreadCount(notifications);
        },
        error: (err) => console.error('Error polling notifications:', err)
      });
  }

  /**
   * Stop polling
   */
  stopPolling(): void {
    if (this.pollingSubscription) {
      this.pollingSubscription.unsubscribe();
    }
  }

  /**
   * Load notifications for a user
   */
  loadNotifications(userId: number): void {
    this.getNotifications(userId).subscribe({
      next: (notifications) => {
        this.notificationsSubject.next(notifications);
        this.updateUnreadCount(notifications);
      },
      error: (err) => console.error('Error loading notifications:', err)
    });
  }

  /**
   * Get all notifications for a user
   */
  getNotifications(userId: number): Observable<Notification[]> {
    return this.http.get<Notification[]>(`${this.apiUrl}/user/${userId}`).pipe(
      catchError((error: any) => {
        console.warn('Notifications endpoint not available, using mock data');
        return of(this.getMockNotifications(userId));
      })
    );
  }

  /**
   * Get unread notifications
   */
  getUnreadNotifications(userId: number): Observable<Notification[]> {
    return this.http.get<Notification[]>(`${this.apiUrl}/user/${userId}/unread`);
  }

  /**
   * Get notification statistics
   */
  getNotificationStats(userId: number): Observable<NotificationStats> {
    return this.http.get<NotificationStats>(`${this.apiUrl}/user/${userId}/stats`);
  }

  /**
   * Mark notification as read
   */
  markAsRead(notificationId: number): Observable<Notification> {
    return this.http.put<Notification>(`${this.apiUrl}/${notificationId}/read`, {})
      .pipe(
        tap(() => {
          // Update local state
          const current = this.notificationsSubject.value;
          const updated = current.map(n => 
            n.notificationId === notificationId ? { ...n, isRead: true, readAt: new Date().toISOString() } : n
          );
          this.notificationsSubject.next(updated);
          this.updateUnreadCount(updated);
        })
      );
  }

  /**
   * Mark all notifications as read
   */
  markAllAsRead(userId: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/user/${userId}/read-all`, {})
      .pipe(
        tap(() => {
          // Update local state
          const current = this.notificationsSubject.value;
          const updated = current.map(n => ({ ...n, isRead: true, readAt: new Date().toISOString() }));
          this.notificationsSubject.next(updated);
          this.updateUnreadCount(updated);
        })
      );
  }

  /**
   * Delete a notification
   */
  deleteNotification(notificationId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${notificationId}`)
      .pipe(
        tap(() => {
          // Update local state
          const current = this.notificationsSubject.value;
          const updated = current.filter(n => n.notificationId !== notificationId);
          this.notificationsSubject.next(updated);
          this.updateUnreadCount(updated);
        })
      );
  }

  /**
   * Create a notification (admin/system use)
   */
  createNotification(notification: Partial<Notification>): Observable<Notification> {
    return this.http.post<Notification>(`${this.apiUrl}`, notification);
  }

  /**
   * Update unread count
   */
  private updateUnreadCount(notifications: Notification[]): void {
    const unreadCount = notifications.filter(n => !n.isRead).length;
    this.unreadCountSubject.next(unreadCount);
  }

  /**
   * Generate mock notifications when API is not available
   */
  private getMockNotifications(userId: number): Notification[] {
    // Get user role from localStorage or determine based on current route
    const userRole = this.getCurrentUserRole();
    
    if (userRole === 'LOAN_OFFICER') {
      return this.getLoanOfficerMockNotifications(userId);
    } else if (userRole === 'COMPLIANCE_OFFICER') {
      return this.getComplianceOfficerMockNotifications(userId);
    } else if (userRole === 'APPLICANT') {
      return this.getApplicantMockNotifications(userId);
    }
    
    return this.getLoanOfficerMockNotifications(userId); // Default fallback
  }

  /**
   * Get current user role
   */
  private getCurrentUserRole(): string {
    // Try to get from current URL
    const currentUrl = window.location.pathname;
    if (currentUrl.includes('/loan-officer')) return 'LOAN_OFFICER';
    if (currentUrl.includes('/compliance-officer')) return 'COMPLIANCE_OFFICER';
    if (currentUrl.includes('/applicant')) return 'APPLICANT';
    
    // Try to get from localStorage
    const user = JSON.parse(localStorage.getItem('currentUser') || '{}');
    return user.role || 'LOAN_OFFICER';
  }

  /**
   * Generate loan officer specific notifications
   */
  private getLoanOfficerMockNotifications(userId: number): Notification[] {
    return [
      {
        notificationId: 1,
        userId: userId,
        userType: 'LOAN_OFFICER',
        title: 'New Escalation Assigned',
        message: 'A new loan application has been escalated for compliance review',
        type: 'ESCALATION',
        relatedEntityType: 'LOAN',
        relatedEntityId: 2,
        isRead: false,
        createdAt: new Date(Date.now() - 1000 * 60 * 30).toISOString(), // 30 minutes ago
        readAt: undefined
      },
      {
        notificationId: 2,
        userId: userId,
        userType: 'LOAN_OFFICER',
        title: 'Document Verification Complete',
        message: 'Document verification has been completed for loan application #1511',
        type: 'DOCUMENT_UPLOADED',
        relatedEntityType: 'DOCUMENT',
        relatedEntityId: 1511,
        isRead: true,
        createdAt: new Date(Date.now() - 1000 * 60 * 60 * 2).toISOString(), // 2 hours ago
        readAt: new Date(Date.now() - 1000 * 60 * 45).toISOString()
      },
      {
        notificationId: 3,
        userId: userId,
        userType: 'LOAN_OFFICER',
        title: 'Fraud Check Alert',
        message: 'High risk fraud indicators detected in application #1510',
        type: 'FRAUD_CHECK_COMPLETE',
        relatedEntityType: 'LOAN',
        relatedEntityId: 1510,
        isRead: false,
        createdAt: new Date(Date.now() - 1000 * 60 * 60 * 4).toISOString(), // 4 hours ago
        readAt: undefined
      },
      {
        notificationId: 4,
        userId: userId,
        userType: 'LOAN_OFFICER',
        title: 'New Application Assigned',
        message: 'A new loan application #1515 has been assigned to you for review',
        type: 'NEW_APPLICATION',
        relatedEntityType: 'LOAN',
        relatedEntityId: 1515,
        isRead: false,
        createdAt: new Date(Date.now() - 1000 * 60 * 60 * 6).toISOString(), // 6 hours ago
        readAt: undefined
      },
      {
        notificationId: 5,
        userId: userId,
        userType: 'LOAN_OFFICER',
        title: 'Document Resubmission Request',
        message: 'Applicant has resubmitted documents for loan application #1512',
        type: 'DOCUMENT_REQUEST',
        relatedEntityType: 'LOAN',
        relatedEntityId: 1512,
        isRead: true,
        createdAt: new Date(Date.now() - 1000 * 60 * 60 * 8).toISOString(), // 8 hours ago
        readAt: new Date(Date.now() - 1000 * 60 * 60 * 7).toISOString()
      }
    ];
  }

  /**
   * Generate compliance officer specific notifications
   */
  private getComplianceOfficerMockNotifications(userId: number): Notification[] {
    return [
      {
        notificationId: 1,
        userId: userId,
        userType: 'COMPLIANCE_OFFICER',
        title: 'New Escalation Assigned',
        message: 'A new loan application has been escalated for compliance review',
        type: 'ESCALATION',
        relatedEntityType: 'LOAN',
        relatedEntityId: 1512,
        isRead: false,
        createdAt: new Date(Date.now() - 1000 * 60 * 30).toISOString(), // 30 minutes ago
        readAt: undefined
      },
      {
        notificationId: 2,
        userId: userId,
        userType: 'COMPLIANCE_OFFICER',
        title: 'Document Verification Complete',
        message: 'Document verification has been completed for loan application #1511',
        type: 'DOCUMENT_UPLOADED',
        relatedEntityType: 'DOCUMENT',
        relatedEntityId: 2234,
        isRead: true,
        createdAt: new Date(Date.now() - 1000 * 60 * 60 * 2).toISOString(), // 2 hours ago
        readAt: new Date(Date.now() - 1000 * 60 * 45).toISOString()
      },
      {
        notificationId: 3,
        userId: userId,
        userType: 'COMPLIANCE_OFFICER',
        title: 'Fraud Check Alert',
        message: 'High risk fraud indicators detected in application #1510',
        type: 'FRAUD_CHECK_COMPLETE',
        relatedEntityType: 'LOAN',
        relatedEntityId: 1510,
        isRead: false,
        createdAt: new Date(Date.now() - 1000 * 60 * 60 * 4).toISOString(), // 4 hours ago
        readAt: undefined
      }
    ];
  }

  /**
   * Generate applicant specific notifications
   */
  private getApplicantMockNotifications(userId: number): Notification[] {
    return [
      {
        notificationId: 1,
        userId: userId,
        userType: 'APPLICANT',
        title: 'Application Status Update',
        message: 'Your loan application #1234 is now under review',
        type: 'STATUS_UPDATE',
        relatedEntityType: 'LOAN',
        relatedEntityId: 1234,
        isRead: false,
        createdAt: new Date(Date.now() - 1000 * 60 * 60).toISOString(), // 1 hour ago
        readAt: undefined
      },
      {
        notificationId: 2,
        userId: userId,
        userType: 'APPLICANT',
        title: 'Document Upload Required',
        message: 'Please upload additional documents for your loan application',
        type: 'DOCUMENT_REQUEST',
        relatedEntityType: 'LOAN',
        relatedEntityId: 1234,
        isRead: true,
        createdAt: new Date(Date.now() - 1000 * 60 * 60 * 24).toISOString(), // 1 day ago
        readAt: new Date(Date.now() - 1000 * 60 * 60 * 20).toISOString()
      }
    ];
  }

  /**
   * Get notification icon based on type
   */
  getNotificationIcon(type: string): string {
    const icons: { [key: string]: string } = {
      'NEW_APPLICATION': 'fas fa-file-plus',
      'DOCUMENT_UPLOADED': 'fas fa-cloud-upload-alt',
      'FRAUD_CHECK_COMPLETE': 'fas fa-shield-alt',
      'ESCALATION': 'fas fa-exclamation-triangle',
      'APPROVAL': 'fas fa-check-circle',
      'REJECTION': 'fas fa-times-circle',
      'DOCUMENT_REQUEST': 'fas fa-file-alt',
      'STATUS_UPDATE': 'fas fa-bell'
    };
    return icons[type] || 'fas fa-bell';
  }

  /**
   * Get notification color based on type
   */
  getNotificationColor(type: string): string {
    const colors: { [key: string]: string } = {
      'NEW_APPLICATION': 'primary',
      'DOCUMENT_UPLOADED': 'info',
      'FRAUD_CHECK_COMPLETE': 'warning',
      'ESCALATION': 'danger',
      'APPROVAL': 'success',
      'REJECTION': 'danger',
      'DOCUMENT_REQUEST': 'warning',
      'STATUS_UPDATE': 'secondary'
    };
    return colors[type] || 'secondary';
  }

  /**
   * Format notification time
   */
  formatNotificationTime(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;
    
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  }
}
