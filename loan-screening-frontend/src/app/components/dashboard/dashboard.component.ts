import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="dashboard">
      <h2>Welcome, {{ user?.username }}!</h2>
      <div class="dashboard-grid">
        <div class="card" *ngIf="user?.role === 'APPLICANT'">
          <h3>My Loans</h3>
          <p>View and manage your loan applications</p>
          <button (click)="navigateTo('/loan-application')">Apply for Loan</button>
        </div>
        <div class="card" *ngIf="user?.role === 'APPLICANT'">
          <h3>Documents</h3>
          <p>Upload and manage your documents</p>
          <button (click)="navigateTo('/document-upload')">Upload Documents</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .dashboard {
      max-width: 1200px;
      margin: 0 auto;
    }
    .dashboard-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: 2rem;
      margin-top: 2rem;
    }
    .card {
      background: white;
      padding: 2rem;
      border-radius: 8px;
      box-shadow: 0 2px 10px rgba(0,0,0,0.1);
    }
    .card h3 {
      margin-bottom: 1rem;
    }
    .card button {
      margin-top: 1rem;
      padding: 0.75rem 1.5rem;
      background: #1976d2;
      color: white;
      border: none;
      border-radius: 4px;
      cursor: pointer;
    }
  `]
})
export class DashboardComponent implements OnInit {
  user: any;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/login']);
      return;
    }
    this.user = this.authService.getUser();
  }

  navigateTo(path: string) {
    this.router.navigate([path]);
  }
}


