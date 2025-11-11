import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription, interval } from 'rxjs';
import { AdminService } from '../../../core/services/admin.service';
import { AuthService } from '../../../core/services/auth.service';

export interface SmartNotification {
  id: string;
  type: 'info' | 'success' | 'warning' | 'error' | 'urgent';
  title: string;
  message: string;
  timestamp: Date;
  read: boolean;
  actionable: boolean;
  action?: {
    label: string;
    route: string;
    params?: any;
  };
  priority: 'low' | 'medium' | 'high' | 'urgent';
  category: 'application' | 'system' | 'approval' | 'document' | 'compliance' | 'general';
  relatedId?: string;
  autoExpire?: boolean;
  expireAfter?: number; // minutes
}

@Component({
  selector: 'app-notification-center',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notification-center.component.html',
  styleUrls: ['./notification-center.component.css']
})
export class NotificationCenterComponent implements OnInit, OnDestroy {
  isOpen = false;
  notifications: SmartNotification[] = [];
  filteredNotifications: SmartNotification[] = [];
  activeFilter = 'all';
  currentUserRole = '';
  unreadCount = 0;
  
  private subscriptions: Subscription[] = [];
  private notificationSound: HTMLAudioElement;

  filters = [
    { key: 'all', label: 'All', icon: 'fas fa-list' },
    { key: 'unread', label: 'Unread', icon: 'fas fa-envelope' },
    { key: 'urgent', label: 'Urgent', icon: 'fas fa-exclamation-triangle' },
    { key: 'application', label: 'Applications', icon: 'fas fa-file-alt' },
    { key: 'approval', label: 'Approvals', icon: 'fas fa-check-circle' },
    { key: 'system', label: 'System', icon: 'fas fa-cog' }
  ];

  constructor(
    private adminService: AdminService,
    private authService: AuthService
  ) {
    // Initialize notification sound
    this.notificationSound = new Audio('assets/sounds/notification.mp3');
    this.notificationSound.volume = 0.3;
  }

  ngOnInit() {
    this.loadUserRole();
    this.loadRealTimeNotifications();
    this.startRealTimeUpdates();
    this.loadStoredNotifications();
  }

