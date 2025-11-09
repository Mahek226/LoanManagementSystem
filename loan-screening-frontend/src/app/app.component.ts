import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, CommonModule],
  template: `
    <div class="app-container">
      <header class="app-header">
        <h1>Loan Screening Automation System</h1>
        <nav>
          <a routerLink="/login" *ngIf="!isLoggedIn">Login</a>
          <a routerLink="/register" *ngIf="!isLoggedIn">Register</a>
          <a routerLink="/dashboard" *ngIf="isLoggedIn">Dashboard</a>
          <button (click)="logout()" *ngIf="isLoggedIn">Logout</button>
        </nav>
      </header>
      <main>
        <router-outlet></router-outlet>
      </main>
    </div>
  `,
  styles: [`
    .app-container {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
    }
    .app-header {
      background: #1976d2;
      color: white;
      padding: 1rem 2rem;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .app-header h1 {
      margin: 0;
      font-size: 1.5rem;
    }
    .app-header nav {
      display: flex;
      gap: 1rem;
    }
    .app-header a, .app-header button {
      color: white;
      text-decoration: none;
      padding: 0.5rem 1rem;
      border: 1px solid white;
      border-radius: 4px;
      background: transparent;
      cursor: pointer;
    }
    main {
      flex: 1;
      padding: 2rem;
    }
  `]
})
export class AppComponent {
  title = 'loan-screening-frontend';
  isLoggedIn = false;

  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    this.isLoggedIn = false;
    window.location.href = '/login';
  }
}


