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
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./features/loan-officer/dashboard/loan-officer-dashboard.component').then(m => m.LoanOfficerDashboardComponent)
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
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./features/applicant/dashboard/applicant-dashboard.component').then(m => m.ApplicantDashboardComponent)
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
