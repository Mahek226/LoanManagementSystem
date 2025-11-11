import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { ToastService, Toast } from '../../../core/services/toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-container">
      <div 
        *ngFor="let toast of toasts; trackBy: trackByToastId" 
        class="toast toast-{{toast.type}}"
        [class.toast-entering]="isEntering(toast)"
        [class.toast-leaving]="isLeaving(toast)">
        
        <div class="toast-icon">
          <i [class]="getIconClass(toast.type)"></i>
        </div>
        
        <div class="toast-content">
          <div class="toast-title">{{ toast.title }}</div>
          <div class="toast-message">{{ toast.message }}</div>
          <div class="toast-timestamp">{{ formatTime(toast.timestamp) }}</div>
        </div>
        
        <button 
          class="toast-close" 
          (click)="closeToast(toast.id)"
          aria-label="Close notification">
          <i class="fas fa-times"></i>
        </button>
        
        <div 
          *ngIf="toast.duration && toast.duration > 0" 
          class="toast-progress"
          [style.animation-duration.ms]="toast.duration">
        </div>
      </div>
    </div>
  `,
  styles: [`
    .toast-container {
      position: fixed;
      top: 20px;
      right: 20px;
      z-index: 9999;
      max-width: 400px;
      pointer-events: none;
    }

    .toast {
      display: flex;
      align-items: flex-start;
      background: white;
      border-radius: 8px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      margin-bottom: 12px;
      padding: 16px;
      position: relative;
      overflow: hidden;
      pointer-events: auto;
      transform: translateX(100%);
      opacity: 0;
      transition: all 0.3s ease-in-out;
      border-left: 4px solid;
      max-width: 100%;
      word-wrap: break-word;
    }

    .toast-entering {
      transform: translateX(0);
      opacity: 1;
    }

    .toast-leaving {
      transform: translateX(100%);
      opacity: 0;
    }

    .toast-success {
      border-left-color: #10b981;
      background: linear-gradient(135deg, #f0fdf4 0%, #ffffff 100%);
    }

    .toast-error {
      border-left-color: #ef4444;
      background: linear-gradient(135deg, #fef2f2 0%, #ffffff 100%);
    }

    .toast-warning {
      border-left-color: #f59e0b;
      background: linear-gradient(135deg, #fffbeb 0%, #ffffff 100%);
    }

    .toast-info {
      border-left-color: #3b82f6;
      background: linear-gradient(135deg, #eff6ff 0%, #ffffff 100%);
    }

    .toast-icon {
      flex-shrink: 0;
      width: 24px;
      height: 24px;
      margin-right: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      font-size: 12px;
    }

    .toast-success .toast-icon {
      background: #10b981;
      color: white;
    }

    .toast-error .toast-icon {
      background: #ef4444;
      color: white;
    }

    .toast-warning .toast-icon {
      background: #f59e0b;
      color: white;
    }

    .toast-info .toast-icon {
      background: #3b82f6;
      color: white;
    }

    .toast-content {
      flex: 1;
      min-width: 0;
    }

    .toast-title {
      font-weight: 600;
      font-size: 14px;
      color: #1f2937;
      margin-bottom: 4px;
      line-height: 1.4;
    }

    .toast-message {
      font-size: 13px;
      color: #6b7280;
      line-height: 1.4;
      margin-bottom: 4px;
    }

    .toast-timestamp {
      font-size: 11px;
      color: #9ca3af;
      font-style: italic;
    }

    .toast-close {
      flex-shrink: 0;
      background: none;
      border: none;
      color: #9ca3af;
      cursor: pointer;
      padding: 4px;
      border-radius: 4px;
      margin-left: 8px;
      transition: all 0.2s ease;
      font-size: 12px;
      width: 24px;
      height: 24px;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .toast-close:hover {
      background: rgba(0, 0, 0, 0.1);
      color: #374151;
    }

    .toast-progress {
      position: absolute;
      bottom: 0;
      left: 0;
      height: 3px;
      background: linear-gradient(90deg, rgba(59, 130, 246, 0.3) 0%, rgba(59, 130, 246, 0.8) 100%);
      animation: toast-progress linear forwards;
      border-radius: 0 0 8px 8px;
    }

    .toast-success .toast-progress {
      background: linear-gradient(90deg, rgba(16, 185, 129, 0.3) 0%, rgba(16, 185, 129, 0.8) 100%);
    }

    .toast-error .toast-progress {
      background: linear-gradient(90deg, rgba(239, 68, 68, 0.3) 0%, rgba(239, 68, 68, 0.8) 100%);
    }

    .toast-warning .toast-progress {
      background: linear-gradient(90deg, rgba(245, 158, 11, 0.3) 0%, rgba(245, 158, 11, 0.8) 100%);
    }

    @keyframes toast-progress {
      from {
        width: 100%;
      }
      to {
        width: 0%;
      }
    }

    /* Mobile responsiveness */
    @media (max-width: 768px) {
      .toast-container {
        top: 10px;
        right: 10px;
        left: 10px;
        max-width: none;
      }

      .toast {
        margin-bottom: 8px;
        padding: 12px;
      }

      .toast-title {
        font-size: 13px;
      }

      .toast-message {
        font-size: 12px;
      }
    }

    /* Dark mode support */
    @media (prefers-color-scheme: dark) {
      .toast {
        background: #1f2937;
        color: #f9fafb;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
      }

      .toast-success {
        background: linear-gradient(135deg, #064e3b 0%, #1f2937 100%);
      }

      .toast-error {
        background: linear-gradient(135deg, #7f1d1d 0%, #1f2937 100%);
      }

      .toast-warning {
        background: linear-gradient(135deg, #78350f 0%, #1f2937 100%);
      }

      .toast-info {
        background: linear-gradient(135deg, #1e3a8a 0%, #1f2937 100%);
      }

      .toast-title {
        color: #f9fafb;
      }

      .toast-message {
        color: #d1d5db;
      }

      .toast-timestamp {
        color: #9ca3af;
      }

      .toast-close {
        color: #9ca3af;
      }

      .toast-close:hover {
        background: rgba(255, 255, 255, 0.1);
        color: #f3f4f6;
      }
    }
  `]
})
export class ToastComponent implements OnInit, OnDestroy {
  toasts: Toast[] = [];
  private subscription: Subscription = new Subscription();
  private enteringToasts = new Set<string>();
  private leavingToasts = new Set<string>();

  constructor(private toastService: ToastService) {}

  ngOnInit(): void {
    this.subscription.add(
      this.toastService.toasts$.subscribe(toasts => {
        // Handle new toasts
        const newToasts = toasts.filter(toast => 
          !this.toasts.some(existingToast => existingToast.id === toast.id)
        );

        // Handle removed toasts
        const removedToasts = this.toasts.filter(toast => 
          !toasts.some(newToast => newToast.id === toast.id)
        );

        // Animate new toasts in
        newToasts.forEach(toast => {
          this.enteringToasts.add(toast.id);
          setTimeout(() => {
            this.enteringToasts.delete(toast.id);
          }, 300);
        });

        // Animate removed toasts out
        removedToasts.forEach(toast => {
          this.leavingToasts.add(toast.id);
          setTimeout(() => {
            this.leavingToasts.delete(toast.id);
          }, 500);
        });

        this.toasts = toasts;
      })
    );
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  trackByToastId(index: number, toast: Toast): string {
    return toast.id;
  }

  isEntering(toast: Toast): boolean {
    return this.enteringToasts.has(toast.id);
  }

  isLeaving(toast: Toast): boolean {
    return this.leavingToasts.has(toast.id);
  }

  closeToast(id: string): void {
    this.toastService.removeToast(id);
  }

  getIconClass(type: Toast['type']): string {
    switch (type) {
      case 'success':
        return 'fas fa-check';
      case 'error':
        return 'fas fa-times';
      case 'warning':
        return 'fas fa-exclamation-triangle';
      case 'info':
        return 'fas fa-info-circle';
      default:
        return 'fas fa-bell';
    }
  }

  formatTime(timestamp: Date): string {
    const now = new Date();
    const diff = now.getTime() - timestamp.getTime();
    const seconds = Math.floor(diff / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);

    if (seconds < 60) {
      return 'Just now';
    } else if (minutes < 60) {
      return `${minutes}m ago`;
    } else if (hours < 24) {
      return `${hours}h ago`;
    } else {
      return timestamp.toLocaleDateString();
    }
  }
}
