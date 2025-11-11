import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LoanIdService } from '../../../core/services/loan-id.service';

@Component({
  selector: 'app-loan-id-display',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="loan-id-display" [ngClass]="size">
      
      <!-- Standard Display -->
      <div *ngIf="displayType === 'standard'" class="standard-display">
        <div class="loan-id-container">
          <span class="loan-id-prefix">{{ formattedId.parts.prefix }}</span>
          <span class="loan-id-number">{{ formattedId.parts.number }}</span>
        </div>
        <div *ngIf="showLabel" class="loan-id-label">{{ label }}</div>
      </div>

      <!-- Badge Display -->
      <div *ngIf="displayType === 'badge'" class="badge-display">
        <span class="loan-id-badge" [ngClass]="badgeColor">
          <i class="fas fa-file-contract me-1"></i>
          {{ formattedId.displayId }}
        </span>
      </div>

      <!-- Card Display -->
      <div *ngIf="displayType === 'card'" class="card-display">
        <div class="loan-id-card">
          <div class="card-header">
            <i class="fas fa-file-contract"></i>
            <span>{{ label }}</span>
          </div>
          <div class="card-body">
            <div class="loan-id-large">
              <span class="prefix">{{ formattedId.parts.prefix }}</span>
              <span class="separator">-</span>
              <span class="number">{{ formattedId.parts.number }}</span>
            </div>
          </div>
          <div class="card-footer" *ngIf="showCopyButton">
            <button class="copy-btn" (click)="copyToClipboard()" [title]="'Copy ' + formattedId.displayId">
              <i class="fas" [ngClass]="copied ? 'fa-check' : 'fa-copy'"></i>
              {{ copied ? 'Copied!' : 'Copy' }}
            </button>
          </div>
        </div>
      </div>

      <!-- QR Code Display -->
      <div *ngIf="displayType === 'qr'" class="qr-display">
        <div class="qr-container">
          <div class="qr-placeholder">
            <i class="fas fa-qrcode"></i>
            <div class="qr-text">QR Code</div>
            <div class="qr-id">{{ formattedId.displayId }}</div>
          </div>
        </div>
      </div>

      <!-- Inline Display -->
      <div *ngIf="displayType === 'inline'" class="inline-display">
        <span class="inline-label" *ngIf="showLabel">{{ label }}:</span>
        <span class="inline-id">{{ formattedId.displayId }}</span>
        <button *ngIf="showCopyButton" class="inline-copy-btn" (click)="copyToClipboard()" [title]="'Copy ' + formattedId.displayId">
          <i class="fas" [ngClass]="copied ? 'fa-check' : 'fa-copy'"></i>
        </button>
      </div>

    </div>
  `,
  styles: [`
    .loan-id-display {
      display: inline-block;
    }

    /* Size Variants */
    .loan-id-display.small {
      font-size: 0.875rem;
    }

    .loan-id-display.medium {
      font-size: 1rem;
    }

    .loan-id-display.large {
      font-size: 1.25rem;
    }

    /* Standard Display */
    .standard-display {
      text-align: center;
    }

    .loan-id-container {
      font-family: 'Courier New', monospace;
      font-weight: 600;
      letter-spacing: 1px;
    }

    .loan-id-prefix {
      color: var(--primary);
      font-weight: 700;
    }

    .loan-id-number {
      color: var(--text-primary);
      margin-left: 2px;
    }

    .loan-id-label {
      font-size: 0.75em;
      color: var(--text-secondary);
      margin-top: 4px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    /* Badge Display */
    .loan-id-badge {
      display: inline-flex;
      align-items: center;
      padding: 6px 12px;
      border-radius: 20px;
      font-size: 0.875rem;
      font-weight: 500;
      font-family: 'Courier New', monospace;
      letter-spacing: 0.5px;
    }

    .loan-id-badge.primary {
      background: var(--primary);
      color: white;
    }

    .loan-id-badge.success {
      background: var(--success);
      color: white;
    }

    .loan-id-badge.info {
      background: var(--info);
      color: white;
    }

    .loan-id-badge.warning {
      background: var(--warning);
      color: white;
    }

    .loan-id-badge.secondary {
      background: var(--bg-secondary);
      color: var(--text-primary);
      border: 1px solid var(--border-color);
    }

    /* Card Display */
    .loan-id-card {
      background: var(--bg-primary);
      border: 1px solid var(--border-color);
      border-radius: 12px;
      overflow: hidden;
      box-shadow: var(--shadow-sm);
      min-width: 280px;
    }

    .card-header {
      background: linear-gradient(135deg, var(--primary), var(--primary-dark));
      color: white;
      padding: 12px 16px;
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: 600;
      font-size: 0.875rem;
    }

    .card-body {
      padding: 20px;
      text-align: center;
    }

    .loan-id-large {
      font-family: 'Courier New', monospace;
      font-size: 1.5rem;
      font-weight: 700;
      letter-spacing: 2px;
    }

    .loan-id-large .prefix {
      color: var(--primary);
    }

    .loan-id-large .separator {
      color: var(--text-muted);
      margin: 0 4px;
    }

    .loan-id-large .number {
      color: var(--text-primary);
    }

    .card-footer {
      background: var(--bg-secondary);
      padding: 12px 16px;
      text-align: center;
      border-top: 1px solid var(--border-color);
    }

    .copy-btn {
      background: var(--primary);
      color: white;
      border: none;
      padding: 6px 12px;
      border-radius: 6px;
      font-size: 0.75rem;
      cursor: pointer;
      transition: all 0.2s ease;
      display: flex;
      align-items: center;
      gap: 4px;
      margin: 0 auto;
    }

    .copy-btn:hover {
      background: var(--primary-dark);
      transform: translateY(-1px);
    }

    .copy-btn:active {
      transform: translateY(0);
    }

    /* QR Display */
    .qr-container {
      text-align: center;
    }

    .qr-placeholder {
      width: 120px;
      height: 120px;
      background: var(--bg-secondary);
      border: 2px dashed var(--border-color);
      border-radius: 8px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      margin: 0 auto;
    }

    .qr-placeholder i {
      font-size: 2rem;
      color: var(--text-muted);
      margin-bottom: 8px;
    }

    .qr-text {
      font-size: 0.75rem;
      color: var(--text-secondary);
      margin-bottom: 4px;
    }

    .qr-id {
      font-size: 0.625rem;
      color: var(--text-muted);
      font-family: 'Courier New', monospace;
    }

    /* Inline Display */
    .inline-display {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .inline-label {
      color: var(--text-secondary);
      font-weight: 500;
    }

    .inline-id {
      font-family: 'Courier New', monospace;
      font-weight: 600;
      color: var(--primary);
      letter-spacing: 0.5px;
    }

    .inline-copy-btn {
      background: none;
      border: none;
      color: var(--text-muted);
      cursor: pointer;
      padding: 4px;
      border-radius: 4px;
      transition: all 0.2s ease;
      font-size: 0.75rem;
    }

    .inline-copy-btn:hover {
      background: var(--bg-secondary);
      color: var(--text-primary);
    }

    /* Animations */
    .loan-id-display {
      animation: fadeIn 0.3s ease-out;
    }

    @keyframes fadeIn {
      from {
        opacity: 0;
        transform: translateY(10px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    /* Responsive */
    @media (max-width: 768px) {
      .loan-id-card {
        min-width: auto;
        width: 100%;
      }

      .loan-id-large {
        font-size: 1.25rem;
        letter-spacing: 1px;
      }
    }
  `]
})
export class LoanIdDisplayComponent implements OnInit {
  @Input() internalLoanId!: number;
  @Input() displayType: 'standard' | 'badge' | 'card' | 'qr' | 'inline' = 'standard';
  @Input() size: 'small' | 'medium' | 'large' = 'medium';
  @Input() label: string = 'Loan ID';
  @Input() showLabel: boolean = true;
  @Input() showCopyButton: boolean = false;
  @Input() badgeColor: 'primary' | 'success' | 'info' | 'warning' | 'secondary' = 'primary';

  formattedId: any = {};
  copied: boolean = false;

  constructor(private loanIdService: LoanIdService) {}

  ngOnInit(): void {
    if (this.internalLoanId) {
      this.formattedId = this.loanIdService.formatLoanIdForDisplay(this.internalLoanId);
    }
  }

  copyToClipboard(): void {
    if (navigator.clipboard && this.formattedId.displayId) {
      navigator.clipboard.writeText(this.formattedId.displayId).then(() => {
        this.copied = true;
        setTimeout(() => {
          this.copied = false;
        }, 2000);
      }).catch(err => {
        console.error('Failed to copy: ', err);
        this.fallbackCopyTextToClipboard(this.formattedId.displayId);
      });
    } else {
      this.fallbackCopyTextToClipboard(this.formattedId.displayId);
    }
  }

  private fallbackCopyTextToClipboard(text: string): void {
    const textArea = document.createElement('textarea');
    textArea.value = text;
    textArea.style.top = '0';
    textArea.style.left = '0';
    textArea.style.position = 'fixed';

    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();

    try {
      const successful = document.execCommand('copy');
      if (successful) {
        this.copied = true;
        setTimeout(() => {
          this.copied = false;
        }, 2000);
      }
    } catch (err) {
      console.error('Fallback: Oops, unable to copy', err);
    }

    document.body.removeChild(textArea);
  }
}
