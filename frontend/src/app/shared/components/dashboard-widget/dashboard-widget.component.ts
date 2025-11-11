import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface WidgetAction {
  label: string;
  icon: string;
  callback: () => void;
}

@Component({
  selector: 'app-dashboard-widget',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="dashboard-widget" [class.loading]="loading" [class.clickable]="clickable" (click)="onWidgetClick()">
      
      <!-- Widget Header -->
      <div class="widget-header" *ngIf="title || actions.length > 0">
        <div class="widget-title-section">
          <h6 class="widget-title" *ngIf="title">{{ title }}</h6>
          <span class="widget-subtitle" *ngIf="subtitle">{{ subtitle }}</span>
        </div>
        
        <div class="widget-actions" *ngIf="actions.length > 0">
          <button
            *ngFor="let action of actions"
            class="action-btn"
            (click)="executeAction(action, $event)"
            [title]="action.label"
          >
            <i class="fas" [ngClass]="action.icon"></i>
          </button>
        </div>
      </div>

      <!-- Widget Content -->
      <div class="widget-content">
        
        <!-- Loading State -->
        <div *ngIf="loading" class="widget-loading">
          <div class="loading-skeleton">
            <div class="skeleton-line large"></div>
            <div class="skeleton-line medium"></div>
            <div class="skeleton-line small"></div>
          </div>
        </div>

        <!-- Error State -->
        <div *ngIf="error && !loading" class="widget-error">
          <div class="error-icon">
            <i class="fas fa-exclamation-triangle"></i>
          </div>
          <p class="error-message">{{ error }}</p>
          <button *ngIf="retryCallback" class="retry-btn" (click)="onRetry()">
            <i class="fas fa-redo"></i>
            Retry
          </button>
        </div>

        <!-- Empty State -->
        <div *ngIf="isEmpty && !loading && !error" class="widget-empty">
          <div class="empty-icon">
            <i class="fas" [ngClass]="emptyIcon || 'fa-inbox'"></i>
          </div>
          <p class="empty-message">{{ emptyMessage || 'No data available' }}</p>
        </div>

        <!-- Main Content -->
        <div *ngIf="!loading && !error && !isEmpty" class="widget-main-content">
          
          <!-- Stat Display -->
          <div *ngIf="type === 'stat'" class="stat-content">
            <div class="stat-icon" *ngIf="icon" [style.background]="iconBackground">
              <i class="fas" [ngClass]="icon"></i>
            </div>
            <div class="stat-info">
              <div class="stat-value" [style.color]="valueColor">
                {{ value }}
                <span *ngIf="unit" class="stat-unit">{{ unit }}</span>
              </div>
              <div class="stat-change" *ngIf="change !== undefined" [class.positive]="change > 0" [class.negative]="change < 0">
                <i class="fas" [ngClass]="change > 0 ? 'fa-arrow-up' : 'fa-arrow-down'"></i>
                {{ Math.abs(change) }}%
              </div>
            </div>
          </div>

          <!-- Chart Display -->
          <div *ngIf="type === 'chart'" class="chart-content">
            <ng-content select="[slot=chart]"></ng-content>
          </div>

          <!-- List Display -->
          <div *ngIf="type === 'list'" class="list-content">
            <ng-content select="[slot=list]"></ng-content>
          </div>

          <!-- Custom Content -->
          <div *ngIf="type === 'custom'" class="custom-content">
            <ng-content></ng-content>
          </div>
        </div>
      </div>

      <!-- Widget Footer -->
      <div class="widget-footer" *ngIf="footerText || hasFooterContent">
        <span *ngIf="footerText" class="footer-text">{{ footerText }}</span>
        <ng-content select="[slot=footer]"></ng-content>
      </div>

      <!-- Hover Overlay -->
      <div *ngIf="clickable" class="hover-overlay">
        <i class="fas fa-external-link-alt"></i>
      </div>
    </div>
  `,
  styles: [`
    .dashboard-widget {
      background: var(--bg-primary);
      border: 1px solid var(--border-color);
      border-radius: 12px;
      overflow: hidden;
      transition: all 0.3s ease;
      position: relative;
      height: 100%;
      display: flex;
      flex-direction: column;
    }

    .dashboard-widget:hover {
      box-shadow: var(--shadow-lg);
      transform: translateY(-2px);
    }

    .dashboard-widget.clickable {
      cursor: pointer;
    }

    .dashboard-widget.loading {
      pointer-events: none;
    }

    /* Widget Header */
    .widget-header {
      padding: 20px 20px 0 20px;
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
    }

    .widget-title-section {
      flex: 1;
      min-width: 0;
    }

    .widget-title {
      font-size: 16px;
      font-weight: 600;
      color: var(--text-primary);
      margin: 0 0 4px 0;
    }

    .widget-subtitle {
      font-size: 12px;
      color: var(--text-secondary);
    }

    .widget-actions {
      display: flex;
      gap: 8px;
    }

    .action-btn {
      background: none;
      border: none;
      color: var(--text-muted);
      padding: 6px;
      border-radius: 6px;
      cursor: pointer;
      transition: all 0.2s ease;
      font-size: 14px;
    }

    .action-btn:hover {
      background: var(--bg-secondary);
      color: var(--text-primary);
    }

    /* Widget Content */
    .widget-content {
      flex: 1;
      padding: 20px;
      display: flex;
      flex-direction: column;
      justify-content: center;
      min-height: 120px;
    }

    /* Loading State */
    .widget-loading {
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .loading-skeleton {
      width: 100%;
    }

    .skeleton-line {
      height: 12px;
      background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
      background-size: 200% 100%;
      animation: shimmer 1.5s infinite;
      border-radius: 6px;
      margin-bottom: 8px;
    }

    .skeleton-line.large { width: 80%; height: 16px; }
    .skeleton-line.medium { width: 60%; }
    .skeleton-line.small { width: 40%; }

    .dark-theme .skeleton-line {
      background: linear-gradient(90deg, #374151 25%, #4b5563 50%, #374151 75%);
      background-size: 200% 100%;
    }

    /* Error State */
    .widget-error {
      text-align: center;
      color: var(--danger);
    }

    .error-icon {
      font-size: 24px;
      margin-bottom: 12px;
    }

    .error-message {
      font-size: 14px;
      margin: 0 0 16px 0;
    }

    .retry-btn {
      background: var(--danger);
      color: white;
      border: none;
      padding: 8px 16px;
      border-radius: 6px;
      cursor: pointer;
      font-size: 12px;
      display: flex;
      align-items: center;
      gap: 6px;
      margin: 0 auto;
      transition: all 0.2s ease;
    }

    .retry-btn:hover {
      background: #dc2626;
    }

    /* Empty State */
    .widget-empty {
      text-align: center;
      color: var(--text-muted);
    }

    .empty-icon {
      font-size: 32px;
      margin-bottom: 12px;
      opacity: 0.5;
    }

    .empty-message {
      font-size: 14px;
      margin: 0;
    }

    /* Stat Content */
    .stat-content {
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .stat-icon {
      width: 48px;
      height: 48px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
      color: white;
      flex-shrink: 0;
    }

    .stat-info {
      flex: 1;
      min-width: 0;
    }

    .stat-value {
      font-size: 24px;
      font-weight: 700;
      color: var(--text-primary);
      line-height: 1.2;
      margin-bottom: 4px;
    }

    .stat-unit {
      font-size: 14px;
      font-weight: 400;
      color: var(--text-secondary);
      margin-left: 4px;
    }

    .stat-change {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 12px;
      font-weight: 600;
    }

    .stat-change.positive {
      color: var(--success);
    }

    .stat-change.negative {
      color: var(--danger);
    }

    /* Chart, List, Custom Content */
    .chart-content,
    .list-content,
    .custom-content {
      height: 100%;
    }

    /* Widget Footer */
    .widget-footer {
      padding: 0 20px 20px 20px;
      border-top: 1px solid var(--border-color);
      margin-top: auto;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .footer-text {
      font-size: 12px;
      color: var(--text-secondary);
    }

    /* Hover Overlay */
    .hover-overlay {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(59, 130, 246, 0.1);
      display: flex;
      align-items: center;
      justify-content: center;
      opacity: 0;
      transition: opacity 0.3s ease;
      font-size: 24px;
      color: var(--primary);
    }

    .dashboard-widget.clickable:hover .hover-overlay {
      opacity: 1;
    }

    @keyframes shimmer {
      0% { background-position: -200% 0; }
      100% { background-position: 200% 0; }
    }

    /* Mobile responsiveness */
    @media (max-width: 768px) {
      .widget-content {
        padding: 16px;
        min-height: 100px;
      }

      .widget-header {
        padding: 16px 16px 0 16px;
      }

      .widget-footer {
        padding: 0 16px 16px 16px;
      }

      .stat-content {
        gap: 12px;
      }

      .stat-icon {
        width: 40px;
        height: 40px;
        font-size: 18px;
      }

      .stat-value {
        font-size: 20px;
      }
    }
  `]
})
export class DashboardWidgetComponent implements OnInit {
  @Input() type: 'stat' | 'chart' | 'list' | 'custom' = 'custom';
  @Input() title: string = '';
  @Input() subtitle: string = '';
  @Input() loading: boolean = false;
  @Input() error: string = '';
  @Input() isEmpty: boolean = false;
  @Input() emptyMessage: string = '';
  @Input() emptyIcon: string = '';
  @Input() footerText: string = '';
  @Input() clickable: boolean = false;
  @Input() actions: WidgetAction[] = [];
  
  // Stat-specific inputs
  @Input() icon: string = '';
  @Input() iconBackground: string = '';
  @Input() value: string | number = '';
  @Input() unit: string = '';
  @Input() valueColor: string = '';
  @Input() change: number | undefined = undefined;

  @Output() widgetClick = new EventEmitter<void>();
  @Output() retry = new EventEmitter<void>();

  hasFooterContent: boolean = false;
  Math = Math;

  constructor() {}

  ngOnInit(): void {
    // Check if there's footer content projected
    // This is a simple check - in a real implementation, you might use ViewChild
  }

  onWidgetClick(): void {
    if (this.clickable && !this.loading) {
      this.widgetClick.emit();
    }
  }

  executeAction(action: WidgetAction, event: Event): void {
    event.stopPropagation();
    action.callback();
  }

  onRetry(): void {
    this.retry.emit();
  }

  get retryCallback(): boolean {
    return this.retry.observers.length > 0;
  }
}
