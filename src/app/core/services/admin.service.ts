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
  applicantId: number;
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
  passwordHash: string;
  isApproved: boolean;
  isEmailVerified: boolean;
  approvalStatus: string;
  createdAt: string;
  updatedAt: string;
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

export interface LoanProgressTimeline {
  loanId: number;
  applicantId: number;
  applicantName: string;
  loanType: string;
  loanAmount: number;
  currentStatus: string;
  appliedAt: string;
  events: LoanProgressEvent[];
}

export interface LoanProgressEvent {
  eventId: number;
  eventType: string; // APPLICATION_SUBMITTED, ASSIGNED_TO_OFFICER, OFFICER_REVIEW, ESCALATED, COMPLIANCE_REVIEW, APPROVED, REJECTED
  eventStatus: string;
  performedBy: string;
  performedByRole: string; // APPLICANT, LOAN_OFFICER, COMPLIANCE_OFFICER, SYSTEM
  officerId?: number;
  officerName?: string;
  action?: string;
  remarks?: string;
  timestamp: string;
}

export interface OfficerResponse {
  officerId: number;
  username: string;
  email: string;
  loanType?: string;
  createdAt: string;
  message?: string;
}

export interface LoanOfficerRequest {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  loanType?: string;
}

export interface ComplianceOfficerRequest {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  loanType?: string;
}

export interface ActivityLog {
  logId: number;
  performedBy: string;
  userRole: string;
  activityType: string;
  entityType?: string;
  entityId?: number;
  description: string;
  ipAddress?: string;
  userAgent?: string;
  oldValue?: string;
  newValue?: string;
  status: string;
  errorMessage?: string;
  timestamp: string;
}

export interface FraudRule {
  ruleId: number;
  ruleCode: string;
  ruleName: string;
  ruleDescription?: string;
  ruleCategory: string;
  severity: string;
  fraudPoints: number;
  isActive: boolean;
  ruleType?: string;
  executionOrder: number;
  createdAt: string;
  updatedAt: string;
  createdBy?: string;
  updatedBy?: string;
}

export interface FraudRuleRequest {
  ruleCode: string;
  ruleName: string;
  ruleDescription?: string;
  ruleCategory: string;
  severity: string;
  fraudPoints: number;
  isActive?: boolean;
  ruleType?: string;
  executionOrder?: number;
}

export interface FraudRuleUpdateRequest {
  ruleName?: string;
  ruleDescription?: string;
  ruleCategory?: string;
  severity?: string;
  fraudPoints?: number;
  isActive?: boolean;
  ruleType?: string;
  executionOrder?: number;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private readonly API_URL = environment.apiUrl;

  constructor(private http: HttpClient) {}

  // Dashboard Statistics
  getDashboardStats(): Observable<any> {
    // Use public endpoint for testing
    return this.http.get<any>(`${this.API_URL}/public/admin/dashboard/stats`);
  }

  // Applicant Management
  getAllApplicants(page: number = 0, size: number = 10): Observable<{content: Applicant[], totalElements: number}> {
    // Use public endpoint for testing
    return this.http.get<{content: Applicant[], totalElements: number}>(`${this.API_URL}/public/admin/applicants?page=${page}&size=${size}`);
  }

  getApplicantById(id: number): Observable<Applicant> {
    return this.http.get<Applicant>(`${this.API_URL}/admin/applicants/${id}`);
  }

  approveApplicant(id: number, comments?: string): Observable<any> {
    // Use correct backend endpoint
    return this.http.put(`${this.API_URL}/admin/applicant-approvals/${id}/approve`, { comments: comments || '' });
  }