  ngOnDestroy() {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  private loadUserRole() {
    // Get current user role from localStorage or token
    const userRole = localStorage.getItem('userRole') || '';
    this.currentUserRole = userRole;
  }

  private loadRealTimeNotifications() {
    // Load dashboard stats for generating notifications
    if (this.currentUserRole === 'ADMIN' || this.currentUserRole === 'LOAN_OFFICER') {
      const statsSub = this.adminService.getDashboardStats().subscribe({
        next: (stats) => {
          this.generateNotificationsFromStats(stats);
        },
        error: (error) => console.error('Failed to load stats for notifications:', error)
      });
      this.subscriptions.push(statsSub);

      // Load recent activity logs for notifications
      const activitySub = this.adminService.getRecentActivityLogs().subscribe({
        next: (activities) => {
          this.generateNotificationsFromActivities(activities);
        },
        error: (error) => console.error('Failed to load activities for notifications:', error)
      });
      this.subscriptions.push(activitySub);
    }
  }

  private generateNotificationsFromStats(stats: any) {
    const newNotifications: SmartNotification[] = [];

    // High pending applications alert
    if (stats.pendingApplications > 10) {
      newNotifications.push({
        id: `pending_alert_${Date.now()}`,
        type: 'warning',
        title: 'High Pending Applications',
        message: `${stats.pendingApplications} applications are pending review. Consider prioritizing processing.`,
        timestamp: new Date(),
        read: false,
        actionable: true,
        action: {
          label: 'View Pending',
          route: '/admin/loans',
          params: { status: 'PENDING' }
        },
        priority: 'high',
        category: 'application',
        autoExpire: true,
        expireAfter: 60
      });
    }

    // Low approval rate warning
    const approvalRate = stats.totalApplicants > 0 ? (stats.approvedLoans / stats.totalApplicants) * 100 : 0;
    if (approvalRate < 50 && stats.totalApplicants > 5) {
      newNotifications.push({
        id: `approval_rate_${Date.now()}`,
        type: 'error',
        title: 'Low Approval Rate Alert',
        message: `Current approval rate is ${approvalRate.toFixed(1)}%. Review approval criteria.`,
        timestamp: new Date(),
        read: false,
        actionable: true,
        action: {
          label: 'View Analytics',
          route: '/admin/dashboard',
          params: { tab: 'predictive-analytics' }
        },
        priority: 'urgent',
        category: 'approval',
        autoExpire: false
      });
    }

    // High volume notification
    if (stats.totalApplicants > 50) {
      newNotifications.push({
        id: `high_volume_${Date.now()}`,
        type: 'info',
        title: 'High Application Volume',
        message: `${stats.totalApplicants} total applications received. System performing well.`,
        timestamp: new Date(),
        read: false,
        actionable: true,
        action: {
          label: 'View Dashboard',
          route: '/admin/dashboard'
        },
        priority: 'medium',
        category: 'system',
        autoExpire: true,
        expireAfter: 30
      });
    }

    // Add new notifications and play sound
    if (newNotifications.length > 0) {
      this.addNotifications(newNotifications);
    }
  }

  private generateNotificationsFromActivities(activities: any[]) {
    const recentActivities = activities.slice(0, 5); // Last 5 activities
    
    recentActivities.forEach(activity => {
      // Only create notifications for important activities
      if (['APPROVE', 'REJECT', 'CREATE', 'ESCALATE'].includes(activity.activityType)) {
        const notification: SmartNotification = {
          id: `activity_${activity.logId}_${Date.now()}`,
          type: this.getNotificationTypeFromActivity(activity.activityType),
          title: this.getActivityTitle(activity),
          message: this.getActivityMessage(activity),
          timestamp: new Date(activity.timestamp),
          read: false,
          actionable: true,
          action: this.getActivityAction(activity),
          priority: this.getActivityPriority(activity.activityType),
          category: this.getActivityCategory(activity.activityType),
          relatedId: activity.entityId,
          autoExpire: true,
          expireAfter: 120 // 2 hours
        };

        // Check if notification already exists to avoid duplicates
        if (!this.notifications.find(n => n.relatedId === activity.entityId && n.category === notification.category)) {
          this.addNotification(notification);
        }
      }
    });
  }

  private getNotificationTypeFromActivity(activityType: string): 'info' | 'success' | 'warning' | 'error' | 'urgent' {
    switch (activityType) {
      case 'APPROVE': return 'success';
      case 'REJECT': return 'error';
      case 'ESCALATE': return 'urgent';
      case 'CREATE': return 'info';
      default: return 'info';
    }
  }

  private getActivityTitle(activity: any): string {
    switch (activity.activityType) {
      case 'APPROVE': return 'Loan Approved';
      case 'REJECT': return 'Loan Rejected';
      case 'CREATE': return 'New Application';
      case 'ESCALATE': return 'Compliance Escalation';
      default: return 'System Activity';
    }
  }

  private getActivityMessage(activity: any): string {
    const entityType = activity.entityType || 'item';
    const entityId = activity.entityId || 'unknown';
    
    switch (activity.activityType) {
      case 'APPROVE':
        return `${entityType} #${entityId} has been approved by ${activity.performedBy}`;
      case 'REJECT':
        return `${entityType} #${entityId} has been rejected by ${activity.performedBy}`;
      case 'CREATE':
        return `New ${entityType} #${entityId} has been created`;
      case 'ESCALATE':
        return `${entityType} #${entityId} has been escalated to compliance`;
      default:
        return `${activity.performedBy} performed ${activity.activityType} on ${entityType} #${entityId}`;
    }
  }

  private getActivityAction(activity: any): { label: string; route: string; params?: any } {
    const entityId = activity.entityId;
    
    switch (activity.entityType) {
      case 'LOAN_APPLICATION':
        return {
          label: 'View Application',
          route: '/admin/loans',
          params: { id: entityId }
        };
      case 'APPLICANT':
        return {
          label: 'View Applicant',
          route: '/admin/applicants',
          params: { id: entityId }
        };
      default:
        return {
          label: 'View Details',
          route: '/admin/dashboard'
        };
    }
  }

  private getActivityPriority(activityType: string): 'low' | 'medium' | 'high' | 'urgent' {
    switch (activityType) {
      case 'ESCALATE': return 'urgent';
      case 'APPROVE':
      case 'REJECT': return 'high';
      case 'CREATE': return 'medium';
      default: return 'low';
    }
  }

  private getActivityCategory(activityType: string): 'application' | 'system' | 'approval' | 'document' | 'compliance' | 'general' {
    switch (activityType) {
      case 'APPROVE':
      case 'REJECT': return 'approval';
      case 'CREATE': return 'application';
      case 'ESCALATE': return 'compliance';
      default: return 'system';
    }
  }

  private startRealTimeUpdates() {
    // Update notifications every 30 seconds
    const updateInterval = interval(30000).subscribe(() => {
      this.loadRealTimeNotifications();
      this.cleanupExpiredNotifications();
    });
    this.subscriptions.push(updateInterval);
  }

  private addNotification(notification: SmartNotification) {
    this.notifications.unshift(notification);
    this.updateUnreadCount();
    this.applyFilter();
    this.saveNotifications();
    this.playNotificationSound();
  }

  private addNotifications(notifications: SmartNotification[]) {
    notifications.forEach(notification => {
      this.notifications.unshift(notification);
    });
    this.updateUnreadCount();
    this.applyFilter();
    this.saveNotifications();
    if (notifications.length > 0) {
      this.playNotificationSound();
    }
  }

  private playNotificationSound() {
    try {
      this.notificationSound.play().catch(() => {
        // Ignore audio play errors (user interaction required)
      });
    } catch (error) {
      // Ignore audio errors
    }
  }

  private cleanupExpiredNotifications() {
    const now = new Date();
    this.notifications = this.notifications.filter(notification => {
      if (notification.autoExpire && notification.expireAfter) {
        const expireTime = new Date(notification.timestamp.getTime() + notification.expireAfter * 60000);
        return now < expireTime;
      }
      return true;
    });
    this.applyFilter();
    this.saveNotifications();
  }

  private loadStoredNotifications() {
    const stored = localStorage.getItem('lms_notifications');
    if (stored) {
      try {
        const parsed = JSON.parse(stored);
        this.notifications = parsed.map((n: any) => ({
          ...n,
          timestamp: new Date(n.timestamp)
        }));
        this.updateUnreadCount();
        this.applyFilter();
      } catch (error) {
        console.error('Failed to load stored notifications:', error);
      }
    }
  }

  private saveNotifications() {
    try {
      localStorage.setItem('lms_notifications', JSON.stringify(this.notifications));
    } catch (error) {
      console.error('Failed to save notifications:', error);
    }
  }

  private updateUnreadCount() {
    this.unreadCount = this.notifications.filter(n => !n.read).length;
  }

  private applyFilter() {
    switch (this.activeFilter) {
      case 'unread':
        this.filteredNotifications = this.notifications.filter(n => !n.read);
        break;
      case 'urgent':
        this.filteredNotifications = this.notifications.filter(n => n.priority === 'urgent');
        break;
      case 'application':
      case 'approval':
      case 'system':
        this.filteredNotifications = this.notifications.filter(n => n.category === this.activeFilter);
        break;
      default:
        this.filteredNotifications = [...this.notifications];
    }
  }

  // Public methods
  toggleNotificationCenter() {
    this.isOpen = !this.isOpen;
  }

  setFilter(filter: string) {
    this.activeFilter = filter;
    this.applyFilter();
  }

  markAsRead(notification: SmartNotification) {
    notification.read = true;
    this.updateUnreadCount();
    this.saveNotifications();
  }

  markAllAsRead() {
    this.notifications.forEach(n => n.read = true);
    this.updateUnreadCount();
    this.saveNotifications();
  }

  deleteNotification(notification: SmartNotification) {
    const index = this.notifications.findIndex(n => n.id === notification.id);
    if (index > -1) {
      this.notifications.splice(index, 1);
      this.updateUnreadCount();
      this.applyFilter();
      this.saveNotifications();
    }
  }

  clearAllNotifications() {
    this.notifications = [];
    this.filteredNotifications = [];
    this.unreadCount = 0;
    this.saveNotifications();
  }

  executeNotificationAction(notification: SmartNotification) {
    if (notification.actionable && notification.action) {
      // Mark as read when action is taken
      this.markAsRead(notification);
      
      // Navigate to the specified route
      // This would typically use Router.navigate()
      console.log('Navigate to:', notification.action.route, notification.action.params);
    }
  }

  getNotificationIcon(type: string): string {
    switch (type) {
      case 'success': return 'fas fa-check-circle';
      case 'warning': return 'fas fa-exclamation-triangle';
      case 'error': return 'fas fa-times-circle';
      case 'urgent': return 'fas fa-fire';
      default: return 'fas fa-info-circle';
    }
  }

  getRelativeTime(timestamp: Date): string {
    const now = new Date();
    const diff = now.getTime() - timestamp.getTime();
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);

    if (minutes < 1) return 'Just now';
    if (minutes < 60) return `${minutes}m ago`;
    if (hours < 24) return `${hours}h ago`;
    if (days < 7) return `${days}d ago`;
    return timestamp.toLocaleDateString();
  }

  trackByNotificationId(index: number, notification: SmartNotification): string {
    return notification.id;
  }
}
