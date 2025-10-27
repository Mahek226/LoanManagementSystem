import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '@core/services/auth.service';

@Component({
  selector: 'app-applicant-layout',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './applicant-layout.component.html',
  styleUrl: './applicant-layout.component.css'
})
export class ApplicantLayoutComponent implements OnInit {
  activeTab: string = 'dashboard';
  userName: string = '';
  showFallbackLogo = false;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {
    const user = this.authService.currentUserValue;
    this.userName = user?.firstName && user?.lastName 
      ? `${user.firstName} ${user.lastName}` 
      : user?.username || 'Applicant';
  }

  ngOnInit(): void {
    // Set active tab based on current route
    const currentPath = this.router.url.split('/').pop();
    this.setActiveTabFromRoute(currentPath || 'dashboard');
  }

  setActiveTabFromRoute(route: string): void {
    if (route.includes('dashboard') || route === 'applicant') {
      this.activeTab = 'dashboard';
    } else if (route.includes('applications')) {
      this.activeTab = 'applications';
    } else if (route.includes('profile')) {
      this.activeTab = 'profile';
    } else if (route.includes('apply-loan')) {
      this.activeTab = 'apply-loan';
    } else if (route.includes('documents')) {
      this.activeTab = 'documents';
    }
  }

  setActiveTab(tab: string): void {
    this.activeTab = tab;
    switch(tab) {
      case 'dashboard':
        this.router.navigate(['/applicant/dashboard']);
        break;
      case 'applications':
        this.router.navigate(['/applicant/applications']);
        break;
      case 'profile':
        this.router.navigate(['/applicant/profile']);
        break;
      case 'apply-loan':
        this.router.navigate(['/applicant/apply-loan']);
        break;
      case 'documents':
        this.router.navigate(['/applicant/documents']);
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
