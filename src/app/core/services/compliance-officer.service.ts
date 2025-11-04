import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
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

// ==================== External Fraud Data Interfaces ====================

export interface ExternalFraudData {
  lastName: string;
  firstName: string;
  gender: string;
  dateOfBirth: string;
  panNumber: string;
  aadhaarNumber: string;
  phoneNumber: string;
  email: string;
  personFound: boolean;
  personId: number;
  externalPersonId: number;
  matchedBy: string;
  lastChecked: string;
  lastUpdated: string;
  searchCriteria: SearchCriteria;
  bankRecords: BankRecords;
  criminalRecords: CriminalRecords;
  loanHistory: LoanHistory;
  governmentDocuments: GovernmentDocuments;
  riskAssessment: RiskAssessment;
}

export interface SearchCriteria {
  aadhaarNumber: string;
  panNumber: string;
}

export interface BankRecords {
  totalBalance: number;
  totalAccounts: number;
  activeAccounts: number;
  accounts: BankAccount[];
}

export interface BankAccount {
  balance: number;
  accountType: string;
  lastTransaction: string;
  bankName: string;
  isActive: boolean;
}

export interface CriminalRecords {
  totalCases: number;
  cases: CriminalCase[];
}

export interface CriminalCase {
  caseNumber: string;
  caseType: string;
  description: string;
  courtName: string;
  status: string;
  verdictDate?: string;
}

export interface LoanHistory {
  totalLoans: number;
  defaultedLoans: number;
  defaultRate: number;
  totalAmountBorrowed: number;
  totalOutstandingBalance: number;
  loans: LoanRecord[];
}

export interface LoanRecord {
  loanId: number;
  loanType: string;
  institutionName: string;
  loanAmount: number;
  outstandingBalance: number;
  startDate: string;
  endDate?: string;
  status: string;
  defaultFlag: boolean;
}

export interface GovernmentDocuments {
  verifiedDocuments: number;
  totalDocuments: number;
  verificationRate: number;
  documents: GovernmentDocument[];
}

export interface GovernmentDocument {
  documentId: number;
  documentType: string;
  documentNumber: string;
  issuingAuthority: string;
  issuedDate: string;
  expiryDate?: string;
  verificationStatus: string;
}

export interface RiskAssessment {
  riskLevel: string;
  overallRiskScore: number;
  recommendation: string;
  assessmentDate: string;
  riskFactors: string[];
  riskBreakdown: RiskBreakdown;
}

export interface RiskBreakdown {
  documentRisk: string;
  criminalRisk: string;
  financialRisk: string;
  bankingRisk: string;
}

// Legacy interfaces for backward compatibility
export interface BankRecord {
  id: number;
  bankName: string;
  accountNumber: string; // masked
  accountType: string;
  balanceAmount: number;
  lastTransactionDate: string;
  isActive: boolean;
  createdAt: string;
}

export interface CriminalRecord {
  id: number;
  caseNumber: string;
  caseType: string;
  description: string;
  courtName: string;
  status: string; // OPEN, CLOSED, CONVICTED, ACQUITTED
  verdictDate?: string;
  createdAt: string;
}

export interface LoanHistoryRecord {
  id: number;
  loanType: string;
  institutionName: string;
  loanAmount: number;
  outstandingBalance: number;
  startDate: string;
  endDate?: string;
  status: string; // ACTIVE, CLOSED, DEFAULTED
  defaultFlag: boolean;
  createdAt: string;
}

export interface FraudSummary {
  totalBankAccounts: number;
  totalCriminalCases: number;
  totalLoanHistory: number;
  hasActiveCriminalCases: boolean;
  hasDefaultedLoans: boolean;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
}

export interface DocumentResubmissionRequest {
  documentId: number;
  assignmentId: number;
  complianceOfficerId: number;
  reason: string;
  instructions: string;
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
}

// DTO that matches the backend DocumentResubmissionRequestDTO
export interface DocumentResubmissionRequestDTO {
  documentId: number;
  loanId: number;
  complianceOfficerId: number;
  resubmissionReason: string;
  specificInstructions?: string;
  directToApplicant?: boolean;
}

