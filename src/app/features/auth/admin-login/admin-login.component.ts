import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-admin-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="min-vh-100 d-flex align-items-center justify-content-center" style="background-color: var(--bg-secondary);">
      <div class="card shadow-lg border-0" style="width: 400px; background-color: var(--bg-primary);">
        <div class="card-body p-4">
          <h3 class="text-center mb-4" style="color: var(--text-primary);">Admin Login</h3>
          
          <form (ngSubmit)="onSubmit()" #loginForm="ngForm">
            <div class="mb-3">
              <label class="form-label" style="color: var(--text-primary);">Username or Email</label>
              <input type="text" 
                     class="form-control" 
                     [(ngModel)]="credentials.usernameOrEmail" 
                     name="usernameOrEmail" 
                     required
                     style="background-color: var(--bg-secondary); border-color: var(--border-color); color: var(--text-primary);">
            </div>
            
            <div class="mb-3">
              <label class="form-label" style="color: var(--text-primary);">Password</label>
              <input type="password" 
                     class="form-control" 
                     [(ngModel)]="credentials.password" 
                     name="password" 
                     required
                     style="background-color: var(--bg-secondary); border-color: var(--border-color); color: var(--text-primary);">
            </div>
            
            <div class="alert alert-danger" *ngIf="errorMessage" style="background-color: rgba(220, 53, 69, 0.1); border-color: #dc3545; color: #dc3545;">
              {{ errorMessage }}
            </div>
            
            <button type="submit" 
                    class="btn btn-primary w-100" 
                    [disabled]="loading || !loginForm.valid">
              <span *ngIf="loading" class="spinner-border spinner-border-sm me-2"></span>
              {{ loading ? 'Signing in...' : 'Sign In' }}
            </button>
          </form>
          
          <div class="text-center mt-3">
            <button class="btn btn-outline-secondary btn-sm" (click)="createSampleData()">
              Create Sample Data
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class AdminLoginComponent {
  credentials = {
    usernameOrEmail: 'admin',
    password: 'admin123'
  };
  
  loading = false;
  errorMessage = '';

  constructor(private http: HttpClient, private router: Router) {}

  onSubmit(): void {
    this.loading = true;
    this.errorMessage = '';

    this.http.post(`${environment.apiUrl}/admin/auth/login`, this.credentials).subscribe({
      next: (response: any) => {
        // Store token if needed
        localStorage.setItem('adminToken', response.token);
        this.router.navigate(['/admin/dashboard']);
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Login failed. Please try again.';
        this.loading = false;
      },
      complete: () => {
        this.loading = false;
      }
    });
  }

  createSampleData(): void {
    this.http.post(`${environment.apiUrl}/test/create-sample-data`, {}).subscribe({
      next: (response: any) => {
        alert('Sample data created successfully!');
      },
      error: (error) => {
        console.error('Error creating sample data:', error);
        alert('Error creating sample data. Check console for details.');
      }
    });
  }
}
