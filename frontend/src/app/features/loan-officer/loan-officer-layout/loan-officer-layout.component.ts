import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { NotificationBellComponent } from '../../../shared/components/notification-bell/notification-bell.component';

@Component({
  selector: 'app-loan-officer-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, NotificationBellComponent],
  templateUrl: './loan-officer-layout.component.html',
  styleUrl: './loan-officer-layout.component.css'
})
export class LoanOfficerLayoutComponent implements OnInit {
  activeTab: string = 'dashboard';
  officerName: string = '';
  showFallbackLogo = false;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {
    const user = this.authService.currentUserValue;
    this.officerName = user?.firstName && user?.lastName 
      ? `${user.firstName} ${user.lastName}` 
      : user?.username || 'Officer';
  }

  ngOnInit(): void {
    // Set active tab based on current route
    const currentPath = this.router.url.split('/').pop();
    this.setActiveTabFromRoute(currentPath || 'dashboard');
  }

  setActiveTabFromRoute(route: string): void {
    if (route.includes('dashboard') || route === 'loan-officer') {
      this.activeTab = 'dashboard';
    } else if (route.includes('assigned-loans')) {
      this.activeTab = 'assigned-loans';
    } else if (route.includes('document-requests')) {
      this.activeTab = 'document-requests';
    } else if (route.includes('profile')) {
      this.activeTab = 'profile';
    }
  }

  setActiveTab(tab: string): void {
    this.activeTab = tab;
    switch(tab) {
      case 'dashboard':
        this.router.navigate(['/loan-officer/dashboard']);
        break;
      case 'assigned-loans':
        this.router.navigate(['/loan-officer/assigned-loans']);
        break;
      case 'document-requests':
        this.router.navigate(['/loan-officer/document-requests']);
        break;
      case 'profile':
        this.router.navigate(['/loan-officer/profile']);
        break;
    }
  }

  onImageError(): void {
    this.showFallbackLogo = true;
  }

  logout(): void {
    this.authService.logout();
  }
}
