import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationBellComponent } from '../../../shared/components/notification-bell/notification-bell.component';

@Component({
  selector: 'app-compliance-officer-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, NotificationBellComponent],
  templateUrl: './compliance-officer-layout.component.html',
  styleUrls: ['./compliance-officer-layout.component.css']
})
export class ComplianceOfficerLayoutComponent implements OnInit {
  activeTab: string = 'dashboard';
  officerName: string = '';
  showUserMenu: boolean = false;
  showFallbackLogo: boolean = false;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const user = this.authService.currentUserValue;
    if (user) {
      // Handle cases where firstName/lastName might be null
      const firstName = user.firstName || '';
      const lastName = user.lastName || '';
      
      if (firstName || lastName) {
        this.officerName = `${firstName} ${lastName}`.trim();
      } else if (user.username) {
        this.officerName = user.username;
      } else {
        this.officerName = 'Compliance Officer';
      }
    }

    // Set active tab based on current route
    const currentRoute = this.router.url;
    if (currentRoute.includes('escalations')) {
      this.activeTab = 'escalations';
    } else if (currentRoute.includes('review')) {
      this.activeTab = 'escalations';
    } else if (currentRoute.includes('profile')) {
      this.activeTab = 'profile';
    } else {
      this.activeTab = 'dashboard';
    }
  }

  setActiveTab(tab: string): void {
    this.activeTab = tab;
  }

  toggleUserMenu(): void {
    this.showUserMenu = !this.showUserMenu;
  }

  onImageError(): void {
    this.showFallbackLogo = true;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }
}
