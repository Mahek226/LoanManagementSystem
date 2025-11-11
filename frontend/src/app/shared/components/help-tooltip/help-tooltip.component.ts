import { Component, Input, OnInit, OnDestroy, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface TooltipPosition {
  top?: string;
  left?: string;
  right?: string;
  bottom?: string;
}

@Component({
  selector: 'app-help-tooltip',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="help-tooltip-trigger" (mouseenter)="showTooltip()" (mouseleave)="hideTooltip()" (click)="toggleTooltip()">
      <ng-content></ng-content>
      <i *ngIf="showIcon" class="help-icon fas fa-question-circle"></i>
    </div>

    <div 
      *ngIf="visible"
      #tooltipElement
      class="help-tooltip"
      [ngClass]="'tooltip-' + placement"
      [style.max-width]="maxWidth"
      [@fadeInOut]
    >
      <div class="tooltip-header" *ngIf="title">
        <h6 class="tooltip-title">{{ title }}</h6>
        <button *ngIf="closable" class="tooltip-close" (click)="hideTooltip()">
          <i class="fas fa-times"></i>
        </button>
      </div>
      
      <div class="tooltip-content">
        <p *ngIf="content" [innerHTML]="content"></p>
        <ng-content select="[slot=content]"></ng-content>
      </div>
      
      <div class="tooltip-actions" *ngIf="actions.length > 0">
        <button 
          *ngFor="let action of actions"
          class="tooltip-action-btn"
          [ngClass]="action.type || 'primary'"
          (click)="executeAction(action)"
        >
          <i *ngIf="action.icon" class="fas" [ngClass]="action.icon"></i>
          {{ action.label }}
        </button>
      </div>
      
      <div class="tooltip-arrow"></div>
    </div>
  `,
  styles: [`
    .help-tooltip-trigger {
      position: relative;
      display: inline-block;
      cursor: help;
    }

    .help-icon {
      color: var(--primary);
      font-size: 14px;
      margin-left: 6px;
      opacity: 0.7;
      transition: opacity 0.2s ease;
    }

    .help-icon:hover {
      opacity: 1;
    }

    .help-tooltip {
      position: absolute;
      background: var(--bg-primary);
      border: 1px solid var(--border-color);
      border-radius: 8px;
      box-shadow: var(--shadow-lg);
      z-index: 10000;
      min-width: 200px;
      font-size: 14px;
      line-height: 1.5;
      animation: fadeInOut 0.3s ease;
    }

    .tooltip-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px 8px 16px;
      border-bottom: 1px solid var(--border-color);
    }

    .tooltip-title {
      margin: 0;
      font-size: 14px;
      font-weight: 600;
      color: var(--text-primary);
    }

    .tooltip-close {
      background: none;
      border: none;
      color: var(--text-muted);
      cursor: pointer;
      padding: 4px;
      border-radius: 4px;
      transition: all 0.2s ease;
    }

    .tooltip-close:hover {
      background: var(--bg-secondary);
      color: var(--text-primary);
    }

    .tooltip-content {
      padding: 12px 16px;
      color: var(--text-secondary);
    }

    .tooltip-content p {
      margin: 0;
    }

    .tooltip-actions {
      padding: 8px 16px 12px 16px;
      display: flex;
      gap: 8px;
      justify-content: flex-end;
    }

    .tooltip-action-btn {
      padding: 6px 12px;
      border: none;
      border-radius: 6px;
      font-size: 12px;
      cursor: pointer;
      transition: all 0.2s ease;
      display: flex;
      align-items: center;
      gap: 4px;
    }

    .tooltip-action-btn.primary {
      background: var(--primary);
      color: white;
    }

    .tooltip-action-btn.primary:hover {
      background: var(--primary-dark);
    }

    .tooltip-action-btn.secondary {
      background: var(--bg-secondary);
      color: var(--text-primary);
      border: 1px solid var(--border-color);
    }

    .tooltip-action-btn.secondary:hover {
      background: var(--bg-tertiary);
    }

    /* Tooltip Positioning */
    .tooltip-top {
      bottom: 100%;
      left: 50%;
      transform: translateX(-50%);
      margin-bottom: 8px;
    }

    .tooltip-bottom {
      top: 100%;
      left: 50%;
      transform: translateX(-50%);
      margin-top: 8px;
    }

    .tooltip-left {
      right: 100%;
      top: 50%;
      transform: translateY(-50%);
      margin-right: 8px;
    }

    .tooltip-right {
      left: 100%;
      top: 50%;
      transform: translateY(-50%);
      margin-left: 8px;
    }

    /* Tooltip Arrows */
    .tooltip-arrow {
      position: absolute;
      width: 0;
      height: 0;
      border: 6px solid transparent;
    }

    .tooltip-top .tooltip-arrow {
      top: 100%;
      left: 50%;
      transform: translateX(-50%);
      border-top-color: var(--bg-primary);
    }

    .tooltip-bottom .tooltip-arrow {
      bottom: 100%;
      left: 50%;
      transform: translateX(-50%);
      border-bottom-color: var(--bg-primary);
    }

    .tooltip-left .tooltip-arrow {
      left: 100%;
      top: 50%;
      transform: translateY(-50%);
      border-left-color: var(--bg-primary);
    }

    .tooltip-right .tooltip-arrow {
      right: 100%;
      top: 50%;
      transform: translateY(-50%);
      border-right-color: var(--bg-primary);
    }

    @keyframes fadeInOut {
      from {
        opacity: 0;
        transform: scale(0.95);
      }
      to {
        opacity: 1;
        transform: scale(1);
      }
    }

    /* Mobile responsiveness */
    @media (max-width: 768px) {
      .help-tooltip {
        max-width: 280px !important;
        font-size: 13px;
      }

      .tooltip-content {
        padding: 10px 12px;
      }

      .tooltip-header {
        padding: 10px 12px 6px 12px;
      }

      .tooltip-actions {
        padding: 6px 12px 10px 12px;
      }
    }
  `],
  animations: []
})
export class HelpTooltipComponent implements OnInit, OnDestroy {
  @Input() title: string = '';
  @Input() content: string = '';
  @Input() placement: 'top' | 'bottom' | 'left' | 'right' = 'top';
  @Input() trigger: 'hover' | 'click' | 'manual' = 'hover';
  @Input() showIcon: boolean = true;
  @Input() closable: boolean = false;
  @Input() maxWidth: string = '300px';
  @Input() actions: TooltipAction[] = [];
  @Input() delay: number = 0;

  @ViewChild('tooltipElement') tooltipElement?: ElementRef;

  visible = false;
  private timeoutId?: number;

  constructor(private elementRef: ElementRef) {}

  ngOnInit(): void {
    if (this.trigger === 'manual') {
      // Manual trigger - controlled externally
    }
  }

  ngOnDestroy(): void {
    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
    }
  }

  showTooltip(): void {
    if (this.trigger !== 'hover') return;
    
    if (this.delay > 0) {
      this.timeoutId = window.setTimeout(() => {
        this.visible = true;
        this.positionTooltip();
      }, this.delay);
    } else {
      this.visible = true;
      setTimeout(() => this.positionTooltip(), 0);
    }
  }

  hideTooltip(): void {
    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
    }
    this.visible = false;
  }

  toggleTooltip(): void {
    if (this.trigger !== 'click') return;
    
    this.visible = !this.visible;
    if (this.visible) {
      setTimeout(() => this.positionTooltip(), 0);
    }
  }

  executeAction(action: TooltipAction): void {
    if (action.callback) {
      action.callback();
    }
    if (action.closeOnClick !== false) {
      this.hideTooltip();
    }
  }

  private positionTooltip(): void {
    if (!this.tooltipElement) return;

    const tooltip = this.tooltipElement.nativeElement;
    const trigger = this.elementRef.nativeElement;
    const rect = trigger.getBoundingClientRect();
    const tooltipRect = tooltip.getBoundingClientRect();
    const viewport = {
      width: window.innerWidth,
      height: window.innerHeight
    };

    // Auto-adjust placement if tooltip would go off-screen
    let finalPlacement = this.placement;

    switch (this.placement) {
      case 'top':
        if (rect.top - tooltipRect.height < 0) {
          finalPlacement = 'bottom';
        }
        break;
      case 'bottom':
        if (rect.bottom + tooltipRect.height > viewport.height) {
          finalPlacement = 'top';
        }
        break;
      case 'left':
        if (rect.left - tooltipRect.width < 0) {
          finalPlacement = 'right';
        }
        break;
      case 'right':
        if (rect.right + tooltipRect.width > viewport.width) {
          finalPlacement = 'left';
        }
        break;
    }

    // Update tooltip class if placement changed
    if (finalPlacement !== this.placement) {
      tooltip.className = tooltip.className.replace(`tooltip-${this.placement}`, `tooltip-${finalPlacement}`);
    }
  }

  // Public methods for manual control
  show(): void {
    this.visible = true;
    setTimeout(() => this.positionTooltip(), 0);
  }

  hide(): void {
    this.visible = false;
  }

  toggle(): void {
    if (this.visible) {
      this.hide();
    } else {
      this.show();
    }
  }
}

export interface TooltipAction {
  label: string;
  icon?: string;
  type?: 'primary' | 'secondary';
  callback?: () => void;
  closeOnClick?: boolean;
}
