import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterOutlet, NavigationEnd } from '@angular/router';
import { ThemeService, Theme } from '../../../core/services/theme.service';
import { AuthService } from '../../../core/services/auth.service';
import { AdminService } from '../../../core/services/admin.service';
import { Subscription, filter } from 'rxjs';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet],
  templateUrl: './admin-layout.component.html',
  styleUrl: './admin-layout.component.css'
})
export class AdminLayoutComponent implements OnInit, OnDestroy {
  currentTheme: Theme = 'system';
  showFallbackLogo = false;
  notificationCount = 3;
  currentRoute = '';
  stats: any = null;

  private themeSubscription: Subscription | null = null;
  private routerSubscription: Subscription | null = null;
  private statsSubscription: Subscription | null = null;

  constructor(
    private themeService: ThemeService,
    private authService: AuthService,
    private adminService: AdminService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Subscribe to theme changes
    this.themeSubscription = this.themeService.theme$.subscribe(theme => {
      this.currentTheme = theme;
    });

    // Subscribe to router events to track current route
    this.routerSubscription = this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event) => {
        if (event instanceof NavigationEnd) {
          this.currentRoute = event.url;
        }
      });

    // Get initial route
    this.currentRoute = this.router.url;

    // Subscribe to dashboard stats for notification badges
    this.statsSubscription = this.adminService.dashboardStats$.subscribe(stats => {
      this.stats = stats;
    });

    // Load stats if not already cached
    if (!this.adminService.getCachedDashboardStats()) {
      this.adminService.getDashboardStats().subscribe();
    }
  }

  ngOnDestroy(): void {
    if (this.themeSubscription) {
      this.themeSubscription.unsubscribe();
    }
    if (this.routerSubscription) {
      this.routerSubscription.unsubscribe();
    }
    if (this.statsSubscription) {
      this.statsSubscription.unsubscribe();
    }
  }

  // Navigation methods
  navigateTo(route: string): void {
    this.router.navigate([`/admin/${route}`]);
  }

  isActiveRoute(route: string): boolean {
    return this.currentRoute.includes(`/admin/${route}`);
  }

  // Theme methods
  setTheme(theme: Theme): void {
    this.themeService.setTheme(theme);
  }

  getThemeIcon(): string {
    switch (this.currentTheme) {
      case 'light': return 'fa-sun';
      case 'dark': return 'fa-moon';
      case 'system': return 'fa-desktop';
      default: return 'fa-desktop';
    }
  }

  getThemeLabel(): string {
    switch (this.currentTheme) {
      case 'light': return 'Light';
      case 'dark': return 'Dark';
      case 'system': return 'System';
      default: return 'System';
    }
  }

  // Utility methods
  onImageError(): void {
    this.showFallbackLogo = true;
  }

  logout(): void {
    // Clear cached data on logout
    this.adminService.clearCache();
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }
}
