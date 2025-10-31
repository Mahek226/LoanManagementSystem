import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

// ==================== Interfaces ====================

export interface ComplianceEscalation {
  assignmentId: number;
  loanId: number;
  applicantId: number;
  applicantName: string;
  loanType: string;
  loanAmount: number;
  riskScore: number;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  status: string;
  remarks?: string;
  assignedAt: string;
  processedAt?: string;
  officerId?: number;
  officerName?: string;
  officerType?: string;
  escalationReason?: string;
  fraudIndicators?: string[];
}

export interface ComplianceDecisionRequest {
  assignmentId: number;
  action: 'APPROVE' | 'REJECT' | 'REQUEST_MORE_INFO';
  remarks?: string;
  rejectionReason?: string;
  additionalChecks?: string[];
}

export interface AdditionalDocumentRequest {
  loanId: number;
  applicantId: number;
  documentTypes: string[];
  reason: string;
  remarks?: string;
}

export interface FraudHistoryRecord {
  recordId: number;
  applicantId: number;
  loanId?: number;
  fraudType: string;
  fraudTags: string[];
  riskLevel: string;
  riskScore: number;
  detectedAt: string;
  resolvedAt?: string;
  status: string;
  remarks?: string;
}

export interface ComplianceDecisionResponse {
  assignmentId: number;
  loanId: number;
  status: string;
  decision: string;
  processedBy: string;
  processedAt: string;
  remarks?: string;
}

export interface DashboardStats {
  totalEscalations: number;
  pendingReview: number;
  approved: number;
  rejected: number;
  highRisk: number;
  criticalRisk: number;
  avgDecisionTime?: number;
  todayReviewed?: number;
}

export interface KYCVerificationRequest {
  applicantId: number;
  panNumber: string;
  aadhaarNumber: string;
  verificationType: 'PAN' | 'AADHAAR' | 'BOTH';
}

export interface KYCVerificationResponse {
  verified: boolean;
  panStatus?: string;
  aadhaarStatus?: string;
  nameMatch: boolean;
  addressMatch: boolean;
  duplicateFound: boolean;
  remarks?: string;
}

export interface AMLScreeningRequest {
  applicantId: number;
  applicantName: string;
  panNumber: string;
  checkTypes: string[]; // ['RBI_DEFAULTERS', 'FATF_SANCTIONS', 'OFAC', 'INTERNAL_BLACKLIST']
}

export interface AMLScreeningResponse {
  applicantId: number;
  screeningDate: string;
  overallRisk: 'CLEAR' | 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  findings: AMLFinding[];
  isPEP: boolean; // Politically Exposed Person
  recommendations: string[];
}

export interface AMLFinding {
  source: string; // 'RBI_DEFAULTERS', 'FATF', 'OFAC', etc.
  matchType: 'EXACT' | 'PARTIAL' | 'NONE';
  matchScore: number;
  details: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
}

export interface ComplianceAuditLog {
  logId: number;
  assignmentId: number;
  loanId: number;
  applicantId: number;
  officerId: number;
  officerName: string;
  action: string;
  decision?: string;
  remarks?: string;
  timestamp: string;
  ipAddress?: string;
  checksPerformed?: string[];
}

export interface RiskCorrelationAnalysis {
  loanId: number;
  applicantId: number;
  fraudTags: string[];
  defaulterHistory: boolean;
  transactionAnomalies: string[];
  complianceRiskRating: number; // 1-5
  riskFactors: RiskFactor[];
  recommendation: string;
}

export interface RiskFactor {
  category: string;
  description: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  weight: number;
}

// ==================== Service ====================

@Injectable({
  providedIn: 'root'
})
export class ComplianceOfficerService {
  private apiUrl = `${environment.apiUrl}/compliance-officer`;
  private profileUrl = `${environment.apiUrl}/profile`;

  constructor(private http: HttpClient) {}

  // ==================== API Methods ====================

  /**
   * Get all escalated loans for compliance review
   */
  getEscalations(): Observable<ComplianceEscalation[]> {
    return this.http.get<ComplianceEscalation[]>(`${this.apiUrl}/escalations`);
  }

