import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-track-application',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="track-container">
      <div class="page-header">
        <button class="back-btn" (click)="goBack()">
          <span class="back-icon">←</span>
        </button>
        <div class="header-content">
          <h1>Track Your Applications</h1>
          <p>Monitor the status and progress of your loan applications.</p>
        </div>
      </div>

      <div class="track-content">
        <div class="applications-section">
          <h2>📋 Your Applications</h2>
          
          <div class="application-card" *ngFor="let app of applications">
            <div class="app-header">
              <div class="app-info">
                <h3>{{ app.loanType }}</h3>
                <p class="app-id">Application ID: {{ app.id }}</p>
              </div>
              <div class="app-status" [class]="'status-' + app.status.toLowerCase()">
                {{ app.status }}
              </div>
            </div>
            
            <div class="app-details">
              <div class="detail-item">
                <span class="label">Amount:</span>
                <span class="value">₹{{ app.amount | number }}</span>
              </div>
              <div class="detail-item">
                <span class="label">Applied Date:</span>
                <span class="value">{{ app.appliedDate }}</span>
              </div>
              <div class="detail-item">
                <span class="label">Expected Closure:</span>
                <span class="value">{{ app.expectedClosure }}</span>
              </div>
            </div>
            
            <div class="progress-section">
              <div class="progress-bar">
                <div class="progress-fill" [style.width.%]="app.progress"></div>
              </div>
              <span class="progress-text">{{ app.progress }}% Complete</span>
            </div>
            
            <div class="app-actions">
              <button class="btn btn-primary">View Details</button>
              <button class="btn btn-secondary">Download Documents</button>
            </div>
          </div>
          
          <div class="empty-state" *ngIf="applications.length === 0">
            <div class="empty-icon">📋</div>
            <h3>No Applications Found</h3>
            <p>You haven't submitted any loan applications yet.</p>
            <button class="btn btn-primary" (click)="startNewApplication()">
              Apply for New Loan
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .track-container {
      padding: 20px;
      max-width: 1200px;
      margin: 0 auto;
    }

    .page-header {
      display: flex;
      align-items: center;
      margin-bottom: 30px;
      gap: 15px;
    }

    .back-btn {
      background: #f8f9fa;
      border: 1px solid #dee2e6;
      border-radius: 8px;
      padding: 10px 15px;
      cursor: pointer;
      font-size: 18px;
      transition: all 0.2s;
    }

    .back-btn:hover {
      background: #e9ecef;
    }

    .header-content h1 {
      margin: 0 0 5px 0;
      color: #2c3e50;
      font-size: 28px;
    }

    .header-content p {
      margin: 0;
      color: #6c757d;
      font-size: 16px;
    }

    .track-content {
      background: white;
      border-radius: 12px;
      padding: 30px;
      box-shadow: 0 2px 10px rgba(0,0,0,0.1);
    }

    .applications-section h2 {
      margin-bottom: 20px;
      color: #2c3e50;
    }

    .application-card {
      border: 1px solid #dee2e6;
      border-radius: 12px;
      padding: 20px;
      margin-bottom: 20px;
      transition: all 0.3s;
    }

    .application-card:hover {
      box-shadow: 0 4px 15px rgba(0,0,0,0.1);
    }

    .app-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 15px;
    }

    .app-info h3 {
      margin: 0 0 5px 0;
      color: #2c3e50;
      font-size: 18px;
    }

    .app-id {
      margin: 0;
      color: #6c757d;
      font-size: 14px;
    }

    .app-status {
      padding: 6px 12px;
      border-radius: 20px;
      font-size: 12px;
      font-weight: 600;
      text-transform: uppercase;
    }

    .status-pending {
      background: #fff3cd;
      color: #856404;
    }

    .status-approved {
      background: #d4edda;
      color: #155724;
    }

    .status-rejected {
      background: #f8d7da;
      color: #721c24;
    }

    .app-details {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 15px;
      margin-bottom: 20px;
    }

    .detail-item {
      display: flex;
      flex-direction: column;
    }

    .label {
      font-size: 12px;
      color: #6c757d;
      margin-bottom: 2px;
    }

    .value {
      font-weight: 500;
      color: #2c3e50;
    }

    .progress-section {
      margin-bottom: 20px;
    }

    .progress-bar {
      background: #e9ecef;
      border-radius: 10px;
      height: 8px;
      margin-bottom: 5px;
    }

    .progress-fill {
      background: #007bff;
      height: 100%;
      border-radius: 10px;
      transition: width 0.3s;
    }

    .progress-text {
      font-size: 12px;
      color: #6c757d;
    }

    .app-actions {
      display: flex;
      gap: 10px;
    }

    .btn {
      padding: 8px 16px;
      border: none;
      border-radius: 6px;
      cursor: pointer;
      font-size: 14px;
      font-weight: 500;
      transition: all 0.2s;
    }

    .btn-primary {
      background: #007bff;
      color: white;
    }

    .btn-primary:hover {
      background: #0056b3;
    }

    .btn-secondary {
      background: #6c757d;
      color: white;
    }

    .btn-secondary:hover {
      background: #545b62;
    }

    .empty-state {
      text-align: center;
      padding: 60px 20px;
    }

    .empty-icon {
      font-size: 64px;
      margin-bottom: 20px;
    }

    .empty-state h3 {
      margin: 0 0 10px 0;
      color: #2c3e50;
    }

    .empty-state p {
      margin: 0 0 30px 0;
      color: #6c757d;
    }
  `]
})
export class TrackApplicationComponent {
  applications = [
    {
      id: 'LN001234',
      loanType: 'Home Loan',
      amount: 2500000,
      appliedDate: '2024-01-15',
      expectedClosure: '2024-02-15',
      status: 'Pending',
      progress: 65
    },
    {
      id: 'LN001235',
      loanType: 'Personal Loan',
      amount: 500000,
      appliedDate: '2024-01-10',
      expectedClosure: '2024-02-10',
      status: 'Approved',
      progress: 100
    }
  ];

  constructor(private router: Router) {}

  goBack(): void {
    this.router.navigate(['/applicant/dashboard']);
  }

  startNewApplication(): void {
    this.router.navigate(['/applicant/apply-loan']);
  }
}
