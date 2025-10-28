import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@environments/environment';

export interface LoanScreeningResponse {
  assignmentId: number;
  loanId: number;
  applicantId: number;
  applicantName: string;
  loanType: string;
  loanAmount: number;
  riskScore: number;
  riskLevel: string; // LOW, MEDIUM, HIGH
  canApproveReject: boolean;
  status: string;
  remarks?: string;
  assignedAt: string;
  processedAt?: string;
  officerId: number;
  officerName: string;
  officerType: string;
}

export interface LoanScreeningRequest {
  assignmentId: number;
  action: string; // APPROVE, REJECT, ESCALATE_TO_COMPLIANCE
  remarks?: string;
  rejectionReason?: string;
}

export interface DashboardStats {
  totalAssigned: number;
  pendingReview: number;
  approved: number;
  rejected: number;
  escalated: number;
  highRiskCount: number;
  mediumRiskCount: number;
  lowRiskCount: number;
}

export interface LoanDocument {
  documentId: number;
  documentType: string;
  documentName: string;
  documentUrl: string;
  uploadedAt: string;
  verificationStatus: string; // PENDING, VERIFIED, REJECTED
  verifiedBy?: string;
  verifiedAt?: string;
  remarks?: string;
}

export interface FraudCheckResult {
  checkId: number;
  loanId: number;
  applicantId: number;
  panNumber?: string;
  aadhaarNumber?: string;
  phoneNumber?: string;
  email?: string;
  fraudTags: string[];
  riskLevel: string;
  riskScore: number;
  apiRemarks?: string;
  checkedAt: string;
  externalApiResponse?: any;
}

export interface DocumentVerificationRequest {
  documentId: number;
  verificationStatus: string; // VERIFIED, REJECTED
  remarks?: string;
}

export interface DocumentResubmissionRequest {
  loanId: number;
  applicantId: number;
  documentTypes: string[];
  reason: string;
  remarks?: string;
}

export interface FraudScreeningTriggerRequest {
  loanId: number;
  applicantId: number;
}

@Injectable({
  providedIn: 'root'
})
export class LoanOfficerService {
  private apiUrl = `${environment.apiUrl}/loan-officer`;

  constructor(private http: HttpClient) {}

  // Get assigned loans for an officer
  getAssignedLoans(officerId: number): Observable<LoanScreeningResponse[]> {
    return this.http.get<LoanScreeningResponse[]>(`${this.apiUrl}/${officerId}/assigned-loans`);
  }

  // Get loan details for screening
  getLoanDetailsForScreening(assignmentId: number): Observable<LoanScreeningResponse> {
    return this.http.get<LoanScreeningResponse>(`${this.apiUrl}/assignment/${assignmentId}/details`);
  }

  // Process loan screening (approve/reject)
  processLoanScreening(officerId: number, request: LoanScreeningRequest): Observable<LoanScreeningResponse> {
    return this.http.post<LoanScreeningResponse>(`${this.apiUrl}/${officerId}/process-screening`, request);
  }

  // Escalate to compliance
  escalateToCompliance(assignmentId: number, remarks?: string): Observable<LoanScreeningResponse> {
    let params = new HttpParams();
    if (remarks) {
      params = params.set('remarks', remarks);
    }
    return this.http.post<LoanScreeningResponse>(
      `${this.apiUrl}/assignment/${assignmentId}/escalate`,
      null,
      { params }
    );
  }

  // Get loan documents for verification
  getLoanDocuments(loanId: number): Observable<LoanDocument[]> {
    return this.http.get<LoanDocument[]>(`${this.apiUrl}/loan/${loanId}/documents`);
  }

  // Verify document
  verifyDocument(officerId: number, request: DocumentVerificationRequest): Observable<LoanDocument> {
    return this.http.post<LoanDocument>(`${this.apiUrl}/${officerId}/verify-document`, request);
  }

  // Request document resubmission
  requestDocumentResubmission(officerId: number, request: DocumentResubmissionRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/${officerId}/request-resubmission`, request);
  }

  // Trigger fraud screening
  triggerFraudScreening(officerId: number, request: FraudScreeningTriggerRequest): Observable<FraudCheckResult> {
    return this.http.post<FraudCheckResult>(`${this.apiUrl}/${officerId}/trigger-fraud-check`, request);
  }

  // Get fraud check results
  getFraudCheckResults(loanId: number): Observable<FraudCheckResult> {
    return this.http.get<FraudCheckResult>(`${this.apiUrl}/loan/${loanId}/fraud-check`);
  }

  // Calculate dashboard statistics
  calculateStats(loans: LoanScreeningResponse[]): DashboardStats {
    return {
      totalAssigned: loans.length,
      pendingReview: loans.filter(l => l.status === 'PENDING' || l.status === 'ASSIGNED').length,
      approved: loans.filter(l => l.status === 'APPROVED').length,
      rejected: loans.filter(l => l.status === 'REJECTED').length,
      escalated: loans.filter(l => l.status === 'ESCALATED').length,
      highRiskCount: loans.filter(l => l.riskLevel === 'HIGH').length,
      mediumRiskCount: loans.filter(l => l.riskLevel === 'MEDIUM').length,
      lowRiskCount: loans.filter(l => l.riskLevel === 'LOW').length
    };
  }

  // Utility methods
  formatCurrency(amount: number): string {
    return '₹' + amount.toLocaleString('en-IN');
  }

  formatDate(dateString: string): string {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { 
      year: 'numeric', 
      month: 'short', 
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getRiskColor(riskLevel: string): string {
    switch (riskLevel) {
      case 'LOW': return 'success';
      case 'MEDIUM': return 'warning';
      case 'HIGH': return 'danger';
      default: return 'secondary';
    }
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'PENDING':
      case 'ASSIGNED': return 'warning';
      case 'APPROVED': return 'success';
      case 'REJECTED': return 'danger';
      case 'ESCALATED': return 'info';
      case 'UNDER_REVIEW': return 'primary';
      default: return 'secondary';
    }
  }
}
