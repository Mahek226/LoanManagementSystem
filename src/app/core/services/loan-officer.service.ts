import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
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
  // Compliance verdict details (when available)
  complianceVerdict?: string; // APPROVED, REJECTED, FLAGGED, CONDITIONAL_APPROVAL
  complianceVerdictReason?: string;
  complianceRemarks?: string;
  complianceOfficerName?: string;
  complianceVerdictTimestamp?: string;
  nextAction?: string; // What the loan officer should do next
  hasComplianceVerdict?: boolean; // Flag to indicate if compliance review is complete
  // Document verification status
  documentsVerified?: boolean; // Flag to indicate if all required documents are verified
  documentVerificationStatus?: string; // PENDING, VERIFIED, REJECTED, PARTIAL
  totalDocuments?: number; // Total number of required documents
  verifiedDocuments?: number; // Number of verified documents
  rejectedDocuments?: number; // Number of rejected documents
}

export interface LoanScreeningRequest {
  decision: string; // APPROVE, REJECT
  remarks: string;
  riskAssessment: number;
  incomeVerified: boolean;
  creditCheckPassed: boolean;
  collateralVerified: boolean;
  employmentVerified: boolean;
  identityVerified: boolean;
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
  // Financial Performance
  totalFundedAmount: number;
  averageInterestRate: number;
  averageDTI: number;
  approvalRate: number;
  pullThroughRate: number;
  defaultRate: number;
  // Loan Quality
  goodLoansCount: number;
  badLoansCount: number;
  underwritingAccuracy: number;
  portfolioYield: number;
  loanQualityIndex: number;
  // Breakdown by Purpose
  personalLoans: number;
  homeLoans: number;
  carLoans: number;
  educationLoans: number;
  businessLoans: number;
  // Monthly Trends
  monthlyApplications: MonthlyTrend[];
  monthlyApprovals: MonthlyTrend[];
  monthlyDefaults: MonthlyTrend[];
  // Geographic Data
  loansByState: StateData[];
}

export interface MonthlyTrend {
  month: string;
  count: number;
  amount: number;
}

export interface StateData {
  state: string;
  count: number;
  amount: number;
}

export interface LoanDocument {
  documentId: number;
  documentType: string;
  documentName: string;
  documentUrl: string;
  cloudinaryUrl?: string; // Alternative URL property
  uploadedAt: string;
  verificationStatus: string; // PENDING, VERIFIED, REJECTED
  verifiedBy?: string;
  verifiedAt?: string;
  remarks?: string;
  extractedJson?: string; // JSON string of extracted data
  extractedText?: string; // Plain text extracted data
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
  flaggedRules?: FlaggedRule[];
}

export interface FlaggedRule {
  ruleId?: number;
  ruleCode?: string;
  ruleName: string;
  description: string;
  category: string;
  severity: string;
  points?: number;
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
  // Compliance verdict details (when available)
  complianceVerdict?: string;
  complianceVerdictReason?: string;
  complianceRemarks?: string;
  complianceOfficerName?: string;
  complianceVerdictTimestamp?: string;
  nextAction?: string;
  hasComplianceVerdict?: boolean;
}

export interface NormalizedRiskScore {
  finalScore: number;
  riskLevel: string;
  scoreInterpretation: string;
}

export interface ScoringBreakdown {
  internalScoring: InternalScoring;
  externalScoring: ExternalScoring;
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

export interface ComprehensiveLoanView {
  // Basic Loan Information
  loanId: number;
  loanType: string;
  loanAmount: number;
  tenureMonths: number;
  interestRate: number;
  monthlyEmi: number;
  loanPurpose: string;
  status: string;
  appliedDate: string;
  approvedDate?: string;
  
  // Applicant Information
  applicantId: number;
  applicantName: string;
  email: string;
  phone: string;
  panNumber?: string;
  aadhaarNumber?: string;
  maritalStatus?: string;
  address?: string;
  city?: string;
  state?: string;
  country?: string;
  zipCode?: string;
  
  // Employment Information
  employerName?: string;
  designation?: string;
  employmentType?: string;
  monthlyIncome?: number;
  employmentStartDate?: string;
  education?: string;
  
