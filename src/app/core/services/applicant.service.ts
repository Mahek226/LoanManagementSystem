import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@environments/environment';

export interface LoanApplication {
  loanId: number;
  applicantId: number;
  loanType: string;
  loanAmount: number;
  loanTenure: number;
  interestRate: number;
  monthlyIncome: number;
  employmentType: string;
  employerName: string;
  loanStatus: string;
  applicationDate: string;
  lastUpdated: string;
  fraudScore: number;
  fraudStatus: string;
  assignedOfficerId?: number;
  assignedOfficerName?: string;
  remarks?: string;
}

export interface ApplicantProfile {
  applicantId: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  phone: string;
  dob: string;
  gender: string;
  address: string;
  city: string;
  state: string;
  country: string;
  isApproved: boolean;
  isEmailVerified: boolean;
  approvalStatus: string;
  createdAt: string;
}

export interface DashboardStats {
  totalApplications: number;
  pendingApplications: number;
  approvedApplications: number;
  rejectedApplications: number;
  totalLoanAmount: number;
  averageFraudScore: number;
  recentApplications: LoanApplication[];
}

@Injectable({
  providedIn: 'root'
})
export class ApplicantService {
  private readonly API_URL = environment.apiUrl;

  constructor(private http: HttpClient) {}

  // Get applicant profile
  getApplicantProfile(applicantId: number): Observable<ApplicantProfile> {
    return this.http.get<ApplicantProfile>(`${this.API_URL}/loan-applications/applicant/${applicantId}`);
  }

  // Get applicant's loan applications
  getMyApplications(applicantId: number): Observable<LoanApplication[]> {
    return this.http.get<LoanApplication[]>(`${this.API_URL}/loan-applications/applicant/${applicantId}/loans`);
  }

  // Get specific loan details
  getLoanDetails(loanId: number): Observable<LoanApplication> {
    return this.http.get<LoanApplication>(`${this.API_URL}/loan-applications/loan/${loanId}`);
  }

  // Submit loan application
  submitLoanApplication(applicationData: any): Observable<any> {
    return this.http.post(`${this.API_URL}/loan-applications/submit-complete`, applicationData);
  }

  // Get dashboard statistics (calculated from applications)
  getDashboardStats(applicantId: number): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.API_URL}/loan-applications/applicant/${applicantId}/stats`);
  }

  // Calculate stats locally from applications
  calculateStats(applications: LoanApplication[]): DashboardStats {
    const totalApplications = applications.length;
    const pendingApplications = applications.filter(app => 
      app.loanStatus === 'PENDING' || app.loanStatus === 'UNDER_REVIEW'
    ).length;
    const approvedApplications = applications.filter(app => 
      app.loanStatus === 'APPROVED'
    ).length;
    const rejectedApplications = applications.filter(app => 
      app.loanStatus === 'REJECTED'
    ).length;
    const totalLoanAmount = applications
      .filter(app => app.loanStatus === 'APPROVED')
      .reduce((sum, app) => sum + app.loanAmount, 0);
    const averageFraudScore = applications.length > 0
      ? applications.reduce((sum, app) => sum + (app.fraudScore || 0), 0) / applications.length
      : 0;
    const recentApplications = applications
      .sort((a, b) => new Date(b.applicationDate).getTime() - new Date(a.applicationDate).getTime())
      .slice(0, 5);

    return {
      totalApplications,
      pendingApplications,
      approvedApplications,
      rejectedApplications,
      totalLoanAmount,
      averageFraudScore,
      recentApplications
    };
  }

  // Format currency
  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(amount);
  }

  // Format date
  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  // Get status color class
  getStatusColor(status: string): string {
    const colors: any = {
      'PENDING': 'warning',
      'UNDER_REVIEW': 'info',
      'APPROVED': 'success',
      'REJECTED': 'danger',
      'DISBURSED': 'success',
      'CLOSED': 'secondary'
    };
    return colors[status] || 'secondary';
  }

  // Get fraud status color
  getFraudStatusColor(status: string): string {
    const colors: any = {
      'LOW_RISK': 'success',
      'MEDIUM_RISK': 'warning',
      'HIGH_RISK': 'danger',
      'FLAGGED': 'danger',
      'CLEARED': 'success'
    };
    return colors[status] || 'secondary';
  }
}
