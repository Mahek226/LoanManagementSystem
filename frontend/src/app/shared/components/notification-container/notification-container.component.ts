import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { EnhancedNotificationService, Notification } from '../../../core/services/enhanced-notification.service';

@Component({
  selector: 'app-notification-container',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="notification-container">
      <div 
        *ngFor="let notification of notifications; trackBy: trackByFn" 
        class="notification-item"
        [ngClass]="'notification-' + notification.type"
        [@slideIn]
      >
        <div class="notification-content">
          <div class="notification-icon">
            <i class="fas" [ngClass]="getIconClass(notification.type)"></i>
          </div>
          
          <div class="notification-body">
            <h6 class="notification-title">{{ notification.title }}</h6>
            <p class="notification-message">{{ notification.message }}</p>
            <small class="notification-time">{{ getTimeAgo(notification.timestamp) }}</small>
          </div>
          
          <div class="notification-actions">
            <button 
              *ngIf="notification.action" 
              class="btn btn-sm notification-action-btn"
              (click)="executeAction(notification)"
            >
              {{ notification.action.label }}
            </button>
            
            <button 
              class="btn btn-sm notification-close-btn"
              (click)="closeNotification(notification.id)"
              title="Close"
            >
              <i class="fas fa-times"></i>
            </button>
          </div>
        </div>
        
        <div 
          *ngIf="!notification.persistent" 
          class="notification-progress"
          [style.animation-duration]="notification.duration + 'ms'"
        ></div>
      </div>
    </div>
  `,
  styles: [`
    .notification-container {
      position: fixed;
      top: 20px;
      right: 20px;
      z-index: 9999;
      max-width: 400px;
      width: 100%;
    }

    .notification-item {
      background: var(--bg-primary);
      border: 1px solid var(--border-color);
      border-radius: 12px;
      margin-bottom: 12px;
      box-shadow: var(--shadow-lg);
      overflow: hidden;
      position: relative;
      animation: slideIn 0.3s ease-out;
    }

    .notification-content {
      display: flex;
      align-items: flex-start;
      padding: 16px;
      gap: 12px;
    }

    .notification-icon {
      flex-shrink: 0;
      width: 24px;
      height: 24px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      font-size: 12px;
    }

    .notification-success .notification-icon {
      background: rgba(16, 185, 129, 0.1);
      color: var(--success);
    }

    .notification-error .notification-icon {
      background: rgba(239, 68, 68, 0.1);
      color: var(--danger);
    }

    .notification-warning .notification-icon {
      background: rgba(245, 158, 11, 0.1);
      color: var(--warning);
    }

    .notification-info .notification-icon {
      background: rgba(6, 182, 212, 0.1);
      color: var(--info);
    }

    .notification-body {
      flex: 1;
      min-width: 0;
    }

    .notification-title {
      font-size: 14px;
      font-weight: 600;
      margin: 0 0 4px 0;
      color: var(--text-primary);
    }

    .notification-message {
      font-size: 13px;
      margin: 0 0 4px 0;
      color: var(--text-secondary);
      line-height: 1.4;
    }

    .notification-time {
      font-size: 11px;
      color: var(--text-muted);
    }

    .notification-actions {
      display: flex;
      flex-direction: column;
      gap: 4px;
      align-items: flex-end;
    }

    .notification-action-btn {
      background: var(--primary);
      color: white;
      border: none;
      padding: 4px 8px;
      border-radius: 4px;
      font-size: 11px;
      cursor: pointer;
      transition: all 0.2s ease;
    }

    .notification-action-btn:hover {
      background: var(--primary-dark);
      transform: translateY(-1px);
    }

    .notification-close-btn {
      background: transparent;
      border: none;
      color: var(--text-muted);
      padding: 4px;
      border-radius: 4px;
      cursor: pointer;
      transition: all 0.2s ease;
      font-size: 10px;
    }

    .notification-close-btn:hover {
      background: rgba(0, 0, 0, 0.1);
      color: var(--text-primary);
    }

    .notification-progress {
      position: absolute;
      bottom: 0;
      left: 0;
      height: 3px;
      background: linear-gradient(90deg, var(--primary), var(--primary-dark));
      animation: progress linear;
      transform-origin: left;
    }

    @keyframes slideIn {
      from {
        transform: translateX(100%);
        opacity: 0;
      }
      to {
        transform: translateX(0);
        opacity: 1;
      }
    }

    @keyframes progress {
      from {
        transform: scaleX(1);
      }
      to {
        transform: scaleX(0);
      }
    }

    /* Mobile responsiveness */
    @media (max-width: 768px) {
      .notification-container {
        left: 20px;
        right: 20px;
        max-width: none;
      }
      
      .notification-content {
        padding: 12px;
      }
    }
  `],
  animations: []
})
export class NotificationContainerComponent implements OnInit, OnDestroy {
  notifications: Notification[] = [];
  private subscription?: Subscription;

  constructor(private notificationService: EnhancedNotificationService) {}

  ngOnInit(): void {
    this.subscription = this.notificationService.getNotifications().subscribe(
      notifications => this.notifications = notifications
    );
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  trackByFn(index: number, notification: Notification): string {
    return notification.id;
  }

  getIconClass(type: string): string {
    switch (type) {
      case 'success': return 'fa-check';
      case 'error': return 'fa-exclamation-triangle';
      case 'warning': return 'fa-exclamation';
      case 'info': return 'fa-info';
      default: return 'fa-bell';
    }
  }

  getTimeAgo(timestamp: Date): string {
    const now = new Date();
    const diff = now.getTime() - timestamp.getTime();
    const seconds = Math.floor(diff / 1000);
    
    if (seconds < 60) return 'Just now';
    if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
    if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
    return `${Math.floor(seconds / 86400)}d ago`;
  }

  closeNotification(id: string): void {
    this.notificationService.remove(id);
  }

  executeAction(notification: Notification): void {
    if (notification.action) {
      notification.action.callback();
      this.closeNotification(notification.id);
    }
  }
}