  // Financial Information
  bankName?: string;
  accountNumber?: string;
  ifscCode?: string;
  creditScore?: number;
  existingDebt?: number;
  totalAssets?: number;
  totalLiabilities?: number;
  dtiRatio?: number;
  interestCoverageRatio?: number;
  
  // Property Information
  propertyAddress?: string;
  propertyValue?: number;
  propertyType?: string;
  
  // Residence Information
  residenceType?: string;
  yearsAtCurrentAddress?: number;
  
  // Risk Assessment
  riskLevel: string;
  riskScore: number;
  canApproveReject: boolean;
  
  // Assignment Information
  assignmentId?: number;
  assignedOfficerName?: string;
  assignedOfficerType?: string;
  assignedAt?: string;
  officerRemarks?: string;
  
  // Verification Status
  kycVerified: boolean;
  bankVerified: boolean;
  employmentVerified: boolean;
  incomeVerified: boolean;
  addressVerified: boolean;
  propertyVerified: boolean;
  
  // Related Data
  documents: LoanDocument[];
  references: LoanReference[];
  dependents: LoanDependent[];
  collaterals: LoanCollateral[];
}

export interface LoanReference {
  referenceId: number;
  name: string;
  relationship: string;
  phoneNumber: string;
  email?: string;
  address?: string;
}

export interface LoanDependent {
  dependentId: number;
  name: string;
  relationship: string;
  age: number;
  occupation?: string;
}

export interface LoanCollateral {
  collateralId: number;
  collateralType: string;
  description: string;
  estimatedValue: number;
  ownershipProof?: string;
  verificationStatus?: string;
}

// Document Resubmission Request from Compliance Officer
export interface ComplianceDocumentResubmissionRequest {
  resubmissionId: number;
  assignmentId: number;
  loanId: number;
  applicantId: number;
  applicantName: string;
  loanType: string;
  loanAmount: number;
  requestedDocuments: string; // JSON string of document types
  reason: string;
  additionalComments?: string;
  priorityLevel: number;
  requestedAt: string;
  requestedBy: string;
  complianceOfficerId: number;
  status: string; // REQUESTED, FORWARDED_TO_APPLICANT, REJECTED_BY_LOAN_OFFICER
}

// Request to process compliance document resubmission
export interface ProcessDocumentResubmissionRequest {
  resubmissionId: number;
  action: 'APPROVE' | 'REJECT';
  remarks?: string;
  rejectionReason?: string;
}

@Injectable({
  providedIn: 'root'
})
export class LoanOfficerService {
  private apiUrl = `${environment.apiUrl}/loan-officer`;
  private profileUrl = `${environment.apiUrl}/profile`;

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
  processLoanScreening(officerId: number, assignmentId: number, request: LoanScreeningRequest): Observable<LoanScreeningResponse> {
    return this.http.post<LoanScreeningResponse>(`${this.apiUrl}/${officerId}/screen-loan/${assignmentId}`, request);
  }

  // Extract documents for a loan (OLD - Spring Boot endpoint)
  extractDocuments(assignmentId: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/assignment/${assignmentId}/extract-documents`, {});
  }

  // Extract single document using FastAPI service
  extractSingleDocument(applicantId: number, documentType: string, file: File): Observable<any> {
    const formData = new FormData();
    formData.append('applicant_id', applicantId.toString());
    formData.append('document_type', documentType);
    formData.append('file', file);

    // Call FastAPI extraction service (adjust port as needed)
    return this.http.post<any>('http://localhost:8000/extract', formData);
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

  // Check document verification status for a loan
  getDocumentVerificationStatus(loanId: number): Observable<{
    documentsVerified: boolean;
    documentVerificationStatus: string;
    totalDocuments: number;
    verifiedDocuments: number;
    rejectedDocuments: number;
    pendingDocuments: number;
  }> {
    return this.http.get<any>(`${this.apiUrl}/loan/${loanId}/document-verification-status`);
  }

  // Verify document
  verifyDocument(officerId: number, request: DocumentVerificationRequest): Observable<LoanDocument> {
    return this.http.post<LoanDocument>(`${this.apiUrl}/${officerId}/verify-document`, request);
  }

  // Request document resubmission
  requestDocumentResubmission(officerId: number, request: DocumentResubmissionRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/${officerId}/request-resubmission`, request);
  }

