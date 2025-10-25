import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface DashboardStats {
  totalApplicants: number;
  pendingApplications: number;
  approvedLoans: number;
  rejectedApplications: number;
  totalLoanAmount: number;
  averageLoanAmount: number;
  monthlyApplications: number[];
  loanStatusDistribution: { status: string; count: number; }[];
}

export interface Applicant {
  id: number;
  username: string;
  firstName: string;
  lastName: string;
  email: string;
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

export interface LoanApplication {
  id: number;
  applicantId: number;
  applicantName: string;
  loanType: string;
  requestedAmount: number;
  purpose: string;
  employmentStatus: string;
  monthlyIncome: number;
  status: string;
  appliedDate: string;
  reviewedDate?: string;
  reviewedBy?: string;
  comments?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private readonly API_URL = environment.apiUrl;

  constructor(private http: HttpClient) {}

  // Dashboard Statistics
  getDashboardStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.API_URL}/admin/dashboard/stats`);
  }

  // Applicant Management
  getAllApplicants(page: number = 0, size: number = 10): Observable<{content: Applicant[], totalElements: number}> {
    return this.http.get<{content: Applicant[], totalElements: number}>(`${this.API_URL}/admin/applicants?page=${page}&size=${size}`);
  }

  getApplicantById(id: number): Observable<Applicant> {
    return this.http.get<Applicant>(`${this.API_URL}/admin/applicants/${id}`);
  }

  approveApplicant(id: number, comments?: string): Observable<any> {
    return this.http.put(`${this.API_URL}/admin/applicants/${id}/approve`, { comments: comments || '' });
  }

  rejectApplicant(id: number, comments: string): Observable<any> {
    return this.http.put(`${this.API_URL}/admin/applicants/${id}/reject`, { comments });
  }

  // Loan Management
  getAllLoanApplications(page: number = 0, size: number = 10): Observable<{content: LoanApplication[], totalElements: number}> {
    return this.http.get<{content: LoanApplication[], totalElements: number}>(`${this.API_URL}/admin/loans?page=${page}&size=${size}`);
  }

  getLoanApplication(id: number): Observable<LoanApplication> {
    return this.http.get<LoanApplication>(`${this.API_URL}/admin/loans/${id}`);
  }

  approveLoan(id: number, comments?: string): Observable<any> {
    return this.http.put(`${this.API_URL}/admin/loans/${id}/approve`, { comments });
  }

  rejectLoan(id: number, comments: string): Observable<any> {
    return this.http.put(`${this.API_URL}/admin/loans/${id}/reject`, { comments });
  }

  // Search and Filter
  searchApplicants(query: string): Observable<Applicant[]> {
    return this.http.get<Applicant[]>(`${this.API_URL}/admin/applicants/search?q=${query}`);
  }

  filterApplicantsByStatus(status: string): Observable<Applicant[]> {
    return this.http.get<Applicant[]>(`${this.API_URL}/admin/applicants/filter?status=${status}`);
  }
}
