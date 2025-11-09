import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-loan-application',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="loan-application">
      <h2>Loan Application</h2>
      <form (ngSubmit)="onSubmit()">
        <div class="form-group">
          <label>Loan Type</label>
          <select [(ngModel)]="formData.loan_type" name="loan_type" required>
            <option value="">Select Loan Type</option>
            <option value="HOME">Home Loan</option>
            <option value="PERSONAL">Personal Loan</option>
            <option value="GOLD">Gold Loan</option>
            <option value="VEHICLE">Vehicle Loan</option>
            <option value="EDUCATION">Education Loan</option>
            <option value="BUSINESS">Business Loan</option>
          </select>
        </div>
        <div class="form-group">
          <label>Loan Amount (₹)</label>
          <input type="number" [(ngModel)]="formData.loan_amount" name="loan_amount" required min="10000">
        </div>
        <div class="form-group">
          <label>Tenure (Months)</label>
          <input type="number" [(ngModel)]="formData.tenure_months" name="tenure_months" required min="6" max="360">
        </div>
        <div class="form-group">
          <label>Loan Purpose</label>
          <textarea [(ngModel)]="formData.loan_purpose" name="loan_purpose"></textarea>
        </div>
        <button type="submit" [disabled]="loading">Submit Application</button>
        <p *ngIf="message" [class]="messageType">{{ message }}</p>
      </form>
    </div>
  `,
  styles: [`
    .loan-application {
      max-width: 600px;
      margin: 0 auto;
      background: white;
      padding: 2rem;
      border-radius: 8px;
      box-shadow: 0 2px 10px rgba(0,0,0,0.1);
    }
    .form-group {
      margin-bottom: 1rem;
    }
    .form-group label {
      display: block;
      margin-bottom: 0.5rem;
      font-weight: 500;
    }
    .form-group input,
    .form-group select,
    .form-group textarea {
      width: 100%;
      padding: 0.75rem;
      border: 1px solid #ddd;
      border-radius: 4px;
      box-sizing: border-box;
    }
    button {
      width: 100%;
      padding: 0.75rem;
      background: #1976d2;
      color: white;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 1rem;
    }
  `]
})
export class LoanApplicationComponent {
  formData = {
    loan_type: '',
    loan_amount: 0,
    tenure_months: 0,
    loan_purpose: ''
  };
  loading = false;
  message = '';
  messageType = '';

  constructor(private http: HttpClient) {}

  onSubmit() {
    const token = localStorage.getItem('token');
    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);
    
    this.loading = true;
    this.http.post(`${environment.apiUrl}/loan/applications/submit`, this.formData, { headers })
      .subscribe({
        next: (response: any) => {
          this.message = response.message || 'Loan application submitted successfully!';
          this.messageType = 'success';
          this.loading = false;
        },
        error: (error) => {
          this.message = error.error?.detail || 'Failed to submit application';
          this.messageType = 'error';
          this.loading = false;
        }
      });
  }
}


