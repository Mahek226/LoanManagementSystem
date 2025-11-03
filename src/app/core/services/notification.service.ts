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
   * Get notification icon based on type
   */
  getNotificationIcon(type: string): string {
    const icons: { [key: string]: string } = {
      'NEW_APPLICATION': 'bi-file-earmark-plus',
      'DOCUMENT_UPLOADED': 'bi-cloud-upload',
      'FRAUD_CHECK_COMPLETE': 'bi-shield-check',
      'ESCALATION': 'bi-exclamation-triangle',
      'APPROVAL': 'bi-check-circle',
      'REJECTION': 'bi-x-circle',
      'DOCUMENT_REQUEST': 'bi-file-earmark-text',
      'STATUS_UPDATE': 'bi-bell'
    };
    return icons[type] || 'bi-bell';
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
