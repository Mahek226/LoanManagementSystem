import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-skeleton-loader',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="skeleton-loader" [ngClass]="type" [style.width]="width" [style.height]="height">
      <div class="skeleton-shimmer"></div>
    </div>
  `,
  styles: [`
    .skeleton-loader {
      background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
      background-size: 200% 100%;
      animation: shimmer 1.5s infinite;
      border-radius: 4px;
      position: relative;
      overflow: hidden;
    }

    .dark-theme .skeleton-loader {
      background: linear-gradient(90deg, #374151 25%, #4b5563 50%, #374151 75%);
      background-size: 200% 100%;
    }

    .skeleton-shimmer {
      position: absolute;
      top: 0;
      left: -100%;
      width: 100%;
      height: 100%;
      background: linear-gradient(90deg, transparent, rgba(255,255,255,0.4), transparent);
      animation: shimmer-move 1.5s infinite;
    }

    .dark-theme .skeleton-shimmer {
      background: linear-gradient(90deg, transparent, rgba(255,255,255,0.1), transparent);
    }

    @keyframes shimmer {
      0% { background-position: -200% 0; }
      100% { background-position: 200% 0; }
    }

    @keyframes shimmer-move {
      0% { left: -100%; }
      100% { left: 100%; }
    }

    .skeleton-loader.text {
      height: 1rem;
      border-radius: 4px;
    }

    .skeleton-loader.title {
      height: 1.5rem;
      border-radius: 4px;
    }

    .skeleton-loader.card {
      height: 200px;
      border-radius: 8px;
    }

    .skeleton-loader.avatar {
      width: 40px;
      height: 40px;
      border-radius: 50%;
    }

    .skeleton-loader.button {
      height: 40px;
      border-radius: 6px;
    }

    .skeleton-loader.chart {
      height: 300px;
      border-radius: 8px;
    }
  `]
})
export class SkeletonLoaderComponent {
  @Input() type: 'text' | 'title' | 'card' | 'avatar' | 'button' | 'chart' = 'text';
  @Input() width: string = '100%';
  @Input() height: string = 'auto';
}