  /**
   * Get detailed information for a specific escalation
   */
  getEscalationDetails(assignmentId: number): Observable<ComplianceEscalation> {
    return this.http.get<ComplianceEscalation>(`${this.apiUrl}/assignment/${assignmentId}/details`);
  }

  /**
   * Process compliance decision (approve/reject/request more info)
   */
  processDecision(complianceOfficerId: number, request: ComplianceDecisionRequest): Observable<ComplianceDecisionResponse> {
    return this.http.post<ComplianceDecisionResponse>(
      `${this.apiUrl}/${complianceOfficerId}/process-decision`,
      request
    );
  }

  /**
   * Request additional documents from applicant
   */
  requestAdditionalDocuments(complianceOfficerId: number, request: AdditionalDocumentRequest): Observable<any> {
    return this.http.post(
      `${this.apiUrl}/${complianceOfficerId}/request-documents`,
      request
    );
  }

  /**
   * Get fraud history for an applicant
   */
  getFraudHistory(applicantId: number): Observable<FraudHistoryRecord[]> {
    return this.http.get<FraudHistoryRecord[]>(`${this.apiUrl}/applicant/${applicantId}/fraud-history`);
  }

  /**
   * Get loan documents for review
   */
  getLoanDocuments(loanId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/loan/${loanId}/documents`);
  }

  /**
   * Get compliance officer profile
   */
  getOfficerProfile(officerId: number): Observable<any> {
    return this.http.get(`${this.profileUrl}/compliance-officer/${officerId}`);
  }

  /**
   * Update compliance officer profile
   */
  updateOfficerProfile(officerId: number, profileData: any): Observable<any> {
    return this.http.put(`${this.profileUrl}/compliance-officer/${officerId}`, profileData);
  }

  /**
   * Perform KYC reverification
   */
  verifyKYC(request: KYCVerificationRequest): Observable<KYCVerificationResponse> {
    return this.http.post<KYCVerificationResponse>(`${this.apiUrl}/kyc-verification`, request);
  }

  /**
   * Perform AML & Sanctions screening
   */
  performAMLScreening(request: AMLScreeningRequest): Observable<AMLScreeningResponse> {
    return this.http.post<AMLScreeningResponse>(`${this.apiUrl}/aml-screening`, request);
  }

  /**
   * Get risk correlation analysis
   */
  getRiskCorrelationAnalysis(loanId: number): Observable<RiskCorrelationAnalysis> {
    return this.http.get<RiskCorrelationAnalysis>(`${this.apiUrl}/loan/${loanId}/risk-correlation`);
  }

  /**
   * Get audit logs for a specific loan/assignment
   */
  getAuditLogs(assignmentId: number): Observable<ComplianceAuditLog[]> {
    return this.http.get<ComplianceAuditLog[]>(`${this.apiUrl}/assignment/${assignmentId}/audit-logs`);
  }

  /**
   * Get all audit logs for compliance officer
   */
  getAllAuditLogs(officerId: number): Observable<ComplianceAuditLog[]> {
    return this.http.get<ComplianceAuditLog[]>(`${this.apiUrl}/${officerId}/audit-logs`);
  }

  /**
   * Generate compliance report PDF
   */
  generateComplianceReport(assignmentId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/assignment/${assignmentId}/generate-report`, {
      responseType: 'blob'
    });
  }

  /**
   * Check if applicant is on RBI defaulters list
   */
  checkRBIDefaulters(panNumber: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/check-rbi-defaulters/${panNumber}`);
  }

  /**
   * Check if applicant is on sanctions list (FATF/OFAC)
   */
  checkSanctionsList(applicantName: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/check-sanctions`, {
      params: { name: applicantName }
    });
  }

  /**
   * Check internal blacklist
   */
  checkInternalBlacklist(applicantId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/check-blacklist/${applicantId}`);
  }

  /**
   * Check if applicant is a Politically Exposed Person (PEP)
   */
  checkPEPStatus(applicantName: string, panNumber: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/check-pep`, {
      params: { name: applicantName, pan: panNumber }
    });
  }

  // ==================== Utility Methods ====================

  /**
   * Calculate dashboard statistics from escalations
   */
  calculateStats(escalations: ComplianceEscalation[]): DashboardStats {
    return {
      totalEscalations: escalations.length,
      pendingReview: escalations.filter(e => e.status === 'PENDING' || e.status === 'ESCALATED_TO_COMPLIANCE').length,
      approved: escalations.filter(e => e.status === 'APPROVED').length,
      rejected: escalations.filter(e => e.status === 'REJECTED').length,
      highRisk: escalations.filter(e => e.riskLevel === 'HIGH').length,
      criticalRisk: escalations.filter(e => e.riskLevel === 'CRITICAL').length
    };
  }

  /**
   * Format currency
   */
  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(amount);
  }

  /**
   * Format date
   */
  formatDate(dateString: string): string {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }).format(date);
  }

  /**
   * Get risk level color
   */
  getRiskColor(riskLevel: string): string {
    const colors: { [key: string]: string } = {
      'LOW': '#10b981',      // Green
      'MEDIUM': '#f59e0b',   // Orange
      'HIGH': '#ef4444',     // Red
      'CRITICAL': '#dc2626'  // Dark Red
    };
    return colors[riskLevel] || '#64748b';
  }

  /**
   * Get status color
   */
  getStatusColor(status: string): string {
    const colors: { [key: string]: string } = {
      'PENDING': '#f59e0b',
      'ESCALATED_TO_COMPLIANCE': '#ef4444',
      'APPROVED': '#10b981',
      'REJECTED': '#ef4444',
      'UNDER_REVIEW': '#3b82f6',
      'REQUEST_MORE_INFO': '#f59e0b'
    };
    return colors[status] || '#64748b';
  }

  /**
   * Get status badge class
   */
  getStatusBadgeClass(status: string): string {
    const classes: { [key: string]: string } = {
      'PENDING': 'badge bg-warning',
      'ESCALATED_TO_COMPLIANCE': 'badge bg-danger',
      'APPROVED': 'badge bg-success',
      'REJECTED': 'badge bg-danger',
      'UNDER_REVIEW': 'badge bg-info',
      'REQUEST_MORE_INFO': 'badge bg-warning'
    };
    return classes[status] || 'badge bg-secondary';
  }

  /**
   * Get risk badge class
   */
  getRiskBadgeClass(riskLevel: string): string {
    const classes: { [key: string]: string } = {
      'LOW': 'badge bg-success',
      'MEDIUM': 'badge bg-warning',
      'HIGH': 'badge bg-danger',
      'CRITICAL': 'badge bg-danger text-white'
    };
    return classes[riskLevel] || 'badge bg-secondary';
  }

  /**
   * Export escalations to CSV
   */
  exportToCSV(escalations: ComplianceEscalation[], filename: string = 'compliance-escalations.csv'): void {
    const headers = ['Assignment ID', 'Loan ID', 'Applicant Name', 'Loan Type', 'Amount', 'Risk Level', 'Risk Score', 'Status', 'Assigned At'];
    const rows = escalations.map(e => [
      e.assignmentId,
      e.loanId,
      e.applicantName,
      e.loanType,
      e.loanAmount,
      e.riskLevel,
      e.riskScore,
      e.status,
      this.formatDate(e.assignedAt)
    ]);

    const csvContent = [
      headers.join(','),
      ...rows.map(row => row.join(','))
    ].join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', filename);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  /**
   * Get priority level based on risk and status
   */
  getPriorityLevel(escalation: ComplianceEscalation): 'URGENT' | 'HIGH' | 'MEDIUM' | 'LOW' {
    if (escalation.riskLevel === 'CRITICAL') return 'URGENT';
    if (escalation.riskLevel === 'HIGH' && escalation.status === 'ESCALATED_TO_COMPLIANCE') return 'URGENT';
    if (escalation.riskLevel === 'HIGH') return 'HIGH';
    if (escalation.riskLevel === 'MEDIUM') return 'MEDIUM';
    return 'LOW';
  }

  /**
   * Get priority color
   */
  getPriorityColor(priority: string): string {
    const colors: { [key: string]: string } = {
      'URGENT': '#dc2626',
      'HIGH': '#ef4444',
      'MEDIUM': '#f59e0b',
      'LOW': '#10b981'
    };
    return colors[priority] || '#64748b';
  }
}
