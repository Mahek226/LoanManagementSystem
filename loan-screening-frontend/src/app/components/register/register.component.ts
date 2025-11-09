import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="register-container">
      <div class="register-card">
        <h2>Register as Applicant</h2>
        <form (ngSubmit)="onSubmit()">
          <div class="form-row">
            <div class="form-group">
              <label>First Name</label>
              <input type="text" [(ngModel)]="formData.first_name" name="first_name" required>
            </div>
            <div class="form-group">
              <label>Last Name</label>
              <input type="text" [(ngModel)]="formData.last_name" name="last_name" required>
            </div>
          </div>
          <div class="form-group">
            <label>Username</label>
            <input type="text" [(ngModel)]="formData.username" name="username" required>
          </div>
          <div class="form-group">
            <label>Email</label>
            <input type="email" [(ngModel)]="formData.email" name="email" required>
          </div>
          <div class="form-group">
            <label>Phone</label>
            <input type="tel" [(ngModel)]="formData.phone" name="phone" required>
          </div>
          <div class="form-group">
            <label>Password</label>
            <input type="password" [(ngModel)]="formData.password" name="password" required>
          </div>
          <div class="form-group">
            <label>Date of Birth</label>
            <input type="date" [(ngModel)]="formData.dob" name="dob" required>
          </div>
          <div class="form-group">
            <label>Gender</label>
            <select [(ngModel)]="formData.gender" name="gender" required>
              <option value="">Select</option>
              <option value="Male">Male</option>
              <option value="Female">Female</option>
              <option value="Other">Other</option>
            </select>
          </div>
          <div class="form-group">
            <label>Address</label>
            <textarea [(ngModel)]="formData.address" name="address" required></textarea>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>City</label>
              <input type="text" [(ngModel)]="formData.city" name="city" required>
            </div>
            <div class="form-group">
              <label>State</label>
              <input type="text" [(ngModel)]="formData.state" name="state" required>
            </div>
            <div class="form-group">
              <label>Country</label>
              <input type="text" [(ngModel)]="formData.country" name="country" required>
            </div>
          </div>
          <button type="submit" [disabled]="loading">Register</button>
          <p *ngIf="message" [class]="messageType">{{ message }}</p>
          <p>Already have an account? <a routerLink="/login">Login</a></p>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .register-container {
      display: flex;
      justify-content: center;
      padding: 2rem;
    }
    .register-card {
      background: white;
      padding: 2rem;
      border-radius: 8px;
      box-shadow: 0 2px 10px rgba(0,0,0,0.1);
      width: 100%;
      max-width: 800px;
    }
    .form-row {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 1rem;
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
export class RegisterComponent {
  formData = {
    username: '',
    email: '',
    password: '',
    first_name: '',
    last_name: '',
    dob: '',
    gender: '',
    phone: '',
    address: '',
    city: '',
    state: '',
    country: 'India'
  };
  loading = false;
  message = '';
  messageType = '';

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  onSubmit() {
    this.loading = true;
    this.message = '';
    
    this.authService.register(this.formData).subscribe({
      next: (response) => {
        this.message = response.message || 'Registration successful! Please check your email for OTP.';
        this.messageType = 'success';
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 3000);
      },
      error: (error) => {
        this.message = error.error?.detail || 'Registration failed';
        this.messageType = 'error';
        this.loading = false;
      }
    });
  }
}