// Enhanced Loan Screening Response interfaces
export interface EnhancedLoanScreeningResponse {
  assignmentId: number;
  loanId: number;
  applicantId: number;
  applicantName: string;
  loanType: string;
  loanAmount: number;
  status: string;
  remarks?: string;
  assignedAt: string;
  processedAt?: string;
  officerId: number;
  officerName: string;
  officerType: string;
  normalizedRiskScore: NormalizedRiskScore;
  scoringBreakdown: ScoringBreakdown;
  ruleViolations: RuleViolation[];
  finalRecommendation: string;
  canApproveReject: boolean;
}

export interface NormalizedRiskScore {
  finalScore: number;
  riskLevel: string;
  scoreInterpretation: string;
}

export interface ScoringBreakdown {
  internalScoring: InternalScoring;
  externalScoring: ExternalScoring;
  severityBreakdown: SeverityBreakdown;
  normalizationMethod: string;
  combinationFormula: string;
}

export interface InternalScoring {
  rawScore: number;
  maxPossibleScore: number;
  normalizedScore: number;
  riskLevel: string;
  violatedRulesCount: number;
  categories: string[];
}

export interface ExternalScoring {
  rawScore: number;
  maxPossibleScore: number;
  normalizedScore: number;
  riskLevel: string;
  violatedRulesCount: number;
  personFound: boolean;
  categories: string[];
}

export interface SeverityBreakdown {
  criticalCount: number;
  highCount: number;
  mediumCount: number;
  lowCount: number;
  totalViolations: number;
  severityScore: number;
  pointsScore: number;
}

export interface RuleViolation {
  source: string;
  ruleCode: string;
  ruleName: string;
  category: string;
  severity: string;
  points: number;
  description: string;
  details: string;
  detectedAt: string;
}

export interface ComplianceVerdict {
  assignmentId: number;
  complianceOfficerId: number;
  verdict: 'RECOMMEND_APPROVE' | 'RECOMMEND_REJECT' | 'REQUEST_MORE_INFO';
  verdictReason: string;
  detailedRemarks?: string;
  rejectionReasons?: string[];
  documentsToResubmit?: DocumentResubmissionInfo[];
  additionalChecksRequired?: string[];
  targetLoanOfficerId?: number;
}

export interface DocumentResubmissionInfo {
  documentId: number;
  documentType: string;
  resubmissionReason: string;
  specificInstructions?: string;
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

  // ==================== External Fraud Data Methods ====================

  /**
   * Get external fraud data for an applicant
   */
  getExternalFraudData(applicantId: number): Observable<ExternalFraudData> {
    return this.http.get<ExternalFraudData>(`${this.apiUrl}/applicant/${applicantId}/external-fraud-data`).pipe(
      catchError((error: any) => {
        console.warn('External fraud data endpoint not available, using mock data');
        return of(this.getMockExternalFraudData(applicantId));
      })
    );
  }

  /**
   * Get bank records for an applicant
   */
  getBankRecords(applicantId: number): Observable<BankRecord[]> {
    return this.http.get<BankRecord[]>(`${this.apiUrl}/applicant/${applicantId}/bank-records`);
  }

  /**
   * Get criminal records for an applicant
   */
  getCriminalRecords(applicantId: number): Observable<CriminalRecord[]> {
    return this.http.get<CriminalRecord[]>(`${this.apiUrl}/applicant/${applicantId}/criminal-records`);
  }

  /**
   * Get loan history for an applicant
   */
  getLoanHistory(applicantId: number): Observable<LoanHistoryRecord[]> {
    return this.http.get<LoanHistoryRecord[]>(`${this.apiUrl}/applicant/${applicantId}/loan-history`);
  }

