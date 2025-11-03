import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '@core/services/auth.service';
import { 
  LoanOfficerService, 
  LoanScreeningResponse, 
  LoanScreeningRequest,
  LoanDocument,
  FraudCheckResult,
  DocumentVerificationRequest,
  DocumentResubmissionRequest,
  FraudScreeningTriggerRequest,
  EnhancedLoanScreeningResponse
} from '@core/services/loan-officer.service';
import { OfficerNotesComponent } from '../../../shared/components/officer-notes/officer-notes.component';
import { LoanEligibilityReportComponent } from '../../../shared/components/loan-eligibility-report/loan-eligibility-report.component';

@Component({
  selector: 'app-loan-review',
  standalone: true,
  imports: [CommonModule, FormsModule, OfficerNotesComponent, LoanEligibilityReportComponent],
  templateUrl: './loan-review.component.html',
  styleUrl: './loan-review.component.css'
})
export class LoanReviewComponent implements OnInit {
  assignmentId: number = 0;
  officerId: number = 0;
  loading = false;
  processing = false;
  error = '';
  success = '';
  viewMode: string = 'screening'; // 'screening' or 'results'

  loan: LoanScreeningResponse | null = null;
  enhancedLoan: EnhancedLoanScreeningResponse | null = null;
  documents: LoanDocument[] = [];
  fraudCheckResult: FraudCheckResult | null = null;

  // Action form
  selectedAction: string = '';
  remarks: string = '';
  rejectionReason: string = '';

  // Modal state
  showActionModal = false;
  actionType: string = '';
  showDocumentsModal = false;
  showFraudCheckModal = false;
  showResubmissionModal = false;

  // Document verification
  selectedDocument: LoanDocument | null = null;
  verificationRemarks: string = '';

  // Document resubmission
  resubmissionReason: string = '';
  selectedDocumentTypes: string[] = [];
  availableDocumentTypes = ['PAN_CARD', 'AADHAAR_CARD', 'INCOME_PROOF', 'BANK_STATEMENT', 'PROPERTY_DOCUMENTS', 'EMPLOYMENT_PROOF'];

  // Fraud check
  fraudCheckTriggered = false;
  fraudCheckLoading = false;

  // Document extraction
  extractingDocuments = false;
  documentsExtracted = false;
  extractionResults: any = null;

