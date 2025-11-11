import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LoanIdService } from '../../../core/services/loan-id.service';
import { LoanIdDisplayComponent } from '../loan-id-display/loan-id-display.component';

@Component({
  selector: 'app-loan-id-demo',
  standalone: true,
  imports: [CommonModule, LoanIdDisplayComponent],
  template: `
    <div class="loan-id-demo">
      <div class="container-fluid px-4 py-4">
        
        <!-- Header -->
        <div class="row mb-4">
          <div class="col-12">
            <h2 class="mb-3">
              <i class="fas fa-id-card me-2 text-primary"></i>
              Loan ID System Demo
            </h2>
            <p class="text-muted">
              This demonstrates how loan IDs are generated and displayed throughout the application.
              Internal loan IDs are converted to user-friendly display IDs.
            </p>
          </div>
        </div>

        <!-- Display Type Examples -->
        <div class="row mb-5">
          <div class="col-12">
            <h4 class="mb-3">Display Types</h4>
            <div class="row g-4">
              
              <!-- Standard Display -->
              <div class="col-md-6 col-lg-4">
                <div class="demo-card">
                  <h6 class="demo-title">Standard Display</h6>
                  <div class="demo-content">
                    <app-loan-id-display 
                      [internalLoanId]="12345"
                      displayType="standard"
                      size="large"
                      label="Loan Application ID">
                    </app-loan-id-display>
                  </div>
                  <small class="text-muted">Used in headers and main displays</small>
                </div>
              </div>

              <!-- Badge Display -->
              <div class="col-md-6 col-lg-4">
                <div class="demo-card">
                  <h6 class="demo-title">Badge Display</h6>
                  <div class="demo-content">
                    <app-loan-id-display 
                      [internalLoanId]="12345"
                      displayType="badge"
                      badgeColor="primary"
                      [showLabel]="false">
                    </app-loan-id-display>
                    <br><br>
                    <app-loan-id-display 
                      [internalLoanId]="12345"
                      displayType="badge"
                      badgeColor="success"
                      [showLabel]="false">
                    </app-loan-id-display>
                  </div>
                  <small class="text-muted">Used in lists and status displays</small>
                </div>
              </div>

              <!-- Inline Display -->
              <div class="col-md-6 col-lg-4">
                <div class="demo-card">
                  <h6 class="demo-title">Inline Display</h6>
                  <div class="demo-content">
                    <app-loan-id-display 
                      [internalLoanId]="12345"
                      displayType="inline"
                      size="medium"
                      [showCopyButton]="true">
                    </app-loan-id-display>
                  </div>
                  <small class="text-muted">Used within text and forms</small>
                </div>
              </div>

              <!-- Card Display -->
              <div class="col-md-6 col-lg-4">
                <div class="demo-card">
                  <h6 class="demo-title">Card Display</h6>
                  <div class="demo-content">
                    <app-loan-id-display 
                      [internalLoanId]="12345"
                      displayType="card"
                      label="Your Loan Reference"
                      [showCopyButton]="true">
                    </app-loan-id-display>
                  </div>
                  <small class="text-muted">Used for prominent displays</small>
                </div>
              </div>

              <!-- QR Code Display -->
              <div class="col-md-6 col-lg-4">
                <div class="demo-card">
                  <h6 class="demo-title">QR Code Display</h6>
                  <div class="demo-content">
                    <app-loan-id-display 
                      [internalLoanId]="12345"
                      displayType="qr">
                    </app-loan-id-display>
                  </div>
                  <small class="text-muted">Used for mobile scanning</small>
                </div>
              </div>

            </div>
          </div>
        </div>

        <!-- Generation Examples -->
        <div class="row mb-5">
          <div class="col-12">
            <h4 class="mb-3">ID Generation Examples</h4>
            <div class="table-responsive">
              <table class="table table-striped">
                <thead>
                  <tr>
                    <th>Internal ID</th>
                    <th>Display ID</th>
                    <th>Formatted</th>
                    <th>Usage Context</th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let example of generationExamples">
                    <td>
                      <code>{{ example.internal }}</code>
                    </td>
                    <td>
                      <strong class="text-primary">{{ example.display }}</strong>
                    </td>
                    <td>
                      <span class="badge bg-secondary">{{ example.formatted }}</span>
                    </td>
                    <td class="text-muted">{{ example.context }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- Usage Guidelines -->
        <div class="row mb-5">
          <div class="col-12">
            <h4 class="mb-3">Usage Guidelines</h4>
            <div class="row g-4">
              
              <div class="col-md-6">
                <div class="guideline-card">
                  <div class="guideline-header">
                    <i class="fas fa-eye text-success"></i>
                    <h6>What Users See</h6>
                  </div>
                  <ul class="guideline-list">
                    <li><strong>LNID123456</strong> - Easy to read and remember</li>
                    <li><strong>Consistent format</strong> - Always LNID + 6 digits</li>
                    <li><strong>Copy functionality</strong> - Easy to share and reference</li>
                    <li><strong>Visual hierarchy</strong> - Different display types for different contexts</li>
                  </ul>
                </div>
              </div>

              <div class="col-md-6">
                <div class="guideline-card">
                  <div class="guideline-header">
                    <i class="fas fa-shield-alt text-primary"></i>
                    <h6>Security Benefits</h6>
                  </div>
                  <ul class="guideline-list">
                    <li><strong>No internal ID exposure</strong> - Database IDs remain hidden</li>
                    <li><strong>Consistent generation</strong> - Same internal ID always generates same display ID</li>
                    <li><strong>Non-sequential</strong> - Cannot guess other loan IDs</li>
                    <li><strong>Professional appearance</strong> - Looks like real banking system</li>
                  </ul>
                </div>
              </div>

            </div>
          </div>
        </div>

        <!-- Implementation Status -->
        <div class="row">
          <div class="col-12">
            <h4 class="mb-3">Implementation Status</h4>
            <div class="row g-3">
              
              <div class="col-md-4">
                <div class="status-card implemented">
                  <i class="fas fa-check-circle"></i>
                  <h6>Applicant Dashboard</h6>
                  <p>Shows loan IDs in application lists</p>
                </div>
              </div>

              <div class="col-md-4">
                <div class="status-card implemented">
                  <i class="fas fa-check-circle"></i>
                  <h6>Loan Officer</h6>
                  <p>Document verification with loan ID display</p>
                </div>
              </div>

              <div class="col-md-4">
                <div class="status-card implemented">
                  <i class="fas fa-check-circle"></i>
                  <h6>Compliance Officer</h6>
                  <p>Comprehensive review with loan ID badges</p>
                </div>
              </div>

            </div>
          </div>
        </div>

      </div>
    </div>
  `,
  styles: [`
    .loan-id-demo {
      background: var(--bg-secondary);
      min-height: 100vh;
    }

    .demo-card {
      background: var(--bg-primary);
      border: 1px solid var(--border-color);
      border-radius: 12px;
      padding: 20px;
      height: 100%;
      text-align: center;
    }

    .demo-title {
      color: var(--text-primary);
      font-weight: 600;
      margin-bottom: 16px;
    }

    .demo-content {
      margin: 20px 0;
      min-height: 80px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-direction: column;
      gap: 10px;
    }

    .guideline-card {
      background: var(--bg-primary);
      border: 1px solid var(--border-color);
      border-radius: 12px;
      padding: 20px;
      height: 100%;
    }

    .guideline-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 16px;
    }

    .guideline-header i {
      font-size: 1.25rem;
    }

    .guideline-header h6 {
      margin: 0;
      color: var(--text-primary);
      font-weight: 600;
    }

    .guideline-list {
      list-style: none;
      padding: 0;
      margin: 0;
    }

    .guideline-list li {
      padding: 8px 0;
      border-bottom: 1px solid var(--border-color);
      color: var(--text-secondary);
      line-height: 1.4;
    }

    .guideline-list li:last-child {
      border-bottom: none;
    }

    .status-card {
      background: var(--bg-primary);
      border: 1px solid var(--border-color);
      border-radius: 12px;
      padding: 20px;
      text-align: center;
      transition: all 0.3s ease;
    }

    .status-card.implemented {
      border-color: var(--success);
      background: rgba(16, 185, 129, 0.05);
    }

    .status-card i {
      font-size: 2rem;
      margin-bottom: 12px;
    }

    .status-card.implemented i {
      color: var(--success);
    }

    .status-card h6 {
      color: var(--text-primary);
      font-weight: 600;
      margin-bottom: 8px;
    }

    .status-card p {
      color: var(--text-secondary);
      margin: 0;
      font-size: 0.875rem;
    }

    .table {
      background: var(--bg-primary);
    }

    .table th {
      background: var(--bg-secondary);
      color: var(--text-primary);
      font-weight: 600;
      border: none;
    }

    .table td {
      color: var(--text-secondary);
      border-color: var(--border-color);
    }

    /* Animations */
    .demo-card,
    .guideline-card,
    .status-card {
      animation: fadeInUp 0.6s ease-out;
    }

    @keyframes fadeInUp {
      from {
        opacity: 0;
        transform: translateY(20px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    /* Hover Effects */
    .demo-card:hover,
    .guideline-card:hover,
    .status-card:hover {
      transform: translateY(-2px);
      box-shadow: var(--shadow-lg);
    }
  `]
})
export class LoanIdDemoComponent implements OnInit {
  generationExamples: any[] = [];

  constructor(private loanIdService: LoanIdService) {}

  ngOnInit(): void {
    this.generateExamples();
  }

  private generateExamples(): void {
    const contexts = [
      'Personal Loan Application',
      'Home Loan Application', 
      'Vehicle Loan Application',
      'Education Loan Application',
      'Business Loan Application',
      'Loan Against Property',
      'Credit Card Application',
      'Overdraft Facility',
      'Gold Loan Application',
      'Agricultural Loan'
    ];

    this.generationExamples = [];
    
    for (let i = 1; i <= 10; i++) {
      const internalId = i * 1000 + Math.floor(Math.random() * 999);
      const formatted = this.loanIdService.formatLoanIdForDisplay(internalId);
      
      this.generationExamples.push({
        internal: internalId,
        display: formatted.displayId,
        formatted: formatted.formatted,
        context: contexts[i - 1]
      });
    }
  }
}