  // Get comprehensive loan view with all related data
  getComprehensiveLoanView(loanId: number): Observable<ComprehensiveLoanView> {
    return this.http.get<ComprehensiveLoanView>(`${this.apiUrl}/loan/${loanId}/comprehensive-view`);
  }

  // Profile Management - Fixed for circular reference issues
  getOfficerProfile(officerId: number): Observable<any> {
    console.log('Making profile request to:', `${this.profileUrl}/loan-officer/${officerId}`);
    
    return this.http.get(`${this.profileUrl}/loan-officer/${officerId}`, {
      responseType: 'text'
    }).pipe(
      map((response: string) => {
        console.log('Raw profile response received (length):', response?.length);
        console.log('Response preview (first 200 chars):', response?.substring(0, 200));
        console.log('Response preview (last 200 chars):', response?.substring(response.length - 200));
        
        try {
          // Check if response is empty or null
          if (!response || response.trim() === '') {
            throw new Error('Empty response from server');
          }
          
          console.log('Attempting to extract profile data using regex patterns...');
          
          // Extract profile data using regex patterns to avoid JSON parsing issues
          const profileData: any = {};
          
          // Extract officerId
          const officerIdMatch = response.match(/"officerId"\s*:\s*(\d+)/);
          if (officerIdMatch) {
            profileData.officerId = parseInt(officerIdMatch[1]);
          }
          
          // Extract username
          const usernameMatch = response.match(/"username"\s*:\s*"([^"]+)"/);
          if (usernameMatch) {
            profileData.username = usernameMatch[1];
          }
          
          // Extract email
          const emailMatch = response.match(/"email"\s*:\s*"([^"]+)"/);
          if (emailMatch) {
            profileData.email = emailMatch[1];
          }
          
          // Extract firstName
          const firstNameMatch = response.match(/"firstName"\s*:\s*"([^"]*)"/);
          if (firstNameMatch) {
            profileData.firstName = firstNameMatch[1] || null;
          }
          
          // Extract lastName
          const lastNameMatch = response.match(/"lastName"\s*:\s*"([^"]*)"/);
          if (lastNameMatch) {
            profileData.lastName = lastNameMatch[1] || null;
          }
          
          // Extract loanType
          const loanTypeMatch = response.match(/"loanType"\s*:\s*"([^"]*)"/);
          if (loanTypeMatch) {
            profileData.loanType = loanTypeMatch[1] || null;
          }
          
