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
        this.fraudCheckResult = result;
        this.fraudCheckTriggered = true;
      },
      error: (err) => {
        console.error('Error loading fraud check results:', err);
        this.fraudCheckTriggered = false;
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

    const request: LoanScreeningRequest = {
      decision: this.selectedAction,
      remarks: finalRemarks,
      riskAssessment: this.loan?.riskScore || 0,
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
        this.processing = false;
        this.success = `Loan ${this.selectedAction.toLowerCase()} successfully!`;
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
}
