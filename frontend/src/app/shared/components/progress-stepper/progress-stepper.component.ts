import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface Step {
  id: string;
  title: string;
  description?: string;
  icon?: string;
  status: 'pending' | 'active' | 'completed' | 'error';
  optional?: boolean;
}

@Component({
  selector: 'app-progress-stepper',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="progress-stepper" [class.vertical]="orientation === 'vertical'">
      <div 
        *ngFor="let step of steps; let i = index; trackBy: trackByFn"
        class="step-container"
        [class.active]="step.status === 'active'"
        [class.completed]="step.status === 'completed'"
        [class.error]="step.status === 'error'"
        [class.clickable]="allowNavigation"
        (click)="onStepClick(step, i)"
      >
        
        <!-- Step Circle -->
        <div class="step-circle">
          <div class="step-number" *ngIf="step.status === 'pending' || step.status === 'active'">
            <i *ngIf="step.icon" class="fas" [ngClass]="step.icon"></i>
            <span *ngIf="!step.icon">{{ i + 1 }}</span>
          </div>
          
          <div class="step-icon" *ngIf="step.status === 'completed'">
            <i class="fas fa-check"></i>
          </div>
          
          <div class="step-icon" *ngIf="step.status === 'error'">
            <i class="fas fa-times"></i>
          </div>
          
          <!-- Animated pulse for active step -->
          <div *ngIf="step.status === 'active'" class="step-pulse"></div>
        </div>

        <!-- Step Content -->
        <div class="step-content">
          <h6 class="step-title">
            {{ step.title }}
            <span *ngIf="step.optional" class="optional-badge">Optional</span>
          </h6>
          <p *ngIf="step.description" class="step-description">{{ step.description }}</p>
        </div>

        <!-- Connector Line -->
        <div 
          *ngIf="i < steps.length - 1" 
          class="step-connector"
          [class.completed]="step.status === 'completed'"
        ></div>
      </div>
    </div>
  `,
  styles: [`
    .progress-stepper {
      display: flex;
      align-items: flex-start;
      gap: 0;
      position: relative;
    }

    .progress-stepper.vertical {
      flex-direction: column;
      align-items: stretch;
    }

    .step-container {
      display: flex;
      align-items: center;
      position: relative;
      flex: 1;
      min-width: 0;
      transition: all 0.3s ease;
    }

    .step-container.clickable {
      cursor: pointer;
    }

    .step-container.clickable:hover .step-circle {
      transform: scale(1.1);
    }

    .vertical .step-container {
      flex-direction: row;
      align-items: flex-start;
      margin-bottom: 30px;
    }

    .vertical .step-container:last-child {
      margin-bottom: 0;
    }

    /* Step Circle */
    .step-circle {
      position: relative;
      width: 40px;
      height: 40px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--bg-secondary);
      border: 2px solid var(--border-color);
      transition: all 0.3s ease;
      z-index: 2;
      flex-shrink: 0;
    }

    .step-container.active .step-circle {
      background: var(--primary);
      border-color: var(--primary);
      color: white;
      box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.2);
    }

    .step-container.completed .step-circle {
      background: var(--success);
      border-color: var(--success);
      color: white;
    }

    .step-container.error .step-circle {
      background: var(--danger);
      border-color: var(--danger);
      color: white;
    }

    .step-number,
    .step-icon {
      font-size: 14px;
      font-weight: 600;
    }

    .step-pulse {
      position: absolute;
      top: -2px;
      left: -2px;
      right: -2px;
      bottom: -2px;
      border-radius: 50%;
      border: 2px solid var(--primary);
      animation: pulse 2s infinite;
    }

    /* Step Content */
    .step-content {
      margin-left: 16px;
      flex: 1;
      min-width: 0;
    }

    .vertical .step-content {
      margin-left: 16px;
      margin-top: 0;
    }

    .step-title {
      font-size: 14px;
      font-weight: 600;
      margin: 0 0 4px 0;
      color: var(--text-primary);
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .step-container.active .step-title {
      color: var(--primary);
    }

    .step-container.completed .step-title {
      color: var(--success);
    }

    .step-container.error .step-title {
      color: var(--danger);
    }

    .step-description {
      font-size: 12px;
      color: var(--text-secondary);
      margin: 0;
      line-height: 1.4;
    }

    .optional-badge {
      background: var(--warning);
      color: white;
      font-size: 10px;
      padding: 2px 6px;
      border-radius: 10px;
      font-weight: 500;
    }

    /* Connector Line */
    .step-connector {
      position: absolute;
      background: var(--border-color);
      transition: all 0.3s ease;
      z-index: 1;
    }

    /* Horizontal connector */
    .progress-stepper:not(.vertical) .step-connector {
      top: 50%;
      left: 40px;
      right: -16px;
      height: 2px;
      transform: translateY(-50%);
    }

    /* Vertical connector */
    .vertical .step-connector {
      top: 40px;
      left: 19px;
      width: 2px;
      height: 30px;
    }

    .step-connector.completed {
      background: var(--success);
    }

    @keyframes pulse {
      0% {
        transform: scale(1);
        opacity: 1;
      }
      50% {
        transform: scale(1.1);
        opacity: 0.7;
      }
      100% {
        transform: scale(1);
        opacity: 1;
      }
    }

    /* Mobile responsiveness */
    @media (max-width: 768px) {
      .progress-stepper:not(.vertical) {
        flex-direction: column;
        align-items: stretch;
      }

      .progress-stepper:not(.vertical) .step-container {
        flex-direction: row;
        align-items: flex-start;
        margin-bottom: 20px;
      }

      .progress-stepper:not(.vertical) .step-container:last-child {
        margin-bottom: 0;
      }

      .progress-stepper:not(.vertical) .step-connector {
        top: 40px;
        left: 19px;
        width: 2px;
        height: 20px;
        right: auto;
      }

      .step-circle {
        width: 36px;
        height: 36px;
      }

      .step-number,
      .step-icon {
        font-size: 12px;
      }
    }
  `]
})
export class ProgressStepperComponent {
  @Input() steps: Step[] = [];
  @Input() orientation: 'horizontal' | 'vertical' = 'horizontal';
  @Input() allowNavigation: boolean = false;
  
  @Output() stepClick = new EventEmitter<{ step: Step; index: number }>();

  trackByFn(index: number, step: Step): string {
    return step.id;
  }

  onStepClick(step: Step, index: number): void {
    if (this.allowNavigation) {
      this.stepClick.emit({ step, index });
    }
  }
}