          // Extract createdAt
          const createdAtMatch = response.match(/"createdAt"\s*:\s*"([^"]*)"/);
          if (createdAtMatch) {
            profileData.createdAt = createdAtMatch[1] || null;
          }
          
          // Extract updatedAt
          const updatedAtMatch = response.match(/"updatedAt"\s*:\s*"([^"]*)"/);
          if (updatedAtMatch) {
            profileData.updatedAt = updatedAtMatch[1] || null;
          }
          
          // Validate that we got the essential fields
          if (!profileData.officerId || !profileData.username || !profileData.email) {
            console.error('Missing essential profile fields:', profileData);
            throw new Error('Could not extract essential profile data from response');
          }
          
          console.log('Successfully extracted profile data using regex:', profileData);
          return profileData;
          
        } catch (parseError: any) {
          console.error('JSON Parse Error:', parseError);
          console.log('Failed to parse response length:', response?.length);
          
          // Try to identify the issue
          if (response?.includes('<!DOCTYPE html>')) {
            throw new Error('Server returned HTML instead of JSON. Check if the endpoint exists.');
          } else if (response?.includes('error') || response?.includes('exception')) {
            throw new Error('Server returned an error response');
          } else {
            throw new Error(`Circular reference detected in JSON response. Backend needs @JsonIgnore on relationships.`);
          }
        }
      }),
      catchError((error: any) => {
        console.error('Profile API error:', error);
        
        if (error.status === 0) {
          return throwError(() => new Error('Cannot connect to server. Please check if the backend is running.'));
        } else if (error.status === 404) {
          return throwError(() => new Error('Loan officer profile endpoint not found.'));
        } else if (error.status === 403) {
          return throwError(() => new Error('Access denied. Please check your permissions.'));
        } else if (error.status === 401) {
          return throwError(() => new Error('Authentication required. Please login again.'));
        } else if (error.status === 500) {
          return throwError(() => new Error('Server error. Please try again later.'));
        } else {
          return throwError(() => new Error(error.message || 'Failed to load profile'));
        }
      })
    );
  }

  // Alternative profile method with standard JSON parsing
  getOfficerProfileAlternative(officerId: number): Observable<any> {
    console.log('Trying alternative profile request to:', `${this.profileUrl}/loan-officer/${officerId}`);
    
    return this.http.get<any>(`${this.profileUrl}/loan-officer/${officerId}`).pipe(
      map((data: any) => {
        console.log('Alternative method - received data:', data);
        
        // Extract only needed fields to avoid circular references
        const profileData = {
          officerId: data.officerId,
          username: data.username,
          email: data.email,
          firstName: data.firstName || null,
          lastName: data.lastName || null,
          loanType: data.loanType || null,
          createdAt: data.createdAt || null,
          updatedAt: data.updatedAt || null
        };
        
        console.log('Alternative method - extracted profile:', profileData);
        return profileData;
      }),
      catchError((error: any) => {
        console.error('Alternative profile method error:', error);
        return throwError(() => new Error(`Alternative method failed: ${error.message || 'Unknown error'}`));
      })
    );
  }

  updateOfficerProfile(officerId: number, profileData: any): Observable<any> {
    return this.http.put(`${this.profileUrl}/loan-officer/${officerId}`, profileData);
  }

  // Change password
  changePassword(officerId: number, passwordData: { currentPassword: string, newPassword: string }): Observable<any> {
    console.log('Making password change request for officer:', officerId);
    return this.http.post(`${this.profileUrl}/change-password/LOAN_OFFICER/${officerId}`, passwordData).pipe(
      catchError((error: any) => {
        console.error('Password change API error:', error);
        return throwError(() => error);
      })
    );
  }

  // Trigger fraud screening
  triggerFraudScreening(officerId: number, request: FraudScreeningTriggerRequest): Observable<FraudCheckResult> {
    return this.http.post<FraudCheckResult>(`${this.apiUrl}/${officerId}/trigger-fraud-check`, request);
  }

  // Get fraud check results
  getFraudCheckResults(loanId: number): Observable<FraudCheckResult> {
    return this.http.get<FraudCheckResult>(`${this.apiUrl}/loan/${loanId}/fraud-check`);
  }

  // Get enhanced loan details with scoring breakdown
  getEnhancedLoanDetails(assignmentId: number): Observable<EnhancedLoanScreeningResponse> {
    return this.http.get<EnhancedLoanScreeningResponse>(`${environment.apiUrl}/enhanced-screening/loan/${assignmentId}`);
  }

  // ==================== Document Resubmission Requests from Compliance Officer ====================

  // Get document resubmission requests from compliance officers
  getDocumentResubmissionRequests(officerId: number): Observable<ComplianceDocumentResubmissionRequest[]> {
    return this.http.get<ComplianceDocumentResubmissionRequest[]>(`${this.apiUrl}/${officerId}/document-resubmission-requests`);
  }

  // Process document resubmission request (approve/reject)
  processDocumentResubmissionRequest(officerId: number, request: ProcessDocumentResubmissionRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/${officerId}/process-document-resubmission-request`, request);
  }

  // ==================== Compliance Verdict Management ====================

  // Get compliance verdict for a specific loan
  getComplianceVerdictForLoan(loanId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/loan/${loanId}/compliance-verdict`);
  }

  // Process loan after compliance verdict
  processLoanAfterCompliance(officerId: number, request: {
    loanId: number;
    assignmentId: number;
    decision: string; // APPROVE, REJECT
    remarks: string;
  }): Observable<LoanScreeningResponse> {
    return this.http.post<LoanScreeningResponse>(`${this.apiUrl}/${officerId}/process-after-compliance`, request);
  }

  // Calculate comprehensive dashboard statistics
  calculateStats(loans: LoanScreeningResponse[]): DashboardStats {
    const approved = loans.filter(l => l.status === 'APPROVED');
    const rejected = loans.filter(l => l.status === 'REJECTED');
    const totalProcessed = approved.length + rejected.length;
    
    // Financial calculations
    const totalFundedAmount = approved.reduce((sum, l) => sum + l.loanAmount, 0);
    const approvalRate = totalProcessed > 0 ? (approved.length / totalProcessed) * 100 : 0;
    
    // Loan type breakdown
    const personalLoans = loans.filter(l => l.loanType === 'Personal Loan').length;
    const homeLoans = loans.filter(l => l.loanType === 'Home Loan').length;
    const carLoans = loans.filter(l => l.loanType === 'Car Loan').length;
    const educationLoans = loans.filter(l => l.loanType === 'Education Loan').length;
    const businessLoans = loans.filter(l => l.loanType === 'Business Loan').length;
    
    // Quality metrics (good loans = LOW risk approved, bad loans = HIGH risk approved or any defaulted)
    const goodLoansCount = approved.filter(l => l.riskLevel === 'LOW').length;
    const badLoansCount = approved.filter(l => l.riskLevel === 'HIGH').length;
    const loanQualityIndex = approved.length > 0 ? (goodLoansCount / approved.length) * 100 : 0;
    
    return {
      totalAssigned: loans.length,
      pendingReview: loans.filter(l => l.status === 'PENDING' || l.status === 'ASSIGNED' || l.status === 'IN_PROGRESS').length,
      approved: approved.length,
      rejected: rejected.length,
      escalated: loans.filter(l => l.status === 'ESCALATED' || l.status === 'ESCALATED_TO_COMPLIANCE').length,
      highRiskCount: loans.filter(l => l.riskLevel === 'HIGH').length,
      mediumRiskCount: loans.filter(l => l.riskLevel === 'MEDIUM').length,
      lowRiskCount: loans.filter(l => l.riskLevel === 'LOW').length,
      // Financial Performance
      totalFundedAmount: totalFundedAmount,
      averageInterestRate: 8.5, // Mock data - should come from backend
      averageDTI: 35.2, // Mock data - should come from backend
      approvalRate: approvalRate,
      pullThroughRate: 75.5, // Mock data - should come from backend
      defaultRate: 2.3, // Mock data - should come from backend
      // Loan Quality
      goodLoansCount: goodLoansCount,
      badLoansCount: badLoansCount,
      underwritingAccuracy: 94.5, // Mock data - should come from backend
      portfolioYield: 9.2, // Mock data - should come from backend
      loanQualityIndex: loanQualityIndex,
      // Breakdown by Purpose
      personalLoans: personalLoans,
      homeLoans: homeLoans,
      carLoans: carLoans,
      educationLoans: educationLoans,
      businessLoans: businessLoans,
      // Monthly Trends (mock data - should come from backend)
      monthlyApplications: this.generateMonthlyTrends(loans),
      monthlyApprovals: this.generateMonthlyTrends(approved),
      monthlyDefaults: [],
      // Geographic Data (mock data - should come from backend)
      loansByState: this.generateStateData(loans)
    };
  }
  
  private generateMonthlyTrends(loans: LoanScreeningResponse[]): MonthlyTrend[] {
    const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'];
    return months.map(month => ({
      month: month,
      count: Math.floor(Math.random() * 50) + 10,
      amount: Math.floor(Math.random() * 5000000) + 1000000
    }));
  }
  
  private generateStateData(loans: LoanScreeningResponse[]): StateData[] {
    const states = ['Maharashtra', 'Delhi', 'Karnataka', 'Tamil Nadu', 'Gujarat'];
    return states.map(state => ({
      state: state,
      count: Math.floor(Math.random() * 100) + 20,
      amount: Math.floor(Math.random() * 10000000) + 2000000
    }));
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
      case 'ASSIGNED':
      case 'IN_PROGRESS': return 'warning';
      case 'APPROVED': return 'success';
      case 'REJECTED': return 'danger';
      case 'ESCALATED':
      case 'ESCALATED_TO_COMPLIANCE': return 'info';
      case 'UNDER_REVIEW': return 'primary';
      default: return 'secondary';
    }
  }
}
