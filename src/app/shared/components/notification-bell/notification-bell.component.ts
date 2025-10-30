import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { NotificationService, Notification } from '@core/services/notification.service';
import { AuthService } from '@core/services/auth.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-notification-bell',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notification-bell.component.html',
  styleUrl: './notification-bell.component.css'
})
export class NotificationBellComponent implements OnInit, OnDestroy {
  notifications: Notification[] = [];
  unreadCount = 0;
  showDropdown = false;
  loading = false;
  
  private subscriptions: Subscription[] = [];

  constructor(
    private notificationService: NotificationService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const user = this.authService.currentUserValue;
    const userId = user?.userId || user?.officerId || user?.applicantId || 0;
    
    if (userId) {
      // Subscribe to notifications stream
      this.subscriptions.push(
        this.notificationService.notifications$.subscribe(notifications => {
          this.notifications = notifications.slice(0, 10); // Show latest 10
        })
      );
      
      // Subscribe to unread count
      this.subscriptions.push(
        this.notificationService.unreadCount$.subscribe(count => {
          this.unreadCount = count;
        })
      );
      
      // Start polling for notifications
      this.notificationService.startPolling(userId);
    }
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.notificationService.stopPolling();
  }

  toggleDropdown(): void {
    this.showDropdown = !this.showDropdown;
  }

  closeDropdown(): void {
    this.showDropdown = false;
  }

  markAsRead(notification: Notification, event: Event): void {
    event.stopPropagation();
    if (!notification.isRead) {
      this.notificationService.markAsRead(notification.notificationId).subscribe();
    }
  }

  markAllAsRead(): void {
    const user = this.authService.currentUserValue;
    const userId = user?.userId || user?.officerId || user?.applicantId || 0;
    
    if (userId) {
      this.notificationService.markAllAsRead(userId).subscribe();
    }
  }

  deleteNotification(notificationId: number, event: Event): void {
    event.stopPropagation();
    this.notificationService.deleteNotification(notificationId).subscribe();
  }

  handleNotificationClick(notification: Notification): void {
    // Mark as read
    if (!notification.isRead) {
      this.notificationService.markAsRead(notification.notificationId).subscribe();
    }
    
    // Navigate based on notification type and related entity
    if (notification.relatedEntityType === 'LOAN' && notification.relatedEntityId) {
      const user = this.authService.currentUserValue;
      const role = user?.role;
      
      if (role === 'LOAN_OFFICER') {
        this.router.navigate(['/loan-officer/review', notification.relatedEntityId]);
      } else if (role === 'COMPLIANCE_OFFICER') {
        this.router.navigate(['/compliance-officer/review', notification.relatedEntityId]);
      } else if (role === 'APPLICANT') {
        this.router.navigate(['/applicant/dashboard']);
      }
    }
    
    this.closeDropdown();
  }

  getNotificationIcon(type: string): string {
    return this.notificationService.getNotificationIcon(type);
  }

  getNotificationColor(type: string): string {
    return this.notificationService.getNotificationColor(type);
  }

  formatTime(dateString: string): string {
    return this.notificationService.formatNotificationTime(dateString);
  }
}
