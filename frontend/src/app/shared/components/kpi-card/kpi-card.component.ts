import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface KPITrend {
  value: number;
  direction: 'up' | 'down' | 'neutral';
  period: string;
}

@Component({
  selector: 'app-kpi-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="kpi-card" [ngClass]="cardClass" [style.animation-delay]="animationDelay">
      <div class="kpi-content">
        <div class="kpi-header">
          <div class="kpi-icon" [ngClass]="iconClass">
            <i [class]="icon"></i>
          </div>
          <div class="kpi-menu" *ngIf="showMenu">
            <button class="menu-btn" (click)="toggleMenu()">
              <i class="fas fa-ellipsis-v"></i>
            </button>
            <div class="menu-dropdown" *ngIf="menuVisible">
              <button class="menu-item" (click)="onMenuAction('view')">
                <i class="fas fa-eye"></i> View Details
              </button>
              <button class="menu-item" (click)="onMenuAction('export')">
                <i class="fas fa-download"></i> Export
              </button>
            </div>
          </div>
        </div>

        <div class="kpi-main">
          <div class="kpi-value-section">
            <h3 class="kpi-value" [ngClass]="valueClass">
              <span class="value-prefix" *ngIf="prefix">{{ prefix }}</span>
              <span class="value-number" [attr.data-value]="value">{{ formattedValue }}</span>
              <span class="value-suffix" *ngIf="suffix">{{ suffix }}</span>
            </h3>
            <p class="kpi-label">{{ label }}</p>
          </div>

          <div class="kpi-trend" *ngIf="trend">
            <div class="trend-indicator" [ngClass]="getTrendClass()">
              <i [class]="getTrendIcon()"></i>
              <span class="trend-value">{{ formatTrendValue() }}</span>
            </div>
            <span class="trend-period">{{ trend.period }}</span>
          </div>
        </div>

        <div class="kpi-footer" *ngIf="description || showProgress">
          <p class="kpi-description" *ngIf="description">{{ description }}</p>
          
          <div class="kpi-progress" *ngIf="showProgress">
            <div class="progress-bar">
              <div 
                class="progress-fill" 
                [style.width.%]="progressPercentage"
                [ngClass]="progressClass"></div>
            </div>
            <div class="progress-labels">
              <span class="progress-current">{{ progressCurrent }}</span>
              <span class="progress-target">{{ progressTarget }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Sparkline Chart (Optional) -->
      <div class="kpi-chart" *ngIf="showChart">
        <canvas #chartCanvas width="100" height="30"></canvas>
      </div>

      <!-- Loading Overlay -->
      <div class="loading-overlay" *ngIf="loading">
        <div class="loading-spinner">
          <i class="fas fa-spinner fa-spin"></i>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .kpi-card {
      background: var(--white);
      border-radius: var(--radius-xl);
      box-shadow: var(--shadow-md);
      padding: var(--space-6);
      position: relative;
      overflow: hidden;
      transition: all var(--transition-normal);
      border: 1px solid transparent;
    }

    .kpi-card:hover {
      transform: translateY(-4px);
      box-shadow: var(--shadow-xl);
    }

    .kpi-card.animate-slide-up {
      animation: slideUp 0.6s ease-out forwards;
      opacity: 0;
      transform: translateY(20px);
    }

    .kpi-card.primary {
      border-color: var(--primary-blue);
      background: linear-gradient(135deg, var(--white) 0%, rgba(30, 64, 175, 0.02) 100%);
    }

    .kpi-card.success {
      border-color: var(--success-green);
      background: linear-gradient(135deg, var(--white) 0%, rgba(5, 150, 105, 0.02) 100%);
    }

    .kpi-card.warning {
      border-color: var(--warning-amber);
      background: linear-gradient(135deg, var(--white) 0%, rgba(217, 119, 6, 0.02) 100%);
    }

    .kpi-card.danger {
      border-color: var(--danger-red);
      background: linear-gradient(135deg, var(--white) 0%, rgba(220, 38, 38, 0.02) 100%);
    }

    .kpi-content {
      position: relative;
      z-index: 2;
    }

    .kpi-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: var(--space-4);
    }

    .kpi-icon {
      width: 56px;
      height: 56px;
      border-radius: var(--radius-xl);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 1.5rem;
      position: relative;
      overflow: hidden;
    }

    .kpi-icon::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: inherit;
      opacity: 0.1;
      border-radius: inherit;
    }

    .kpi-icon.primary {
      background: var(--gradient-primary);
      color: var(--white);
    }

    .kpi-icon.success {
      background: var(--gradient-success);
      color: var(--white);
    }

    .kpi-icon.warning {
      background: var(--gradient-gold);
      color: var(--white);
    }

    .kpi-icon.danger {
      background: var(--gradient-danger);
      color: var(--white);
    }

    .kpi-icon.info {
      background: linear-gradient(135deg, var(--info-cyan) 0%, var(--info-cyan-light) 100%);
      color: var(--white);
    }

    .kpi-menu {
      position: relative;
    }

    .menu-btn {
      padding: var(--space-1);
      border: none;
      background: none;
      color: var(--gray-400);
      cursor: pointer;
      border-radius: var(--radius-md);
      transition: all var(--transition-normal);
    }

    .menu-btn:hover {
      background: var(--gray-100);
      color: var(--gray-600);
    }

    .menu-dropdown {
      position: absolute;
      top: 100%;
      right: 0;
      background: var(--white);
      border-radius: var(--radius-lg);
      box-shadow: var(--shadow-xl);
      padding: var(--space-2);
      min-width: 150px;
      z-index: var(--z-dropdown);
      animation: slideDown 0.2s ease-out;
    }

    .menu-item {
      display: flex;
      align-items: center;
      gap: var(--space-2);
      width: 100%;
      padding: var(--space-2) var(--space-3);
      border: none;
      background: none;
      color: var(--gray-700);
      font-size: var(--text-sm);
      text-align: left;
      border-radius: var(--radius-md);
      cursor: pointer;
      transition: all var(--transition-normal);
    }

    .menu-item:hover {
      background: var(--gray-50);
      color: var(--primary-blue);
    }

    .kpi-main {
      display: flex;
      justify-content: space-between;
      align-items: flex-end;
      margin-bottom: var(--space-4);
    }

    .kpi-value {
      font-size: var(--text-3xl);
      font-weight: var(--font-bold);
      line-height: 1;
      margin: 0 0 var(--space-2) 0;
      display: flex;
      align-items: baseline;
      gap: var(--space-1);
    }

    .kpi-value.animate-counter .value-number {
      animation: countUp 1s ease-out;
    }

    .kpi-value.primary { color: var(--primary-blue); }
    .kpi-value.success { color: var(--success-green); }
    .kpi-value.warning { color: var(--warning-amber); }
    .kpi-value.danger { color: var(--danger-red); }
    .kpi-value.info { color: var(--info-cyan); }

    .value-prefix,
    .value-suffix {
      font-size: var(--text-lg);
      font-weight: var(--font-medium);
      color: var(--gray-500);
    }

    .kpi-label {
      font-size: var(--text-sm);
      font-weight: var(--font-medium);
      color: var(--gray-600);
      margin: 0;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }

    .kpi-trend {
      text-align: right;
    }

    .trend-indicator {
      display: inline-flex;
      align-items: center;
      gap: var(--space-1);
      padding: var(--space-1) var(--space-2);
      border-radius: var(--radius-full);
      font-size: var(--text-xs);
      font-weight: var(--font-semibold);
      margin-bottom: var(--space-1);
    }

    .trend-indicator.positive {
      background: rgba(5, 150, 105, 0.1);
      color: var(--success-green);
    }

    .trend-indicator.negative {
      background: rgba(220, 38, 38, 0.1);
      color: var(--danger-red);
    }

    .trend-indicator.neutral {
      background: var(--gray-100);
      color: var(--gray-600);
    }

    .trend-period {
      font-size: var(--text-xs);
      color: var(--gray-500);
      display: block;
    }

    .kpi-description {
      font-size: var(--text-sm);
      color: var(--gray-600);
      margin: 0;
      line-height: 1.4;
    }

    .kpi-progress {
      margin-top: var(--space-3);
    }

    .progress-bar {
      height: 6px;
      background: var(--gray-100);
      border-radius: var(--radius-full);
      overflow: hidden;
      margin-bottom: var(--space-2);
    }

    .progress-fill {
      height: 100%;
      border-radius: inherit;
      transition: width 1s ease-out;
      position: relative;
      overflow: hidden;
    }

    .progress-fill::after {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: linear-gradient(90deg, transparent, rgba(255,255,255,0.4), transparent);
      animation: shimmer 2s infinite;
    }

    .progress-fill.primary { background: var(--gradient-primary); }
    .progress-fill.success { background: var(--gradient-success); }
    .progress-fill.warning { background: var(--gradient-gold); }
    .progress-fill.danger { background: var(--gradient-danger); }

    .progress-labels {
      display: flex;
      justify-content: space-between;
      font-size: var(--text-xs);
      color: var(--gray-500);
    }

    .kpi-chart {
      margin-top: var(--space-4);
      padding-top: var(--space-4);
      border-top: 1px solid var(--gray-100);
    }

    .loading-overlay {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(255, 255, 255, 0.8);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 10;
      backdrop-filter: blur(2px);
    }

    .loading-spinner {
      font-size: 1.5rem;
      color: var(--primary-blue);
    }

    @keyframes countUp {
      from { opacity: 0; transform: translateY(10px); }
      to { opacity: 1; transform: translateY(0); }
    }

    @keyframes shimmer {
      0% { transform: translateX(-100%); }
      100% { transform: translateX(100%); }
    }

    @keyframes slideUp {
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    @keyframes slideDown {
      from {
        opacity: 0;
        transform: translateY(-10px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    /* Responsive Design */
    @media (max-width: 768px) {
      .kpi-card {
        padding: var(--space-4);
      }

      .kpi-value {
        font-size: var(--text-2xl);
      }

      .kpi-icon {
        width: 48px;
        height: 48px;
        font-size: 1.25rem;
      }

      .kpi-main {
        flex-direction: column;
        align-items: flex-start;
        gap: var(--space-2);
      }

      .kpi-trend {
        text-align: left;
      }
    }
  `]
})
export class KpiCardComponent implements OnInit {
  @Input() label = '';
  @Input() value: number | string = 0;
  @Input() icon = 'fas fa-chart-line';
  @Input() color: 'primary' | 'success' | 'warning' | 'danger' | 'info' = 'primary';
  @Input() trend?: KPITrend;
  @Input() description?: string;
  @Input() prefix?: string;
  @Input() suffix?: string;
  @Input() loading = false;
  @Input() showMenu = false;
  @Input() showChart = false;
  @Input() showProgress = false;
  @Input() progressCurrent?: number;
  @Input() progressTarget?: number;
  @Input() animationDelay = '0s';
  @Input() animate = true;

  menuVisible = false;

  get cardClass(): string {
    const classes: string[] = [this.color];
    if (this.animate) classes.push('animate-slide-up');
    return classes.join(' ');
  }

  get iconClass(): string {
    return this.color;
  }

  get valueClass(): string {
    const classes: string[] = [this.color];
    if (this.animate) classes.push('animate-counter');
    return classes.join(' ');
  }

  get progressClass(): string {
    return this.color;
  }

  get progressPercentage(): number {
    if (!this.progressCurrent || !this.progressTarget) return 0;
    return Math.min((this.progressCurrent / this.progressTarget) * 100, 100);
  }

  get formattedValue(): string {
    if (typeof this.value === 'string') return this.value;
    
    if (this.value >= 1000000) {
      return (this.value / 1000000).toFixed(1) + 'M';
    } else if (this.value >= 1000) {
      return (this.value / 1000).toFixed(1) + 'K';
    }
    
    return this.value.toLocaleString();
  }

  ngOnInit(): void {
    // Component initialization
  }

  toggleMenu(): void {
    this.menuVisible = !this.menuVisible;
  }

  onMenuAction(action: string): void {
    this.menuVisible = false;
    console.log('Menu action:', action);
  }

  getTrendClass(): string {
    if (!this.trend) return '';
    
    switch (this.trend.direction) {
      case 'up': return 'positive';
      case 'down': return 'negative';
      default: return 'neutral';
    }
  }

  getTrendIcon(): string {
    if (!this.trend) return '';
    
    switch (this.trend.direction) {
      case 'up': return 'fas fa-arrow-up';
      case 'down': return 'fas fa-arrow-down';
      default: return 'fas fa-minus';
    }
  }

  formatTrendValue(): string {
    if (!this.trend) return '';
    
    const sign = this.trend.direction === 'up' ? '+' : 
                 this.trend.direction === 'down' ? '-' : '';
    
    return `${sign}${Math.abs(this.trend.value)}%`;
  }
}