  rejectApplicant(id: number, comments: string): Observable<any> {
    // Use correct backend endpoint
    return this.http.put(`${this.API_URL}/admin/applicant-approvals/${id}/reject`, { comments });
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

  getLoanProgressTimeline(loanId: number): Observable<LoanProgressTimeline> {
    return this.http.get<LoanProgressTimeline>(`${this.API_URL}/admin/loans/${loanId}/progress`);
  }

  // Search and Filter
  searchApplicants(query: string): Observable<Applicant[]> {
    return this.http.get<Applicant[]>(`${this.API_URL}/admin/applicants/search?q=${query}`);
  }

  filterApplicantsByStatus(status: string): Observable<Applicant[]> {
    return this.http.get<Applicant[]>(`${this.API_URL}/admin/applicants/filter?status=${status}`);
  }

  // Loan Officer Management
  getAllLoanOfficers(): Observable<OfficerResponse[]> {
    return this.http.get<OfficerResponse[]>(`${this.API_URL}/admin/officers/loan-officers`);
  }

  getLoanOfficerById(id: number): Observable<OfficerResponse> {
    return this.http.get<OfficerResponse>(`${this.API_URL}/admin/officers/loan-officers/${id}`);
  }

  addLoanOfficer(request: LoanOfficerRequest): Observable<OfficerResponse> {
    return this.http.post<OfficerResponse>(`${this.API_URL}/admin/officers/loan-officers`, request);
  }

  deleteLoanOfficer(id: number): Observable<any> {
    return this.http.delete(`${this.API_URL}/admin/officers/loan-officers/${id}`);
  }

  // Compliance Officer Management
  getAllComplianceOfficers(): Observable<OfficerResponse[]> {
    return this.http.get<OfficerResponse[]>(`${this.API_URL}/admin/officers/compliance-officers`);
  }

  getComplianceOfficerById(id: number): Observable<OfficerResponse> {
    return this.http.get<OfficerResponse>(`${this.API_URL}/admin/officers/compliance-officers/${id}`);
  }

  addComplianceOfficer(request: ComplianceOfficerRequest): Observable<OfficerResponse> {
    return this.http.post<OfficerResponse>(`${this.API_URL}/admin/officers/compliance-officers`, request);
  }

  deleteComplianceOfficer(id: number): Observable<any> {
    return this.http.delete(`${this.API_URL}/admin/officers/compliance-officers/${id}`);
  }

  // Activity Logs Management
  getAllActivityLogs(page: number = 0, size: number = 20): Observable<any> {
    return this.http.get(`${this.API_URL}/admin/activity-logs?page=${page}&size=${size}`);
  }

  getRecentActivityLogs(): Observable<ActivityLog[]> {
    return this.http.get<ActivityLog[]>(`${this.API_URL}/admin/activity-logs/recent`);
  }

  getActivityLogsByUser(username: string, page: number = 0, size: number = 20): Observable<any> {
    return this.http.get(`${this.API_URL}/admin/activity-logs/user/${username}?page=${page}&size=${size}`);
  }

  getActivityLogsByType(activityType: string, page: number = 0, size: number = 20): Observable<any> {
    return this.http.get(`${this.API_URL}/admin/activity-logs/type/${activityType}?page=${page}&size=${size}`);
  }

  searchActivityLogs(query: string, page: number = 0, size: number = 20): Observable<any> {
    return this.http.get(`${this.API_URL}/admin/activity-logs/search?query=${query}&page=${page}&size=${size}`);
  }

  getActivityStatistics(): Observable<any> {
    return this.http.get(`${this.API_URL}/admin/activity-logs/statistics`);
  }

  // Fraud Rules Management
  getAllFraudRules(): Observable<FraudRule[]> {
    return this.http.get<FraudRule[]>(`${this.API_URL}/admin/fraud-rules`);
  }

  getActiveFraudRules(): Observable<FraudRule[]> {
    return this.http.get<FraudRule[]>(`${this.API_URL}/admin/fraud-rules/active`);
  }

  getFraudRulesByCategory(category: string): Observable<FraudRule[]> {
    return this.http.get<FraudRule[]>(`${this.API_URL}/admin/fraud-rules/category/${category}`);
  }

  getFraudRuleById(id: number): Observable<FraudRule> {
    return this.http.get<FraudRule>(`${this.API_URL}/admin/fraud-rules/${id}`);
  }

  createFraudRule(request: FraudRuleRequest): Observable<FraudRule> {
    return this.http.post<FraudRule>(`${this.API_URL}/admin/fraud-rules`, request);
  }

  updateFraudRule(id: number, request: FraudRuleUpdateRequest): Observable<FraudRule> {
    return this.http.put<FraudRule>(`${this.API_URL}/admin/fraud-rules/${id}`, request);
  }

  toggleFraudRule(id: number): Observable<FraudRule> {
    return this.http.patch<FraudRule>(`${this.API_URL}/admin/fraud-rules/${id}/toggle`, {});
  }

  deleteFraudRule(id: number): Observable<any> {
    return this.http.delete(`${this.API_URL}/admin/fraud-rules/${id}`);
  }

  getFraudRuleStatistics(): Observable<any> {
    return this.http.get(`${this.API_URL}/admin/fraud-rules/statistics`);
  }
}