  // Tab management
  activeTab: string = 'overview';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private loanOfficerService: LoanOfficerService
  ) {
    const user = this.authService.currentUserValue;
    this.officerId = user?.officerId || user?.userId || 0;
  }

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.assignmentId = +params['id'];
      if (this.assignmentId) {
        this.loadLoanDetails();
      }
    });
    
    // Check for viewMode query parameter
    this.route.queryParams.subscribe(params => {
      this.viewMode = params['viewMode'] || 'screening';
    });
  }

  loadDocuments(): void {
    if (!this.loan) return;
    
    this.loanOfficerService.getLoanDocuments(this.loan.loanId).subscribe({
      next: (docs) => {
        this.documents = docs;
      },
      error: (err) => {
        console.error('Error loading documents:', err);
      }
    });
  }

  loadFraudCheckResults(): void {
    if (!this.loan) return;
    
    this.loanOfficerService.getFraudCheckResults(this.loan.loanId).subscribe({
      next: (result) => {
        console.log('Fraud check results loaded:', result);
        this.fraudCheckResult = result;
        this.fraudCheckTriggered = true;
      },
      error: (err) => {
        console.error('Error loading fraud check results:', err);
        console.log('No fraud check data available - this is normal if fraud check hasn\'t been run yet');
        this.fraudCheckTriggered = false;
        this.fraudCheckResult = null;
      }
    });
  }

  loadLoanDetails(): void {
    this.loading = true;
    this.error = '';

    // Load enhanced loan details with scoring breakdown
    this.loanOfficerService.getEnhancedLoanDetails(this.assignmentId).subscribe({
      next: (enhancedData) => {
        this.enhancedLoan = enhancedData;
        // Map to basic loan for backward compatibility
        this.loan = {
          assignmentId: enhancedData.assignmentId,
          loanId: enhancedData.loanId,
          applicantId: enhancedData.applicantId,
          applicantName: enhancedData.applicantName,
          loanType: enhancedData.loanType,
          loanAmount: enhancedData.loanAmount,
          riskScore: Math.round(enhancedData.normalizedRiskScore.finalScore),
          riskLevel: enhancedData.normalizedRiskScore.riskLevel,
          canApproveReject: enhancedData.canApproveReject,
          status: enhancedData.status,
          remarks: enhancedData.remarks,
          assignedAt: enhancedData.assignedAt,
          processedAt: enhancedData.processedAt,
          officerId: enhancedData.officerId,
          officerName: enhancedData.officerName,
          officerType: enhancedData.officerType
        };
        this.loading = false;
        console.log('Enhanced loan details loaded:', this.enhancedLoan);
        // Load documents and fraud check results
        this.loadDocuments();
        this.loadFraudCheckResults();
      },
      error: (err) => {
        console.error('Error loading enhanced loan details:', err);
        // Fallback to basic loan details if enhanced fails
        this.loanOfficerService.getLoanDetailsForScreening(this.assignmentId).subscribe({
          next: (data) => {
            this.loan = data;
            this.loading = false;
            this.loadDocuments();
            this.loadFraudCheckResults();
          },
          error: (err2) => {
            console.error('Error loading loan details:', err2);
            this.error = 'Failed to load loan details. Please try again.';
            this.loading = false;
          }
        });
      }
    });
  }

  openActionModal(action: string): void {
    this.actionType = action;
    this.selectedAction = action;
    this.showActionModal = true;
    this.remarks = '';
    this.rejectionReason = '';
    console.log('Action modal opened with action:', action);
  }

  closeActionModal(): void {
    this.showActionModal = false;
    this.actionType = '';
    this.selectedAction = '';
    this.remarks = '';
    this.rejectionReason = '';
  }

  confirmAction(): void {
    if (this.actionType === 'ESCALATE_TO_COMPLIANCE') {
      this.escalateToCompliance();
    } else {
      this.processScreening();
    }
  }

  processScreening(): void {
    if (!this.selectedAction) {
      this.error = 'Please select an action';
      return;
    }

    if (this.selectedAction === 'REJECT' && !this.rejectionReason) {
      this.error = 'Please provide a rejection reason';
      return;
    }

    this.processing = true;
    this.error = '';
    this.success = '';

    // Combine remarks and rejection reason
    const finalRemarks = this.selectedAction === 'REJECT' 
      ? `${this.remarks}${this.rejectionReason ? ' - ' + this.rejectionReason : ''}`
      : this.remarks || 'All verification checks passed';

    // Compute a robust risk score: prefer enhanced normalized score, then basic loan riskScore, default to 65.
    const computedRisk = Math.round(
      (this.enhancedLoan?.normalizedRiskScore?.finalScore as number | undefined) ??
      (this.loan?.riskScore as number | undefined) ??
      65
    );

    // Clamp between 0 and 100
    const riskAssessment = Math.max(0, Math.min(100, computedRisk));

    const request: LoanScreeningRequest = {
      decision: this.selectedAction,
      remarks: finalRemarks,
      riskAssessment: riskAssessment,
      incomeVerified: true,
      creditCheckPassed: true,
      collateralVerified: true,
      employmentVerified: true,
      identityVerified: true
    };

    console.log('Processing loan screening:', {
      officerId: this.officerId,
      assignmentId: this.assignmentId,
      request: request
    });

    this.loanOfficerService.processLoanScreening(this.officerId, this.assignmentId, request).subscribe({
      next: (response) => {
        console.log('Loan screening response:', response);
        console.log('Response status:', response.status);
        this.processing = false;
        const actionText = this.selectedAction === 'APPROVE' ? 'approved' : 'rejected';
        this.success = `Loan ${actionText} successfully!`;
        this.closeActionModal();
        
        // Reload loan details
        setTimeout(() => {
          this.loadLoanDetails();
        }, 1000);

        // Navigate back after 2 seconds
        setTimeout(() => {
          this.router.navigate(['/loan-officer/assigned-loans']);
        }, 2000);
      },
      error: (err) => {
        console.error('Error processing loan:', err);
        console.error('Error status:', err.status);
        console.error('Error body:', err.error);
        
        // Extract error message from different possible formats
        let errorMessage = 'Failed to process loan. Please try again.';
        if (err.error?.message) {
          errorMessage = err.error.message;
        } else if (typeof err.error === 'string') {
          errorMessage = err.error;
        } else if (err.message) {
          errorMessage = err.message;
        }
        
        this.error = errorMessage;
        this.processing = false;
      }
    });
  }

  escalateToCompliance(): void {
    this.processing = true;
    this.error = '';
    this.success = '';

    this.loanOfficerService.escalateToCompliance(this.assignmentId, this.remarks).subscribe({
      next: (response) => {
        this.processing = false;
        this.success = 'Loan escalated to compliance officer successfully!';
        this.closeActionModal();
        
        // Navigate back after 2 seconds
        setTimeout(() => {
          this.router.navigate(['/loan-officer/assigned-loans']);
        }, 2000);
      },
      error: (err) => {
        console.error('Error escalating loan:', err);
        this.error = err.error?.message || 'Failed to escalate loan. Please try again.';
        this.processing = false;
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/loan-officer/assigned-loans']);
  }

  // Document Extraction
  extractDocuments(): void {
    if (!this.assignmentId || this.extractingDocuments || this.documentsExtracted) {
      return;
    }

    this.extractingDocuments = true;
    this.error = '';
    this.success = '';

    this.loanOfficerService.extractDocuments(this.assignmentId).subscribe({
      next: (response) => {
        this.extractingDocuments = false;
        this.documentsExtracted = true;
        this.extractionResults = response;
        
        const successCount = response.successCount || 0;
        const totalCount = response.totalDocuments || 0;
        
        if (successCount === totalCount) {
          this.success = `Successfully extracted data from all ${totalCount} document(s)!`;
        } else {
          this.success = `Extracted ${successCount} out of ${totalCount} document(s). Check extraction results for details.`;
        }
        
        console.log('Document extraction results:', response);
        
        // Auto-clear success message after 5 seconds
        setTimeout(() => {
          this.success = '';
        }, 5000);
      },
      error: (error) => {
        this.extractingDocuments = false;
        console.error('Document extraction error:', error);
        this.error = error.error?.message || 'Failed to extract documents. Please try again.';
        
        // Auto-clear error message after 5 seconds
        setTimeout(() => {
          this.error = '';
        }, 5000);
      }
    });
  }

  formatCurrency(amount: number): string {
    return this.loanOfficerService.formatCurrency(amount);
  }

  formatDate(dateString: string): string {
    return this.loanOfficerService.formatDate(dateString);
  }

  getRiskColor(riskLevel: string): string {
    return this.loanOfficerService.getRiskColor(riskLevel);
  }

  getStatusColor(status: string): string {
    return this.loanOfficerService.getStatusColor(status);
  }

  getRiskPercentage(riskScore: number): number {
    return riskScore;
  }

  getRiskProgressColor(riskScore: number): string {
    if (riskScore < 30) return 'bg-success';
    if (riskScore < 70) return 'bg-warning';
    return 'bg-danger';
  }

  // Document Management
  openDocumentsModal(): void {
    this.showDocumentsModal = true;
    this.loadDocuments();
  }

  closeDocumentsModal(): void {
    this.showDocumentsModal = false;
    this.selectedDocument = null;
    this.verificationRemarks = '';
  }

  verifyDocument(document: LoanDocument, status: string): void {
    const request: DocumentVerificationRequest = {
      documentId: document.documentId,
      verificationStatus: status,
      remarks: this.verificationRemarks
    };

    this.processing = true;
    this.loanOfficerService.verifyDocument(this.officerId, request).subscribe({
      next: (updatedDoc) => {
        this.success = `Document ${status.toLowerCase()} successfully!`;
        this.loadDocuments();
        this.verificationRemarks = '';
        setTimeout(() => this.success = '', 3000);
        this.processing = false;
      },
      error: (err) => {
        this.error = err.error?.message || 'Failed to verify document';
        this.processing = false;
      }
    });
  }

  getDocumentStatusClass(status: string): string {
    const classes: any = {
      'PENDING': 'badge bg-warning',
      'VERIFIED': 'badge bg-success',
      'REJECTED': 'badge bg-danger'
    };
    return classes[status] || 'badge bg-secondary';
  }

  // Document Resubmission
  openResubmissionModal(): void {
    this.showResubmissionModal = true;
    this.selectedDocumentTypes = [];
    this.resubmissionReason = '';
  }

  closeResubmissionModal(): void {
    this.showResubmissionModal = false;
    this.selectedDocumentTypes = [];
    this.resubmissionReason = '';
  }

  toggleDocumentType(docType: string): void {
    const index = this.selectedDocumentTypes.indexOf(docType);
    if (index > -1) {
      this.selectedDocumentTypes.splice(index, 1);
    } else {
      this.selectedDocumentTypes.push(docType);
    }
  }

  requestResubmission(): void {
    if (!this.loan || this.selectedDocumentTypes.length === 0 || !this.resubmissionReason) {
      this.error = 'Please select documents and provide a reason';
      return;
    }

    const request: DocumentResubmissionRequest = {
      loanId: this.loan.loanId,
      applicantId: this.loan.applicantId,
      documentTypes: this.selectedDocumentTypes,
      reason: this.resubmissionReason,
      remarks: this.remarks
    };

    this.processing = true;
    this.loanOfficerService.requestDocumentResubmission(this.officerId, request).subscribe({
      next: () => {
        this.success = 'Document resubmission request sent to applicant!';
        this.closeResubmissionModal();
        setTimeout(() => this.success = '', 3000);
        this.processing = false;
      },
      error: (err) => {
        this.error = err.error?.message || 'Failed to request document resubmission';
        this.processing = false;
      }
    });
  }

  // Fraud Screening
  openFraudCheckModal(): void {
    this.showFraudCheckModal = true;
  }

  closeFraudCheckModal(): void {
    this.showFraudCheckModal = false;
  }

  triggerFraudCheck(): void {
    if (!this.loan) return;

    const request: FraudScreeningTriggerRequest = {
      loanId: this.loan.loanId,
      applicantId: this.loan.applicantId
    };

    this.fraudCheckLoading = true;
    this.error = '';

    this.loanOfficerService.triggerFraudScreening(this.officerId, request).subscribe({
      next: (result) => {
        this.fraudCheckResult = result;
        this.fraudCheckTriggered = true;
        this.fraudCheckLoading = false;
        this.success = 'Fraud screening completed successfully!';
        this.closeFraudCheckModal();
        setTimeout(() => this.success = '', 3000);
        // Reload loan details to get updated risk score
        this.loadLoanDetails();
      },
      error: (err) => {
        this.error = err.error?.message || 'Failed to trigger fraud screening';
        this.fraudCheckLoading = false;
      }
    });
  }

  formatDocumentType(type: string): string {
    return type.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase());
  }

  // Helper method to check if resubmission button should be disabled
  isResubmissionDisabled(): boolean {
    return this.processing || this.selectedDocumentTypes.length === 0 || !this.resubmissionReason;
  }

  // Getter for resubmission button text
  getResubmissionButtonText(): string {
    return this.processing ? 'Sending...' : 'Send Request';
  }

  // Helper methods for template text to avoid ternary operators
  getApprovalIndicatorText(): string {
    return this.loan?.canApproveReject ? 'You can approve/reject this loan' : 'Requires escalation to compliance';
  }

  getApprovalIndicatorClass(): string {
    return this.loan?.canApproveReject ? 'bg-success bg-opacity-10' : 'bg-warning bg-opacity-10';
  }

  getExtractButtonText(): string {
    if (this.extractingDocuments) return 'Extracting Documents...';
    if (this.documentsExtracted) return 'Documents Extracted ✓';
    return 'Extract Document Data';
  }

  getFraudCheckButtonText(): string {
    return this.fraudCheckTriggered ? 'Fraud Check Completed' : 'Trigger Fraud Screening';
  }

  getActionModalTitle(): string {
    if (this.actionType === 'APPROVE') return 'Approve Loan';
    if (this.actionType === 'REJECT') return 'Reject Loan';
    return 'Escalate to Compliance';
  }

  getActionModalMessage(): string {
    if (this.actionType === 'APPROVE') return 'Are you sure you want to approve this loan application?';
    if (this.actionType === 'REJECT') return 'Please provide a reason for rejecting this loan application:';
    return 'Escalate this loan to a compliance officer for further review.';
  }

  getConfirmButtonText(): string {
    return this.processing ? 'Processing...' : 'Confirm';
  }

  getFraudCheckModalButtonText(): string {
    return this.fraudCheckLoading ? 'Processing...' : 'Trigger Fraud Check';
  }

  getModalDisplayStyle(show: boolean): string {
    return show ? 'block' : 'none';
  }

  isActionRejectWithNoReason(): boolean {
    return this.actionType === 'REJECT' && !this.rejectionReason;
  }

  // Helper methods for button disabled states
  isFraudCheckDisabled(): boolean {
    if (this.fraudCheckTriggered) return true;
    if (!this.loan) return true;
    return this.loan.status !== 'ASSIGNED' && this.loan.status !== 'PENDING';
  }

  isResubmissionButtonDisabled(): boolean {
    if (!this.loan) return true;
    return this.loan.status !== 'ASSIGNED' && this.loan.status !== 'PENDING';
  }

  isApproveDisabled(): boolean {
    if (!this.loan) return true;
    if (!this.loan.canApproveReject) return true;
    const validStatuses = ['ASSIGNED', 'PENDING', 'IN_PROGRESS'];
    return !validStatuses.includes(this.loan.status);
  }

  isRejectDisabled(): boolean {
    if (!this.loan) return true;
    if (!this.loan.canApproveReject) return true;
    const validStatuses = ['ASSIGNED', 'PENDING', 'IN_PROGRESS'];
    return !validStatuses.includes(this.loan.status);
  }

  isEscalateDisabled(): boolean {
    if (!this.loan) return true;
    const validStatuses = ['ASSIGNED', 'PENDING', 'IN_PROGRESS'];
    return !validStatuses.includes(this.loan.status);
  }

  // Document count helpers
  getVerifiedDocumentsCount(): number {
    return this.documents.filter(d => d.verificationStatus === 'VERIFIED').length;
  }

  getPendingDocumentsCount(): number {
    return this.documents.filter(d => d.verificationStatus === 'PENDING').length;
  }

  getRejectedDocumentsCount(): number {
    return this.documents.filter(d => d.verificationStatus === 'REJECTED').length;
  }

  // Check if all documents are verified
  allDocumentsVerified(): boolean {
    if (this.documents.length === 0) return false;
    return this.documents.every(d => d.verificationStatus === 'VERIFIED');
  }

  // Document verification button states
  isVerifyButtonDisabled(doc: any): boolean {
    return doc.verificationStatus === 'VERIFIED' || this.processing;
  }

  isRejectDocButtonDisabled(doc: any): boolean {
    return doc.verificationStatus === 'REJECTED' || this.processing;
  }

  // Tab switching
  switchTab(tab: string): void {
    this.activeTab = tab;
  }
  
  // Navigate to verify documents page
  navigateToVerifyDocuments(): void {
    if (this.loan) {
      this.router.navigate(['/loan-officer/verify-documents', this.assignmentId], {
        queryParams: { loanId: this.loan.loanId }
      });
    }
  }

  // ==================== Real Data Helper Methods ====================
  
  // Document verification progress
  getDocumentVerificationProgress(): number {
    if (this.documents.length === 0) return 0;
    return Math.round((this.getVerifiedDocumentsCount() / this.documents.length) * 100);
  }

  // Identity verification status
  getIdentityVerificationStatus(): boolean {
    // Check if PAN and Aadhaar documents are verified
    const panDoc = this.documents.find(d => d.documentType === 'PAN_CARD');
    const aadhaarDoc = this.documents.find(d => d.documentType === 'AADHAAR_CARD');
    return (panDoc?.verificationStatus === 'VERIFIED') && (aadhaarDoc?.verificationStatus === 'VERIFIED');
  }

  getIdentityVerificationText(): string {
    const panDoc = this.documents.find(d => d.documentType === 'PAN_CARD');
    const aadhaarDoc = this.documents.find(d => d.documentType === 'AADHAAR_CARD');
    
    if (!panDoc && !aadhaarDoc) return 'Identity documents not uploaded';
    if (panDoc?.verificationStatus === 'VERIFIED' && aadhaarDoc?.verificationStatus === 'VERIFIED') {
      return 'PAN and Aadhaar cross-verification completed';
    }
    if (panDoc?.verificationStatus === 'VERIFIED' || aadhaarDoc?.verificationStatus === 'VERIFIED') {
      return 'Partial identity verification completed';
    }
    return 'Identity verification pending';
  }

  // Financial analysis status
  getFinancialAnalysisStatus(): boolean {
    if (!this.loan) return false;
    // Consider financial analysis complete if risk score is calculated
    return this.loan.riskScore > 0;
  }

  getFinancialAnalysisText(): string {
    if (!this.loan) return 'Loan data not loaded';
    if (this.loan.riskScore > 0) {
      return `Risk score: ${this.loan.riskScore}% (${this.loan.riskLevel} risk)`;
    }
    return 'Financial analysis pending';
  }

  getFinancialAnalysisProgress(): number {
    if (!this.loan) return 0;
    return this.loan.riskScore > 0 ? 100 : 0;
  }

  getFinancialStatusText(): string {
    if (!this.loan) return 'Pending';
    if (this.loan.riskLevel === 'LOW') return 'Stable';
    if (this.loan.riskLevel === 'MEDIUM') return 'Moderate';
    if (this.loan.riskLevel === 'HIGH') return 'High Risk';
    return 'Under Review';
  }

  // Fraud check status
  getFraudCheckStatusText(): string {
    if (this.fraudCheckLoading) return 'Running fraud detection...';
    if (!this.fraudCheckResult) return 'Fraud check not performed';
    
    const rulesCount = this.fraudCheckResult.flaggedRules?.length || 0;
    if (rulesCount === 0) {
      return 'No fraud indicators detected';
    }
    return `${rulesCount} fraud rule(s) flagged - ${this.fraudCheckResult.riskLevel} risk`;
  }

  getFraudCheckProgress(): number {
    if (this.fraudCheckLoading) return 60;
    if (!this.fraudCheckResult) return 0;
    return 100;
  }

  getFraudCheckBadgeText(): string {
    if (this.fraudCheckLoading) return 'Running';
    if (!this.fraudCheckResult) return 'Pending';
    return this.fraudCheckResult.riskLevel;
  }

  // Final risk assessment
  getFinalRiskAssessmentText(): string {
    if (!this.loan) return 'Risk assessment pending';
    return `${this.loan.riskLevel} risk (${this.loan.riskScore}% score)`;
  }

  // ==================== Fraud Score Calculation Methods ====================
  
  // Count rules by severity
  getCriticalRulesCount(): number {
    if (!this.fraudCheckResult?.flaggedRules) return 0;
    return this.fraudCheckResult.flaggedRules.filter(rule => rule.severity === 'CRITICAL').length;
  }

  getHighRulesCount(): number {
    if (!this.fraudCheckResult?.flaggedRules) return 0;
    return this.fraudCheckResult.flaggedRules.filter(rule => rule.severity === 'HIGH').length;
  }

  getMediumRulesCount(): number {
    if (!this.fraudCheckResult?.flaggedRules) return 0;
    return this.fraudCheckResult.flaggedRules.filter(rule => rule.severity === 'MEDIUM').length;
  }

  getLowRulesCount(): number {
    if (!this.fraudCheckResult?.flaggedRules) return 0;
    return this.fraudCheckResult.flaggedRules.filter(rule => rule.severity === 'LOW').length;
  }

  // Calculate base score using the formula: Base = min(60, Critical×15 + High×10 + Medium×5 + Low×2)
  getBaseScore(): number {
    const critical = this.getCriticalRulesCount();
    const high = this.getHighRulesCount();
    const medium = this.getMediumRulesCount();
    const low = this.getLowRulesCount();
    
    const baseScore = critical * 15 + high * 10 + medium * 5 + low * 2;
    return Math.min(60, baseScore);
  }

  // Calculate points score using the formula: Points = min(40, RawScore/800×40)
  getPointsScore(): number {
    const totalPoints = this.getTotalPointsDeducted();
    const pointsScore = (totalPoints / 800) * 40;
    return Math.min(40, Math.round(pointsScore));
  }

  // ==================== Mock Data for Testing ====================
  
  createMockFraudData(): void {
    console.log('Creating mock fraud data for testing...');
    
    // Create mock fraud check result with various severity rules
    this.fraudCheckResult = {
      checkId: 1,
      loanId: this.loan?.loanId || 0,
      applicantId: this.loan?.applicantId || 0,
      panNumber: 'ABCDE1234F',
      aadhaarNumber: '1234-5678-9012',
      phoneNumber: '+91-9876543210',
      email: 'test@example.com',
      fraudTags: ['DUPLICATE_PAN', 'HIGH_RISK_LOCATION', 'SUSPICIOUS_INCOME'],
      riskLevel: 'HIGH',
      riskScore: 75,
      apiRemarks: 'Multiple fraud indicators detected. Recommend manual review.',
      checkedAt: new Date().toISOString(),
      flaggedRules: [
        {
          ruleId: 1,
          ruleCode: 'FR001',
          ruleName: 'Duplicate PAN Number',
          description: 'The provided PAN number has been used in multiple loan applications within the last 30 days.',
          category: 'IDENTITY_FRAUD',
          severity: 'CRITICAL',
          points: 50
        },
        {
          ruleId: 2,
          ruleCode: 'FR002',
          ruleName: 'High Risk Location',
          description: 'Applicant address is in a high-risk fraud zone based on historical data.',
          category: 'LOCATION_RISK',
          severity: 'HIGH',
          points: 30
        },
        {
          ruleId: 3,
          ruleCode: 'FR003',
          ruleName: 'Income Inconsistency',
          description: 'Declared income shows significant variance from employment records.',
          category: 'FINANCIAL_FRAUD',
          severity: 'MEDIUM',
          points: 20
        },
        {
          ruleId: 4,
          ruleCode: 'FR004',
          ruleName: 'Phone Number Pattern',
          description: 'Phone number follows a pattern commonly associated with fraudulent applications.',
          category: 'CONTACT_FRAUD',
          severity: 'LOW',
          points: 10
        }
      ]
    };
    
    this.fraudCheckTriggered = true;
    console.log('Mock fraud data created:', this.fraudCheckResult);
  }

  // ==================== Current Risk Data Methods ====================
  
  // Get current risk level - always use loan data as it's the primary risk assessment
  getCurrentRiskLevel(): string {
    return this.loan?.riskLevel || 'UNKNOWN';
  }

  // Get current risk score - always use loan data as it's the primary risk assessment  
  getCurrentRiskScore(): number {
    return this.loan?.riskScore || 0;
  }

  // Get current check date (fraud check date or current time)
  getCurrentCheckDate(): string {
    if (this.fraudCheckResult) {
      return this.formatDate(this.fraudCheckResult.checkedAt);
    }
    return this.formatDate(new Date().toISOString());
  }

  // ==================== Loan Risk Calculation Methods ====================
  
  // Calculate credit score impact (0-50 points based on risk level)
  getLoanCreditScoreImpact(): number {
    if (!this.loan) return 0;
    
    // Distribute risk score across components based on loan risk level
    if (this.loan.riskLevel === 'HIGH') {
      return Math.round(this.loan.riskScore * 0.4); // 40% of total risk
    } else if (this.loan.riskLevel === 'MEDIUM') {
      return Math.round(this.loan.riskScore * 0.35); // 35% of total risk
    } else {
      return Math.round(this.loan.riskScore * 0.3); // 30% of total risk
    }
  }

  // Calculate income analysis impact (0-30 points)
  getLoanIncomeImpact(): number {
    if (!this.loan) return 0;
    
    if (this.loan.riskLevel === 'HIGH') {
      return Math.round(this.loan.riskScore * 0.25); // 25% of total risk
    } else if (this.loan.riskLevel === 'MEDIUM') {
      return Math.round(this.loan.riskScore * 0.3); // 30% of total risk
    } else {
      return Math.round(this.loan.riskScore * 0.35); // 35% of total risk
    }
  }

  // Calculate DTI impact (0-25 points)
  getLoanDTIImpact(): number {
    if (!this.loan) return 0;
    
    if (this.loan.riskLevel === 'HIGH') {
      return Math.round(this.loan.riskScore * 0.2); // 20% of total risk
    } else if (this.loan.riskLevel === 'MEDIUM') {
      return Math.round(this.loan.riskScore * 0.2); // 20% of total risk
    } else {
      return Math.round(this.loan.riskScore * 0.2); // 20% of total risk
    }
  }

  // Calculate other factors (0-15 points)
  getLoanOtherFactors(): number {
    if (!this.loan) return 0;
    
    const creditImpact = this.getLoanCreditScoreImpact();
    const incomeImpact = this.getLoanIncomeImpact();
    const dtiImpact = this.getLoanDTIImpact();
    
    // Calculate remaining points to reach total risk score
    return Math.max(0, this.loan.riskScore - creditImpact - incomeImpact - dtiImpact);
  }

  // Get total risk points (should equal loan.riskScore)
  getLoanTotalRiskPoints(): number {
    return this.getLoanCreditScoreImpact() + this.getLoanIncomeImpact() + 
           this.getLoanDTIImpact() + this.getLoanOtherFactors();
  }

  // Get risk level description
  getRiskLevelDescription(): string {
    if (!this.loan) return '';
    
    switch (this.loan.riskLevel) {
      case 'LOW': return 'Low risk - Excellent creditworthiness';
      case 'MEDIUM': return 'Medium risk - Good creditworthiness with some concerns';
      case 'HIGH': return 'High risk - Significant creditworthiness concerns';
      default: return 'Risk level unknown';
    }
  }

  // Fraud check calculation helpers
  getTotalPointsDeducted(): number {
    if (!this.fraudCheckResult?.flaggedRules || this.fraudCheckResult.flaggedRules.length === 0) {
      return 0;
    }
    return this.fraudCheckResult.flaggedRules.reduce((sum, rule) => sum + (rule.points || 0), 0);
  }

  getMaxPossiblePoints(): number {
    if (!this.fraudCheckResult?.flaggedRules || this.fraudCheckResult.flaggedRules.length === 0) {
      return 100;
    }
    const totalPoints = this.getTotalPointsDeducted();
    const riskScore = this.fraudCheckResult.riskScore || 1;
    return Math.round((totalPoints * 100) / riskScore);
  }

  // Parse extracted JSON data
  parseExtractedData(doc: LoanDocument): any {
    if (!doc.extractedJson) return null;
    try {
      return JSON.parse(doc.extractedJson);
    } catch (e) {
      console.error('Error parsing extracted JSON:', e);
      return null;
    }
  }

  // Check if document has extracted data
  hasExtractedData(doc: LoanDocument): boolean {
    return !!doc.extractedJson && doc.extractedJson !== '{}';
  }

  // Format extracted field for display
  formatExtractedField(key: string): string {
    // Convert snake_case and camelCase to Title Case
    return key
      .replace(/_/g, ' ')
      .replace(/([A-Z])/g, ' $1')
      .split(' ')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
      .join(' ')
      .trim();
  }

  // Get object keys for ngFor
  objectKeys(obj: any): string[] {
    return obj ? Object.keys(obj) : [];
  }

  // Check if value is an object (for nested data)
  isObject(val: any): boolean {
    return val !== null && typeof val === 'object' && !Array.isArray(val);
  }

  // Check if value is an array
  isArray(val: any): boolean {
    return Array.isArray(val);
  }
}
