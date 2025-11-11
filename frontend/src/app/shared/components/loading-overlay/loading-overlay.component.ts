import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { LoadingService, LoadingState } from '../../../core/services/loading.service';

@Component({
  selector: 'app-loading-overlay',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div *ngIf="loadingStates.length > 0" class="loading-overlay" [@fadeIn]>
      <div class="loading-content">
        <div *ngFor="let state of loadingStates" class="loading-item">
          
          <!-- Spinner Loading -->
          <div *ngIf="state.type === 'spinner'" class="spinner-container">
            <div class="modern-spinner">
              <div class="spinner-ring"></div>
              <div class="spinner-ring"></div>
              <div class="spinner-ring"></div>
            </div>
            <p class="loading-message">{{ state.message || 'Loading...' }}</p>
          </div>

          <!-- Progress Loading -->
          <div *ngIf="state.type === 'progress'" class="progress-container">
            <div class="progress-circle">
              <svg class="progress-svg" viewBox="0 0 100 100">
                <circle 
                  class="progress-bg" 
                  cx="50" 
                  cy="50" 
                  r="45"
                ></circle>
                <circle 
                  class="progress-fill" 
                  cx="50" 
                  cy="50" 
                  r="45"
                  [style.stroke-dasharray]="getCircumference()"
                  [style.stroke-dashoffset]="getStrokeDashoffset(state.progress || 0)"
                ></circle>
              </svg>
              <div class="progress-text">
                <span class="progress-percentage">{{ state.progress || 0 }}%</span>
              </div>
            </div>
            <p class="loading-message">{{ state.message || 'Processing...' }}</p>
          </div>

        </div>
      </div>
    </div>
  `,
  styles: [`
    .loading-overlay {
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: rgba(0, 0, 0, 0.7);
      backdrop-filter: blur(4px);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 10000;
      animation: fadeIn 0.3s ease-out;
    }

    .loading-content {
      background: var(--bg-primary);
      border-radius: 16px;
      padding: 40px;
      box-shadow: var(--shadow-lg);
      text-align: center;
      min-width: 300px;
      border: 1px solid var(--border-color);
    }

    .loading-item {
      margin-bottom: 20px;
    }

    .loading-item:last-child {
      margin-bottom: 0;
    }

    /* Modern Spinner */
    .spinner-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 20px;
    }

    .modern-spinner {
      position: relative;
      width: 60px;
      height: 60px;
    }

    .spinner-ring {
      position: absolute;
      width: 100%;
      height: 100%;
      border: 3px solid transparent;
      border-radius: 50%;
      animation: spin 2s linear infinite;
    }

    .spinner-ring:nth-child(1) {
      border-top-color: var(--primary);
      animation-delay: 0s;
    }

    .spinner-ring:nth-child(2) {
      border-right-color: var(--success);
      animation-delay: 0.3s;
      animation-direction: reverse;
    }

    .spinner-ring:nth-child(3) {
      border-bottom-color: var(--warning);
      animation-delay: 0.6s;
    }

    /* Progress Circle */
    .progress-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 20px;
    }

    .progress-circle {
      position: relative;
      width: 80px;
      height: 80px;
    }

    .progress-svg {
      width: 100%;
      height: 100%;
      transform: rotate(-90deg);
    }

    .progress-bg {
      fill: none;
      stroke: var(--border-color);
      stroke-width: 3;
    }

    .progress-fill {
      fill: none;
      stroke: var(--primary);
      stroke-width: 3;
      stroke-linecap: round;
      transition: stroke-dashoffset 0.3s ease;
    }

    .progress-text {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
    }

    .progress-percentage {
      font-size: 16px;
      font-weight: 600;
      color: var(--text-primary);
    }

    .loading-message {
      color: var(--text-secondary);
      font-size: 14px;
      margin: 0;
      font-weight: 500;
    }

    @keyframes spin {
      0% { transform: rotate(0deg); }
      100% { transform: rotate(360deg); }
    }

    @keyframes fadeIn {
      from {
        opacity: 0;
        transform: scale(0.9);
      }
      to {
        opacity: 1;
        transform: scale(1);
      }
    }

    /* Dark theme adjustments */
    .dark-theme .loading-overlay {
      background: rgba(0, 0, 0, 0.8);
    }

    /* Mobile responsiveness */
    @media (max-width: 768px) {
      .loading-content {
        margin: 20px;
        padding: 30px 20px;
        min-width: auto;
        width: calc(100% - 40px);
      }

      .modern-spinner {
        width: 50px;
        height: 50px;
      }

      .progress-circle {
        width: 70px;
        height: 70px;
      }
    }
  `],
  animations: []
})
export class LoadingOverlayComponent implements OnInit, OnDestroy {
  loadingStates: LoadingState[] = [];
  private subscription?: Subscription;

  constructor(private loadingService: LoadingService) {}

  ngOnInit(): void {
    this.subscription = this.loadingService.getLoadingStates().subscribe(
      states => this.loadingStates = states
    );
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  getCircumference(): string {
    const radius = 45;
    return `${2 * Math.PI * radius}`;
  }

  getStrokeDashoffset(progress: number): string {
    const radius = 45;
    const circumference = 2 * Math.PI * radius;
    const offset = circumference - (progress / 100) * circumference;
    return `${offset}`;
  }
}
