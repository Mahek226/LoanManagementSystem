import { Component, OnInit, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '@core/services/auth.service';

@Component({
  selector: 'app-professional-header',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <header class="professional-header">
      <div class="header-container">
        <!-- Brand Section -->
        <div class="header-brand">
          <div class="brand-logo">
            <i class="fas fa-university text-primary-blue"></i>
          </div>
          <div class="brand-text">
            <h1 class="brand-title">LMS</h1>
            <span class="brand-subtitle">Loan Management System</span>
          </div>
        </div>

        <!-- Search Section -->
        <div class="header-search" *ngIf="showSearch">
          <div class="search-wrapper">
            <i class="fas fa-search search-icon"></i>
            <input 
              type="text" 
              class="search-input" 
              placeholder="Search loans, applicants..."
              [(ngModel)]="searchQuery"
              (keyup.enter)="onSearch()">
          </div>
        </div>

        <!-- Actions Section -->
        <div class="header-actions">
          <!-- Notifications -->
          <div class="notification-wrapper" *ngIf="showNotifications">
            <button class="notification-btn" (click)="toggleNotifications()">
              <i class="fas fa-bell"></i>
              <span class="notification-badge" *ngIf="notificationCount > 0">
                {{ notificationCount }}
              </span>
            </button>
            
            <!-- Notification Dropdown -->
            <div class="notification-dropdown" *ngIf="showNotificationDropdown">
              <div class="dropdown-header">
                <h3>Notifications</h3>
                <button class="mark-all-read" (click)="markAllAsRead()">
                  Mark all as read
                </button>
              </div>
              <div class="notification-list">
                <div class="notification-item" *ngFor="let notification of notifications">
                  <div class="notification-icon" [ngClass]="getNotificationIconClass(notification.type)">
                    <i [class]="getNotificationIcon(notification.type)"></i>
                  </div>
                  <div class="notification-content">
                    <p class="notification-title">{{ notification.title }}</p>
                    <p class="notification-message">{{ notification.message }}</p>
                    <span class="notification-time">{{ formatTime(notification.timestamp) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- User Menu -->
          <div class="user-menu-wrapper">
            <button class="user-menu-btn" (click)="toggleUserMenu()">
              <img 
                [src]="userAvatar || '/assets/default-avatar.png'" 
                [alt]="userName"
                class="user-avatar">
              <div class="user-info">
                <span class="user-name">{{ userName }}</span>
                <span class="user-role">{{ userRole }}</span>
              </div>
              <i class="fas fa-chevron-down dropdown-icon" 
                 [class.rotated]="showUserDropdown"></i>
            </button>

            <!-- User Dropdown -->
            <div class="user-dropdown" *ngIf="showUserDropdown">
              <div class="dropdown-header">
                <img [src]="userAvatar || '/assets/default-avatar.png'" class="dropdown-avatar">
                <div>
                  <p class="dropdown-name">{{ userName }}</p>
                  <p class="dropdown-email">{{ userEmail }}</p>
                </div>
              </div>
              
              <div class="dropdown-divider"></div>
              
              <nav class="dropdown-nav">
                <a routerLink="/profile" class="dropdown-item">
                  <i class="fas fa-user"></i>
                  <span>My Profile</span>
                </a>
                <a routerLink="/settings" class="dropdown-item">
                  <i class="fas fa-cog"></i>
                  <span>Settings</span>
                </a>
                <a routerLink="/help" class="dropdown-item">
                  <i class="fas fa-question-circle"></i>
                  <span>Help & Support</span>
                </a>
              </nav>
              
              <div class="dropdown-divider"></div>
              
              <button class="dropdown-item logout-btn" (click)="logout()">
                <i class="fas fa-sign-out-alt"></i>
                <span>Sign Out</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </header>
  `,
  styles: [`
    .professional-header {
      background: linear-gradient(135deg, var(--primary-blue) 0%, var(--primary-blue-light) 100%);
      box-shadow: var(--shadow-lg);
      position: sticky;
      top: 0;
      z-index: var(--z-sticky);
      backdrop-filter: blur(10px);
    }

    .header-container {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: var(--space-4) var(--space-6);
      max-width: 1400px;
      margin: 0 auto;
    }

    .header-brand {
      display: flex;
      align-items: center;
      gap: var(--space-3);
    }

    .brand-logo {
      width: 48px;
      height: 48px;
      background: rgba(255, 255, 255, 0.1);
      border-radius: var(--radius-xl);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 1.5rem;
      color: white;
    }

    .brand-text {
      color: white;
    }

    .brand-title {
      font-size: var(--text-2xl);
      font-weight: var(--font-bold);
      margin: 0;
      line-height: 1;
    }

    .brand-subtitle {
      font-size: var(--text-sm);
      opacity: 0.9;
      font-weight: var(--font-medium);
    }

    .header-search {
      flex: 1;
      max-width: 400px;
      margin: 0 var(--space-8);
    }

    .search-wrapper {
      position: relative;
    }

    .search-input {
      width: 100%;
      padding: var(--space-3) var(--space-4) var(--space-3) var(--space-10);
      border: none;
      border-radius: var(--radius-full);
      background: rgba(255, 255, 255, 0.1);
      color: white;
      font-size: var(--text-base);
      backdrop-filter: blur(10px);
      transition: all var(--transition-normal);
    }

    .search-input::placeholder {
      color: rgba(255, 255, 255, 0.7);
    }

    .search-input:focus {
      outline: none;
      background: rgba(255, 255, 255, 0.2);
      box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.1);
    }

    .search-icon {
      position: absolute;
      left: var(--space-3);
      top: 50%;
      transform: translateY(-50%);
      color: rgba(255, 255, 255, 0.7);
      font-size: var(--text-base);
    }

    .header-actions {
      display: flex;
      align-items: center;
      gap: var(--space-4);
    }

    .notification-wrapper {
      position: relative;
    }

    .notification-btn {
      position: relative;
      padding: var(--space-2);
      border: none;
      background: rgba(255, 255, 255, 0.1);
      color: white;
      border-radius: var(--radius-lg);
      cursor: pointer;
      transition: all var(--transition-normal);
      font-size: 1.1rem;
    }

    .notification-btn:hover {
      background: rgba(255, 255, 255, 0.2);
      transform: scale(1.05);
    }

    .notification-badge {
      position: absolute;
      top: -4px;
      right: -4px;
      background: var(--danger-red);
      color: white;
      font-size: var(--text-xs);
      font-weight: var(--font-bold);
      padding: 2px 6px;
      border-radius: var(--radius-full);
      min-width: 18px;
      text-align: center;
      animation: pulse 2s infinite;
    }

    .user-menu-wrapper {
      position: relative;
    }

    .user-menu-btn {
      display: flex;
      align-items: center;
      gap: var(--space-3);
      padding: var(--space-2) var(--space-3);
      border: none;
      background: rgba(255, 255, 255, 0.1);
      color: white;
      border-radius: var(--radius-xl);
      cursor: pointer;
      transition: all var(--transition-normal);
    }

    .user-menu-btn:hover {
      background: rgba(255, 255, 255, 0.2);
    }

    .user-avatar {
      width: 40px;
      height: 40px;
      border-radius: var(--radius-full);
      border: 2px solid rgba(255, 255, 255, 0.2);
      object-fit: cover;
    }

    .user-info {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      text-align: left;
    }

    .user-name {
      font-size: var(--text-sm);
      font-weight: var(--font-semibold);
      line-height: 1;
    }

    .user-role {
      font-size: var(--text-xs);
      opacity: 0.8;
      line-height: 1;
    }

    .dropdown-icon {
      font-size: var(--text-sm);
      transition: transform var(--transition-normal);
    }

    .dropdown-icon.rotated {
      transform: rotate(180deg);
    }

    .notification-dropdown,
    .user-dropdown {
      position: absolute;
      top: calc(100% + var(--space-2));
      right: 0;
      background: var(--white);
      border-radius: var(--radius-xl);
      box-shadow: var(--shadow-2xl);
      min-width: 300px;
      z-index: var(--z-dropdown);
      animation: slideDown 0.2s ease-out;
    }

    .dropdown-header {
      padding: var(--space-4);
      border-bottom: 1px solid var(--gray-100);
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .dropdown-header h3 {
      font-size: var(--text-lg);
      font-weight: var(--font-semibold);
      color: var(--gray-800);
      margin: 0;
    }

    .mark-all-read {
      background: none;
      border: none;
      color: var(--primary-blue);
      font-size: var(--text-sm);
      cursor: pointer;
    }

    .dropdown-avatar {
      width: 48px;
      height: 48px;
      border-radius: var(--radius-full);
      margin-right: var(--space-3);
    }

    .dropdown-name {
      font-weight: var(--font-semibold);
      color: var(--gray-800);
      margin: 0;
    }

    .dropdown-email {
      font-size: var(--text-sm);
      color: var(--gray-500);
      margin: 0;
    }

    .dropdown-divider {
      height: 1px;
      background: var(--gray-100);
      margin: var(--space-2) 0;
    }

    .dropdown-nav {
      padding: var(--space-2);
    }

    .dropdown-item {
      display: flex;
      align-items: center;
      gap: var(--space-3);
      padding: var(--space-3) var(--space-4);
      border-radius: var(--radius-lg);
      text-decoration: none;
      color: var(--gray-700);
      font-size: var(--text-sm);
      transition: all var(--transition-normal);
      border: none;
      background: none;
      width: 100%;
      cursor: pointer;
    }

    .dropdown-item:hover {
      background: var(--gray-50);
      color: var(--primary-blue);
    }

    .logout-btn {
      color: var(--danger-red);
    }

    .logout-btn:hover {
      background: rgba(220, 38, 38, 0.1);
    }

    @media (max-width: 768px) {
      .header-search {
        display: none;
      }
      
      .user-info {
        display: none;
      }
      
      .header-container {
        padding: var(--space-3) var(--space-4);
      }
    }
  `]
})
export class ProfessionalHeaderComponent implements OnInit {
  @Input() showSearch = true;
  @Input() showNotifications = true;
  @Input() userAvatar?: string;
  
  searchQuery = '';
  showNotificationDropdown = false;
  showUserDropdown = false;
  notificationCount = 0;
  notifications: any[] = [];
  
  userName = '';
  userEmail = '';
  userRole = '';

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const user = this.authService.currentUserValue;
    if (user) {
      this.userName = user.firstName && user.lastName 
        ? `${user.firstName} ${user.lastName}` 
        : user.username || 'User';
      this.userEmail = user.email || '';
      this.userRole = this.formatRole(user.role);
    }
    
    this.loadNotifications();
  }

  onSearch(): void {
    if (this.searchQuery.trim()) {
      // Implement search functionality
      console.log('Searching for:', this.searchQuery);
    }
  }

  toggleNotifications(): void {
    this.showNotificationDropdown = !this.showNotificationDropdown;
    this.showUserDropdown = false;
  }

  toggleUserMenu(): void {
    this.showUserDropdown = !this.showUserDropdown;
    this.showNotificationDropdown = false;
  }

  markAllAsRead(): void {
    this.notifications.forEach(n => n.read = true);
    this.notificationCount = 0;
  }

  logout(): void {
    this.authService.logout();
  }

  private loadNotifications(): void {
    // Mock notifications - replace with actual service
    this.notifications = [
      {
        id: 1,
        type: 'loan',
        title: 'New Loan Application',
        message: 'John Doe submitted a new loan application',
        timestamp: new Date(),
        read: false
      }
    ];
    this.notificationCount = this.notifications.filter(n => !n.read).length;
  }

  private formatRole(role: string): string {
    return role?.replace('ROLE_', '').replace('_', ' ').toLowerCase()
      .replace(/\b\w/g, l => l.toUpperCase()) || 'User';
  }

  getNotificationIcon(type: string): string {
    const icons = {
      loan: 'fas fa-file-contract',
      approval: 'fas fa-check-circle',
      rejection: 'fas fa-times-circle',
      system: 'fas fa-cog'
    };
    return icons[type as keyof typeof icons] || 'fas fa-bell';
  }

  getNotificationIconClass(type: string): string {
    const classes = {
      loan: 'text-blue-500',
      approval: 'text-green-500',
      rejection: 'text-red-500',
      system: 'text-gray-500'
    };
    return classes[type as keyof typeof classes] || 'text-gray-500';
  }

  formatTime(timestamp: Date): string {
    const now = new Date();
    const diff = now.getTime() - timestamp.getTime();
    const minutes = Math.floor(diff / 60000);
    
    if (minutes < 1) return 'Just now';
    if (minutes < 60) return `${minutes}m ago`;
    if (minutes < 1440) return `${Math.floor(minutes / 60)}h ago`;
    return `${Math.floor(minutes / 1440)}d ago`;
  }
}