  /**
   * Get comprehensive review details for compliance officer
   */
  getComprehensiveReviewDetails(assignmentId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/assignment/${assignmentId}/comprehensive-review`);
  }

  /**
   * Submit compliance verdict to loan officer
   */
  submitComplianceVerdict(verdict: ComplianceVerdict): Observable<any> {
    return this.http.post(`${this.apiUrl}/submit-verdict`, verdict);
  }

  /**
   * Request document resubmission
   */
  requestDocumentResubmission(request: DocumentResubmissionRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/document/${request.documentId}/request-resubmission`, request);
  }

  /**
   * Request document resubmission using DTO structure
   */
  requestDocumentResubmissionDTO(request: DocumentResubmissionRequestDTO): Observable<any> {
    return this.http.post(`${this.apiUrl}/document/${request.documentId}/request-resubmission`, request);
  }

  /**
   * Get enhanced loan screening details
   */
  getEnhancedLoanDetails(assignmentId: number): Observable<EnhancedLoanScreeningResponse> {
    // Use the compliance-specific enhanced screening endpoint
    return this.http.get<EnhancedLoanScreeningResponse>(`${this.apiUrl}/assignment/${assignmentId}/enhanced-screening`).pipe(
      catchError(error => {
        console.warn('Enhanced screening endpoint not available, using mock data');
        return of(this.getMockEnhancedScreeningData(assignmentId));
      })
    );
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

  // ==================== Mock Data Methods ====================

  /**
   * Generate mock enhanced screening data when API is not available
   */
  private getMockEnhancedScreeningData(assignmentId: number): EnhancedLoanScreeningResponse {
    return {
      assignmentId: assignmentId,
      loanId: assignmentId + 100,
      applicantId: assignmentId + 1000,
      applicantName: 'Jay Soni',
      loanType: 'HOME',
      loanAmount: 12000,
      status: 'ESCALATED_TO_COMPLIANCE',
      assignedAt: new Date().toISOString(),
      officerId: 1,
      officerName: 'Compliance Officer',
      officerType: 'COMPLIANCE_OFFICER',
      normalizedRiskScore: {
        finalScore: 72,
        riskLevel: 'HIGH',
        scoreInterpretation: 'High risk applicant requiring compliance review'
      },
      scoringBreakdown: {
        internalScoring: {
          rawScore: 43,
          maxPossibleScore: 100,
          normalizedScore: 57.6,
          riskLevel: 'HIGH',
          violatedRulesCount: 5,
          categories: ['Identity', 'Financial', 'Employment']
        },
        externalScoring: {
          rawScore: 28,
          maxPossibleScore: 50,
          normalizedScore: 86.4,
          riskLevel: 'HIGH',
          violatedRulesCount: 3,
          personFound: true,
          categories: ['Credit History', 'Fraud Records', 'Legal Issues']
        },
        severityBreakdown: {
          criticalCount: 0,
          highCount: 2,
          mediumCount: 3,
          lowCount: 0,
          totalViolations: 5,
          severityScore: 68,
          pointsScore: 72
        },
        normalizationMethod: 'WEIGHTED_AVERAGE',
        combinationFormula: 'INTERNAL_70_EXTERNAL_30'
      },
      ruleViolations: [
        {
          ruleCode: 'ID_001',
          ruleName: 'Identity Verification Check',
          category: 'IDENTITY',
          severity: 'HIGH',
          description: 'Identity Verification Mismatch',
          details: 'Discrepancy found between provided identity documents',
          source: 'INTERNAL',
          points: 15,
          detectedAt: new Date().toISOString()
        },
        {
          ruleCode: 'FIN_002',
          ruleName: 'Income Documentation Check',
          category: 'FINANCIAL',
          severity: 'MEDIUM',
          description: 'Income Documentation Gap',
          details: 'Incomplete income verification documentation',
          source: 'INTERNAL',
          points: 8,
          detectedAt: new Date().toISOString()
        },
        {
          ruleCode: 'EXT_001',
          ruleName: 'Loan History Check',
          category: 'LOAN_HISTORY',
          severity: 'MEDIUM',
          description: 'Previous Loan Default',
          details: 'Previous loan default found in external database',
          source: 'EXTERNAL',
          points: 12,
          detectedAt: new Date().toISOString()
        }
      ],
      finalRecommendation: 'ESCALATE_TO_COMPLIANCE',
      canApproveReject: true
    };
  }

  /**
   * Generate mock external fraud data when API is not available
   */
  private getMockExternalFraudData(applicantId: number): ExternalFraudData {
    return {
      lastName: 'Kumar',
      firstName: 'Rajesh',
      gender: 'MALE',
      dateOfBirth: '1982-11-08',
      panNumber: 'ABC****',
      aadhaarNumber: '****9012',
      phoneNumber: '9876543212',
      email: 'rajesh.kumar@email.com',
      personFound: true,
      personId: 3,
      externalPersonId: 3,
      matchedBy: 'PAN',
      lastChecked: new Date().toISOString(),
      lastUpdated: new Date().toISOString(),
      searchCriteria: {
        aadhaarNumber: '****9012',
        panNumber: 'ABC****'
      },
      bankRecords: {
        totalBalance: 19600.0,
        totalAccounts: 10,
        activeAccounts: 2,
        accounts: [
          {
            balance: 5000.00,
            accountType: 'SAVINGS',
            lastTransaction: '2024-08-15',
            bankName: 'HDFC Bank',
            isActive: true
          },
          {
            balance: 2000.00,
            accountType: 'CURRENT',
            lastTransaction: '2024-07-20',
            bankName: 'ICICI Bank',
            isActive: false
          }
        ]
      },
      criminalRecords: {
        totalCases: 2,
        cases: [
          {
            caseNumber: 'CR/2019/1234',
            caseType: 'FINANCIAL_FRAUD',
            description: 'Cheque bounce case under Section 138 NI Act',
            courtName: 'Metropolitan Magistrate Court',
            status: 'CONVICTED',
            verdictDate: '2020-03-15'
          },
          {
            caseNumber: 'CR/2021/5678',
            caseType: 'FORGERY',
            description: 'Document forgery case under IPC Section 420',
            courtName: 'Sessions Court',
            status: 'OPEN'
          }
        ]
      },
      loanHistory: {
        totalLoans: 4,
        defaultedLoans: 3,
        defaultRate: 75.0,
        totalAmountBorrowed: 2700000.0,
        totalOutstandingBalance: 2230000.0,
        loans: [
          {
            loanId: 5,
            loanType: 'PERSONAL',
            institutionName: 'HDFC Bank',
            loanAmount: 400000.00,
            outstandingBalance: 400000.00,
            startDate: '2020-05-10',
            status: 'DEFAULTED',
            defaultFlag: true
          },
          {
            loanId: 6,
            loanType: 'BUSINESS',
            institutionName: 'ICICI Bank',
            loanAmount: 1500000.00,
            outstandingBalance: 1200000.00,
            startDate: '2019-02-28',
            status: 'DEFAULTED',
            defaultFlag: true
          }
        ]
      },
      governmentDocuments: {
        verifiedDocuments: 2,
        totalDocuments: 3,
        verificationRate: 66.66666666666666,
        documents: [
          {
            documentId: 6,
            documentType: 'PAN',
            documentNumber: 'KLMNO9012P',
            issuingAuthority: 'INCOME TAX DEPARTMENT',
            issuedDate: '2008-03-10',
            verificationStatus: 'VERIFIED'
          },
          {
            documentId: 7,
            documentType: 'AADHAAR',
            documentNumber: '345678901234',
            issuingAuthority: 'UIDAI',
            issuedDate: '2011-07-25',
            verificationStatus: 'VERIFIED'
          },
          {
            documentId: 8,
            documentType: 'DRIVING_LICENSE',
            documentNumber: 'DL1234567890',
            issuingAuthority: 'RTO',
            issuedDate: '2015-01-20',
            expiryDate: '2025-01-19',
            verificationStatus: 'EXPIRED'
          }
        ]
      },
      riskAssessment: {
        riskLevel: 'CRITICAL',
        overallRiskScore: 72,
        recommendation: 'REJECT',
        assessmentDate: new Date().toISOString(),
        riskFactors: [
          'Criminal records found: 2 cases',
          'Loan defaults found: 3 loans',
          'Inactive bank accounts: 8',
          'Unverified documents: 1'
        ],
        riskBreakdown: {
          documentRisk: 'MEDIUM',
          criminalRisk: 'HIGH',
          financialRisk: 'HIGH',
          bankingRisk: 'MEDIUM'
        }
      }
    };
  }
}
