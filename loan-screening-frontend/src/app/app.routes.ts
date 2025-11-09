import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { LoanApplicationComponent } from './components/loan-application/loan-application.component';
import { DocumentUploadComponent } from './components/document-upload/document-upload.component';

export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'dashboard', component: DashboardComponent },
  { path: 'loan-application', component: LoanApplicationComponent },
  { path: 'document-upload', component: DocumentUploadComponent },
  { path: '**', redirectTo: '/login' }
];


