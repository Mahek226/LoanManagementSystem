import { Routes } from '@angular/router';
import { authGuard } from '@core/guards/auth.guard';
import { roleGuard } from '@core/guards/role.guard';
import { guestGuard } from '@core/guards/guest.guard';
import { APP_CONSTANTS } from '@core/constants/app.constants';

export const routes: Routes = [
  // Default route - redirect to login
  {
    path: '',
    redirectTo: 'auth/login',
    pathMatch: 'full'
  },

  // Auth routes (accessible only to guests)
  {
    path: 'auth',
    canActivate: [guestGuard],
    children: [
      {
        path: 'login',
        loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
      },
      {
        path: 'register',
        loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent)
      },
      {
        path: 'verify-email',
        loadComponent: () => import('./features/auth/email-verification/email-verification.component').then(m => m.EmailVerificationComponent)
      },
      {
        path: 'forgot-password',
        loadComponent: () => import('./features/auth/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent)
      },
      {
        path: 'reset-password/:token',
        loadComponent: () => import('./features/auth/reset-password/reset-password.component').then(m => m.ResetPasswordComponent)
      },
      {
        path: 'admin-login',
        loadComponent: () => import('./features/auth/admin-login/admin-login.component').then(m => m.AdminLoginComponent)
      }
    ]
  },

  // Admin routes
  {
    path: 'admin',
    canActivate: [authGuard, roleGuard],
    data: { roles: [APP_CONSTANTS.ROLES.ADMIN] },
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./features/admin/dashboard/dashboard.component').then(m => m.DashboardComponent)
      },
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      }
    ]
  },

  // Loan Officer routes
  {
    path: 'loan-officer',
    canActivate: [authGuard, roleGuard],
    data: { roles: [APP_CONSTANTS.ROLES.LOAN_OFFICER] },
    loadComponent: () => import('./features/loan-officer/loan-officer-layout/loan-officer-layout.component').then(m => m.LoanOfficerLayoutComponent),
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./features/loan-officer/dashboard/dashboard.component').then(m => m.LoDashboardComponent)
      },
      {
        path: 'assigned-loans',
        loadComponent: () => import('./features/loan-officer/assigned-loans/assigned-loans.component').then(m => m.AssignedLoansComponent)
      },
      {
        path: 'review/:id',
        loadComponent: () => import('./features/loan-officer/loan-review/loan-review.component').then(m => m.LoanReviewComponent)
      },
      {
        path: 'profile',
        loadComponent: () => import('./features/loan-officer/profile/lo-profile.component').then(m => m.LoProfileComponent)
      },
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      }
    ]
  },

  // Compliance Officer routes
  {
    path: 'compliance',
    canActivate: [authGuard, roleGuard],
    data: { roles: [APP_CONSTANTS.ROLES.COMPLIANCE_OFFICER] },
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./features/compliance-officer/dashboard/compliance-dashboard.component').then(m => m.ComplianceDashboardComponent)
      },
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      }
    ]
  },

  // Applicant routes
  {
    path: 'applicant',
    canActivate: [authGuard, roleGuard],
    data: { roles: [APP_CONSTANTS.ROLES.APPLICANT] },
    loadComponent: () => import('./features/applicant/applicant-layout/applicant-layout.component').then(m => m.ApplicantLayoutComponent),
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./features/applicant/dashboard/enhanced-dashboard/enhanced-dashboard.component').then(m => m.EnhancedDashboardComponent)
      },
      {
        path: 'applications',
        loadComponent: () => import('./features/applicant/my-applications/my-applications.component').then(m => m.MyApplicationsComponent)
      },
      {
        path: 'profile',
        loadComponent: () => import('./features/applicant/profile/profile.component').then(m => m.ProfileComponent)
      },
      {
        path: 'apply-loan',
        loadComponent: () => import('./features/applicant/apply-loan/apply-loan.component').then(m => m.ApplyLoanComponent)
      },
      {
        path: 'documents',
        loadComponent: () => import('./features/applicant/documents/documents.component').then(m => m.DocumentsComponent)
      },
      {
        path: 'track-application',
        loadComponent: () => import('./features/applicant/track-application/track-application.component').then(m => m.TrackApplicationComponent)
      },
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      }
    ]
  },

  // 404 - Redirect to login
  {
    path: '**',
    redirectTo: 'auth/login'
  }
];
