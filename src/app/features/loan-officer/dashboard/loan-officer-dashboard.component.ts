import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '@core/services/auth.service';

@Component({
  selector: 'app-loan-officer-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="dashboard-container">
      <div class="dashboard-header">
        <h1>Loan Officer Dashboard</h1>
        <button (click)="logout()" class="btn btn-secondary">Logout</button>
      </div>
      <div class="dashboard-content">
        <div class="welcome-card card">
          <div class="card-body">
            <h2>Welcome, {{ userName }}!</h2>
            <p>You are logged in as a <strong>Loan Officer</strong>.</p>
            <p class="text-secondary">This is a placeholder for the loan officer dashboard. You can manage loan applications here.</p>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .dashboard-container {
      min-height: 100vh;
      background-color: var(--background);
      padding: 2rem;
    }
    
    .dashboard-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 2rem;
    }
    
    .dashboard-header h1 {
      font-size: 2rem;
      font-weight: 700;
      color: var(--text-primary);
    }
    
    .dashboard-content {
      max-width: 1200px;
    }
    
    .welcome-card {
      margin-bottom: 2rem;
    }
    
    .welcome-card h2 {
      font-size: 1.5rem;
      margin-bottom: 1rem;
      color: var(--text-primary);
    }
  `]
})
export class LoanOfficerDashboardComponent {
  userName: string;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {
    const user = this.authService.currentUserValue;
    this.userName = user?.username || (user?.firstName && user?.lastName ? `${user.firstName} ${user.lastName}` : 'Loan Officer');
  }

  logout(): void {
    this.authService.logout();
  }
}
